package edu.uno.cs.tjfs.client;

import org.apache.commons.lang.ArrayUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class ChunkChopperTest {
    private final String data1 = "abcdef";
    private final String data2 = "abcdefg";
    private final String data3 = "abcdefgh";

    @Test
    public void testChopNext() throws IOException {
        ChunkChopper chopper = new ChunkChopper(3, new ByteArrayInputStream(data1.getBytes()));
        assertThat(chopper.chopNext(), equalTo("abc".getBytes()));
        assertThat(chopper.chopNext(), equalTo("def".getBytes()));
        assertThat(chopper.chopNext(), nullValue());

        // One overflowing
        chopper = new ChunkChopper(3, new ByteArrayInputStream(data2.getBytes()));
        assertThat(chopper.chopNext(), equalTo("abc".getBytes()));
        assertThat(chopper.chopNext(), equalTo("def".getBytes()));
        assertThat(chopper.chopNext(), equalTo("g".getBytes()));
        assertThat(chopper.chopNext(), nullValue());

        // Two overflowing
        chopper = new ChunkChopper(3, new ByteArrayInputStream(data3.getBytes()));
        assertThat(chopper.chopNext(), equalTo("abc".getBytes()));
        assertThat(chopper.chopNext(), equalTo("def".getBytes()));
        assertThat(chopper.chopNext(), equalTo("gh".getBytes()));
        assertThat(chopper.chopNext(), nullValue());

        // Even chunk size
        chopper = new ChunkChopper(2, new ByteArrayInputStream(data1.getBytes()));
        assertThat(chopper.chopNext(), equalTo("ab".getBytes()));
        assertThat(chopper.chopNext(), equalTo("cd".getBytes()));
        assertThat(chopper.chopNext(), equalTo("ef".getBytes()));
        assertThat(chopper.chopNext(), nullValue());
    }

    @Test
    public void testChopNextIteration() throws IOException {
        // This doesn't really test anything extra. It's merely a verification that the chunk
        // reading in cycle works as intended.
        ChunkChopper chopper = new ChunkChopper(1, new ByteArrayInputStream(data1.getBytes()));
        byte[] chunk;
        byte[] result = new byte[0];
        while ((chunk = chopper.chopNext()) != null) {
            result = ArrayUtils.addAll(result, chunk);
        }

        assertThat(result, equalTo(data1.getBytes()));
        assertThat(chopper.chopNext(), nullValue());
    }
}