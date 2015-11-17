package edu.uno.cs.tjfs.client;

import edu.uno.cs.tjfs.SoftConfig;
import edu.uno.cs.tjfs.common.*;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class TjfsClientTest {

    IMasterClient masterClient;
    SoftConfig config;
    ITjfsClient tjfsClient;
    Path path;
    FileDescriptor file;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Mock
    IChunkClient chunkClient;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        masterClient = new DummyMasterClient();
        config = new SoftConfig();
        tjfsClient = new TjfsClient(config, masterClient, chunkClient);

        path = Paths.get("/some/file");
        file = new FileDescriptor(path, null,
            new ArrayList<>(Arrays.asList(
                new ChunkDescriptor("0", new LinkedList<>(), 3, 0),
                new ChunkDescriptor("1", new LinkedList<>(), 3, 1),
                new ChunkDescriptor("2", new LinkedList<>(), 3, 1),
                new ChunkDescriptor("3", new LinkedList<>(), 3, 1),
                new ChunkDescriptor("4", new LinkedList<>(), 2, 2))));
    }

    @Test
    public void testPut() throws Exception {
        // The put operation is quite complicated and all edge cases are tested separately in the
        // operation sub components (JobProducer, JobExecutor, PutChunkJob). This merely makes
        // sure that all components work together as they should.

        config.setChunkSize(3);
        config.setExecutorPoolSize(2);
        config.setExecutorQueueSize(2);
        tjfsClient.put(path, new ByteArrayInputStream("abcdefgh".getBytes()));

        // Test that the file metadata (chunks) have been updated on the master server
        FileDescriptor file = masterClient.getFile(path);
        assertThat(file.getChunk(0).size, is(3));
        assertThat(file.getChunk(1).size, is(3));
        assertThat(file.getChunk(2).size, is(2));
        assertThat(file.getChunk(3), equalTo(null));

        // Test that the chunks have been correctly pushed using the chunk client.
        ArgumentCaptor<InputStream> argument = ArgumentCaptor.forClass(InputStream.class);
        verify(chunkClient, times(2)).put(anyObject(), eq(3), argument.capture());
        verify(chunkClient, times(1)).put(anyObject(), eq(2), argument.capture());

        // The order of individual IChunkClient#put calls is arbitrary. So we capture all written
        // data and order it alphabetically to get a predictable order that we can assert.
        String[] result = new String[3];
        for (int i = 0; i < 3; i++) {
            result[i] = IOUtils.toString(argument.getAllValues().get(i));
        }
        Arrays.sort(result);
        assertThat(result[0], equalTo("abc"));
        assertThat(result[1], equalTo("def"));
        assertThat(result[2], equalTo("gh"));
    }

    @Test
    public void testGet() throws Exception {
        // Read chunks from the file system and store them into a string. As in the previous
        // case, all the edge situation are covered by the test for individual components. Here
        // we're merely testing that everything works together.

        config.setChunkSize(3);
        config.setExecutorPoolSize(2); // squeeze it a bit to test queueing
        config.setExecutorQueueSize(2);
        config.setPipeBufferSize(2);
        masterClient.putFile(file);

        when(chunkClient.get(file.getChunk(0))).thenReturn(new ByteArrayInputStream("abc".getBytes()));
        when(chunkClient.get(file.getChunk(1))).thenReturn(new ByteArrayInputStream("def".getBytes()));
        when(chunkClient.get(file.getChunk(2))).thenReturn(new ByteArrayInputStream("ghi".getBytes()));
        when(chunkClient.get(file.getChunk(3))).thenReturn(new ByteArrayInputStream("jkl".getBytes()));
        when(chunkClient.get(file.getChunk(4))).thenReturn(new ByteArrayInputStream("mn".getBytes()));

        InputStream stream = tjfsClient.get(path);
        assertThat(IOUtils.toString(stream), equalTo("abcdefghijklmn"));
    }

    @Test
    public void testGetWithAnError() throws Exception {
        // Read chunks from the file system and store them into a string, except now the last
        // chunk will fail to load.

        config.setChunkSize(3);
        masterClient.putFile(file);

        when(chunkClient.get(file.getChunk(0))).thenReturn(new ByteArrayInputStream("abc".getBytes()));
        when(chunkClient.get(file.getChunk(1))).thenReturn(new ByteArrayInputStream("def".getBytes()));
        when(chunkClient.get(file.getChunk(2))).thenReturn(new ByteArrayInputStream("ghi".getBytes()));
        when(chunkClient.get(file.getChunk(3))).thenReturn(new ByteArrayInputStream("jkl".getBytes()));
        when(chunkClient.get(file.getChunk(4))).thenThrow(new TjfsException("Some reason"));

        // Seriously, a better message should be provided...
        // exception.expect(IOException.class);
        // exception.expectMessage("Pipe closed");

        // InputStream stream = tjfsClient.get(path);
        // System.out.println("Stream: " + IOUtils.toString(stream));
        // TODO: fix this test
    }
}
