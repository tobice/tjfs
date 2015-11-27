package edu.uno.cs.tjfs.client;

import edu.uno.cs.tjfs.common.ChunkDescriptor;
import edu.uno.cs.tjfs.common.FileDescriptor;
import edu.uno.cs.tjfs.common.IChunkClient;
import edu.uno.cs.tjfs.common.TjfsException;
import edu.uno.cs.tjfs.common.threads.IJobFailureListener;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PutChunkJobTest {

    @Mock
    private IChunkClient chunkClient;

    @Mock
    private IJobFailureListener failureListener;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void testAddNewChunk() throws Exception {
        // We're testing a simple job that is supposed to push a brand new chunk to a file.

        FileDescriptor file = new FileDescriptor(Paths.get("random_file"));
        ChunkDescriptor chunk = new ChunkDescriptor("0", new LinkedList<>());
        byte[] data = "abcdef".getBytes();
        int byteOffset = 0;
        PutChunkJob job = new PutChunkJob(chunkClient, file, null, chunk, 0, data, byteOffset);
        job.run();

        // We are verifying that IChunkClient#put() has been called with proper arguments. The
        // last argument is an instance of InputStream. To compare the actual content of the stream,
        // we have to capture the argument and do the comparison manually.
        ArgumentCaptor<byte[]> argument = ArgumentCaptor.forClass(byte[].class);
        verify(chunkClient).put(eq(chunk), argument.capture());
        assertThat(argument.getValue(), equalTo(data));
        assertThat(file.getChunk(0).name, equalTo(chunk.name));
        assertThat(file.getChunk(0).index, equalTo(0));
        assertThat(file.getChunk(0).size, equalTo(data.length));
    }

    @Test
    public void testReplaceChunk() throws Exception {
        // This job is slightly more complicated as it is supposed to update an existing chunk.
        // Therefore it first has to load the old chunk content and then put new chunk with
        // content combined the old chunk and the incoming data

        ChunkDescriptor oldChunk = new ChunkDescriptor("0", new LinkedList<>(), 6, 0);
        FileDescriptor file = new FileDescriptor(Paths.get("random_file"), null,
            new ArrayList<>(Arrays.asList(oldChunk)));
        ChunkDescriptor chunk = new ChunkDescriptor("0", new LinkedList<>());
        byte[] data = "abc".getBytes();
        int byteOffset = 3;
        PutChunkJob job = new PutChunkJob(chunkClient, file, oldChunk, chunk, 0, data, byteOffset);

        // This is the content of our old chunk.
        when(chunkClient.get(oldChunk)).thenReturn("abcdef".getBytes());
        job.run();

        // The job should first load the old chunk.
        verify(chunkClient).get(oldChunk);

        // And then put a new chunk merged from the old one and the data.
        ArgumentCaptor<byte[]> argument = ArgumentCaptor.forClass(byte[].class);
        verify(chunkClient).put(eq(chunk), argument.capture());
        assertThat(argument.getValue(), equalTo("abcabc".getBytes()));
        assertThat(file.getChunk(0).name, equalTo(chunk.name));
        assertThat(file.getChunk(0).index, equalTo(0));
        assertThat(file.getChunk(0).size, equalTo(6));
    }

    @Test
    public void testHandleException() throws Exception {
        // In this case, the chunk client will throw an exception and the job has to notify the
        // failure listener

        ChunkDescriptor oldChunk = new ChunkDescriptor("0", new LinkedList<>(), 6, 0);
        FileDescriptor file = new FileDescriptor(Paths.get("random_file"), null,
                new ArrayList<>(Arrays.asList(oldChunk)));
        ChunkDescriptor chunk = new ChunkDescriptor("0", new LinkedList<>());
        byte[] data = "abcdef".getBytes();
        int byteOffset = 0;
        PutChunkJob job = new PutChunkJob(chunkClient, file, oldChunk, chunk, 0, data, byteOffset);

        // Set the failure listener
        job.setJobFailureListener(failureListener);

        // The chunk client will fail to provide the old chunk.
        when(chunkClient.get(oldChunk)).thenThrow(new TjfsException("Some error"));
        job.run();

        // We have to test that the IJobFailureListener#onJobFailure was called and that the
        // correct error message is generated.
        ArgumentCaptor<TjfsException> argument = ArgumentCaptor.forClass(TjfsException.class);
        verify(failureListener).onJobFailure(eq(job), argument.capture());
        assertThat(argument.getValue().getMessage(), equalTo("Put chunk job failed. Reason: Some error"));
    }
}
