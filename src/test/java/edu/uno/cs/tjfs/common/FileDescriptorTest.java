package edu.uno.cs.tjfs.common;

import org.junit.Test;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class FileDescriptorTest {

    @Test
    public void testReplaceChunk() throws Exception {
        FileDescriptor file = new FileDescriptor(Paths.get("random_file"), 8, null,
            new ArrayList<>(Arrays.asList(
                new ChunkDescriptor("0", new LinkedList<>(), 2, 0))));

        ChunkDescriptor chunk1 = new ChunkDescriptor("1", new LinkedList<>(), 2, 0);
        ChunkDescriptor chunk3 = new ChunkDescriptor("3", new LinkedList<>(), 2, 2);

        file.replaceChunk(chunk1);
        file.replaceChunk(chunk3);

        assertThat(file.getChunk(0), is(chunk1));
        assertThat(file.getChunk(1), equalTo(null));
        assertThat(file.getChunk(2), is(chunk3));
        assertThat(file.chunks.size(), is(3));
    }
}