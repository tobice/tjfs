package edu.uno.cs.tjfs.client;

import edu.uno.cs.tjfs.chunkserver.IChunkClient;
import edu.uno.cs.tjfs.common.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;

public class PutChunkJobGeneratorTest {

    IMasterClient masterClient;
    IChunkClient chunkClient;

    @Before
    public void setUp() throws Exception {
        masterClient = new DummyMasterClient();
        chunkClient = null;
    }

    @Test
    public void testNewFile() throws Exception {
        // Putting a new file. Therefore we don't have to deal with any offsets or padding or
        // updating existing chunks.

        int chunkSize = 3;
        int byteOffset = 0;
        FileDescriptor file = new FileDescriptor(Paths.get("random_file"));
        InputStream data = new ByteArrayInputStream("abcdefg".getBytes());
        PutChunkJobGenerator generator = new PutChunkJobGenerator(masterClient, chunkClient, chunkSize, file, data, byteOffset);
        PutChunkJob job;

        job = generator.getNext();
        assertThat(job.data, equalTo("abc".getBytes()));
        assertThat(job.byteOffset, equalTo(0));
        assertThat(job.index, equalTo(0));
        assertThat(job.oldChunk, equalTo(null));

        job = generator.getNext();
        assertThat(job.data, equalTo("def".getBytes()));
        assertThat(job.byteOffset, equalTo(0));
        assertThat(job.index, equalTo(1));
        assertThat(job.oldChunk, equalTo(null));

        job = generator.getNext();
        assertThat(job.data, equalTo("g".getBytes()));
        assertThat(job.byteOffset, equalTo(0));
        assertThat(job.index, equalTo(2));
        assertThat(job.oldChunk, equalTo(null));

        job = generator.getNext();
        assertThat(job, equalTo(null));
    }

    @Test
    public void testUpdatingMiddleOfFile() throws Exception {
        // Updating an existing file. We will write few bytes to the middle of the file, just on the
        // edge between two chunks to test that the offsets are calculated properly.

        int chunkSize = 3;
        int byteOffset = 5;
        FileDescriptor file = new FileDescriptor(Paths.get("random_file"), 9, null,
            new LinkedList<>(Arrays.asList(
                new ChunkDescriptor("0", new LinkedList<>(), 3, 0),
                new ChunkDescriptor("1", new LinkedList<>(), 3, 1),
                new ChunkDescriptor("2", new LinkedList<>(), 3, 2))));
        InputStream data = new ByteArrayInputStream("ab".getBytes());
        PutChunkJobGenerator generator = new PutChunkJobGenerator(masterClient, chunkClient, chunkSize, file, data, byteOffset);
        PutChunkJob job;

        job = generator.getNext();
        assertThat(job.data, equalTo("a".getBytes()));
        assertThat(job.byteOffset, equalTo(2));
        assertThat(job.index, equalTo(1));
        assertThat(job.oldChunk, is(file.getChunk(1)));

        job = generator.getNext();
        assertThat(job.data, equalTo("b".getBytes()));
        assertThat(job.byteOffset, equalTo(0));
        assertThat(job.index, equalTo(2));
        assertThat(job.oldChunk, is(file.getChunk(2)));

        job = generator.getNext();
        assertThat(job, equalTo(null));
    }

    @Test
    public void testExtendFile() throws Exception {
        // Updating an existing file. We will extend the file by couple of bytes. The last chunk
        // in the old file is not complete, so we will fill it up.

        int chunkSize = 3;
        int byteOffset = 6;
        FileDescriptor file = new FileDescriptor(Paths.get("random_file"), 8, null,
            new LinkedList<>(Arrays.asList(
                    new ChunkDescriptor("0", new LinkedList<>(), 3, 0),
                    new ChunkDescriptor("1", new LinkedList<>(), 3, 1),
                    new ChunkDescriptor("2", new LinkedList<>(), 2, 2))));
        InputStream data = new ByteArrayInputStream("abcde".getBytes());
        PutChunkJobGenerator generator = new PutChunkJobGenerator(masterClient, chunkClient, chunkSize, file, data, byteOffset);
        PutChunkJob job;

        job = generator.getNext();
        assertThat(job.data, equalTo("abc".getBytes()));
        assertThat(job.byteOffset, equalTo(0));
        assertThat(job.index, equalTo(2));
        assertThat(job.oldChunk, is(file.getChunk(2)));

        job = generator.getNext();
        assertThat(job.data, equalTo("de".getBytes()));
        assertThat(job.byteOffset, equalTo(0));
        assertThat(job.index, equalTo(3));
        assertThat(job.oldChunk, equalTo(null));

        job = generator.getNext();
        assertThat(job, equalTo(null));
    }

    @Test
    public void testPadFile() throws Exception {

        int chunkSize = 3;
        int byteOffset = 4;
        FileDescriptor file = new FileDescriptor(Paths.get("random_file"));
        InputStream data = new ByteArrayInputStream("abcdefg".getBytes());
        PutChunkJobGenerator generator = new PutChunkJobGenerator(masterClient, chunkClient, chunkSize, file, data, byteOffset);
        PutChunkJob job;

        job = generator.getNext();
        assertThat(job.data, equalTo(new byte[] {0, 0, 0}));
        assertThat(job.byteOffset, equalTo(0));
        assertThat(job.index, equalTo(0));
        assertThat(job.oldChunk, equalTo(null));

        // TODO: finish this
    }
}