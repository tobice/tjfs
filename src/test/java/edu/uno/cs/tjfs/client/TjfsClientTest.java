package edu.uno.cs.tjfs.client;

import edu.uno.cs.tjfs.SoftConfig;
import edu.uno.cs.tjfs.common.DummyMasterClient;
import edu.uno.cs.tjfs.common.FileDescriptor;
import edu.uno.cs.tjfs.common.IChunkClient;
import edu.uno.cs.tjfs.common.IMasterClient;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class TjfsClientTest {

    IMasterClient masterClient;
    SoftConfig config;
    ITjfsClient tjfsClient;

    @Mock
    IChunkClient chunkClient;


    @Before
    public void setUp() throws Exception {
        initMocks(this);
        masterClient = new DummyMasterClient();
        config = new SoftConfig();
        tjfsClient = new TjfsClient(config, masterClient, chunkClient);
    }

    @Test
    public void testPut() throws Exception {
        // The put operation is quite complicated and all edge cases are tested separately in the
        // operation sub components (JobProducer, JobExecutor, PutChunkJob). This merely makes
        // sure that all components work together as they should.

        config.setChunkSize(3);
        Path path = Paths.get("/some/file");
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


}