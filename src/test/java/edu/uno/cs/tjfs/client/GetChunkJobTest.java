package edu.uno.cs.tjfs.client;

import edu.uno.cs.tjfs.common.ChunkDescriptor;
import edu.uno.cs.tjfs.common.IChunkClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class GetChunkJobTest {

    @Mock
    IChunkClient chunkClient;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    ChunkDescriptor chunk1;
    ChunkDescriptor chunk2;
    ChunkDescriptor chunk3;
    ByteArrayOutputStream outputStream;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        chunk1 = new ChunkDescriptor("0", new LinkedList<>());
        chunk2 = new ChunkDescriptor("1", new LinkedList<>());
        chunk3 = new ChunkDescriptor("2", new LinkedList<>());
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void testGetChunk() throws Exception {
        // We'll download bunch of chunks and write them to a single output stream.

        GetChunkJob job1 = new GetChunkJob(chunkClient, outputStream, chunk1, 1, 2, false, null);
        GetChunkJob job2 = new GetChunkJob(chunkClient, outputStream, chunk2, 0, 3, false, job1);
        GetChunkJob job3 = new GetChunkJob(chunkClient, outputStream, chunk3, 0, 2, true, job2);

        when(chunkClient.get(chunk1)).thenReturn("abc".getBytes());
        when(chunkClient.get(chunk2)).thenReturn("def".getBytes());
        when(chunkClient.get(chunk3)).thenReturn("ghi".getBytes());

        job1.run();
        job2.run();
        job3.run();

        assertThat(outputStream.toByteArray(), equalTo("bcdefgh".getBytes()));
    }
}
