package edu.uno.cs.tjfs.client;

import edu.uno.cs.tjfs.common.ChunkDescriptor;
import edu.uno.cs.tjfs.common.FileDescriptor;
import edu.uno.cs.tjfs.common.IChunkClient;
import edu.uno.cs.tjfs.common.threads.UnableToProduceJobException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class GetChunkJobProducerTest {

    IChunkClient chunkClient;
    FileDescriptor file;
    int chunkSize;
    OutputStream outputStream;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        chunkClient = null;
        chunkSize = 3;
        file = new FileDescriptor(Paths.get("random_file"), null,
            new ArrayList<>(Arrays.asList(
                new ChunkDescriptor("0", new LinkedList<>(), 3, 0),
                new ChunkDescriptor("1", new LinkedList<>(), 3, 1),
                new ChunkDescriptor("2", new LinkedList<>(), 2, 2))));
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    public void testGetFullFile() throws UnableToProduceJobException {
        // Get a complete file.

        int byteOffset = 0;
        int length = 8;
        GetChunkJobProducer producer = new GetChunkJobProducer(chunkClient, outputStream, chunkSize, file, byteOffset, length);
        GetChunkJob job;

        job = (GetChunkJob) producer.getNext();
        assertThat(job.chunk, is(file.getChunk(0)));
        assertThat(job.byteOffset, equalTo(0));
        assertThat(job.length, equalTo(3));
        assertThat(job.closeStream, equalTo(false));

        job = (GetChunkJob) producer.getNext();
        assertThat(job.chunk, is(file.getChunk(1)));
        assertThat(job.byteOffset, equalTo(0));
        assertThat(job.length, equalTo(3));
        assertThat(job.closeStream, equalTo(false));

        job = (GetChunkJob) producer.getNext();
        assertThat(job.chunk, is(file.getChunk(2)));
        assertThat(job.byteOffset, equalTo(0));
        assertThat(job.length, equalTo(2));
        assertThat(job.closeStream, equalTo(true));

        job = (GetChunkJob) producer.getNext();
        assertThat(job, equalTo(null));
    }

    @Test
    public void testGetPartOfFile() throws UnableToProduceJobException {
        // Get range of bytes from file that go across several chunks.

        int byteOffset = 5;
        int length = 3;
        GetChunkJobProducer producer = new GetChunkJobProducer(chunkClient, outputStream, chunkSize, file, byteOffset, length);
        GetChunkJob job;

        job = (GetChunkJob) producer.getNext();
        assertThat(job.chunk, is(file.getChunk(1)));
        assertThat(job.byteOffset, equalTo(2));
        assertThat(job.length, equalTo(1));
        assertThat(job.closeStream, equalTo(false));

        job = (GetChunkJob) producer.getNext();
        assertThat(job.chunk, is(file.getChunk(2)));
        assertThat(job.byteOffset, equalTo(0));
        assertThat(job.length, equalTo(2));
        assertThat(job.closeStream, equalTo(true));

        job = (GetChunkJob) producer.getNext();
        assertThat(job, equalTo(null));
    }

    @Test
    public void testGetPartOfFileWithinAChunk() throws UnableToProduceJobException {
        // Get range of bytes from file that belongs only to a single chunk.

        int byteOffset = 4;
        int length = 1;
        GetChunkJobProducer producer = new GetChunkJobProducer(chunkClient, outputStream, chunkSize, file, byteOffset, length);
        GetChunkJob job;

        job = (GetChunkJob) producer.getNext();
        assertThat(job.chunk, is(file.getChunk(1)));
        assertThat(job.byteOffset, equalTo(1));
        assertThat(job.length, equalTo(1));
        assertThat(job.closeStream, equalTo(true));

        job = (GetChunkJob) producer.getNext();
        assertThat(job, equalTo(null));
    }

    @Test
    public void testGetPartOfFileOutOfRange() throws UnableToProduceJobException {
        // Try to get bytes out of the file range (i. e. read more bytes than there is in the file)

        int byteOffset = 10;
        int length = 1;
        GetChunkJobProducer producer = new GetChunkJobProducer(chunkClient, outputStream, chunkSize, file, byteOffset, length);

        exception.expect(UnableToProduceJobException.class);
        exception.expectMessage("Reading out of file range");

        producer.getNext();
    }
}
