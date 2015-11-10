package edu.uno.cs.tjfs.common;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class UtilsTest {

    @Test
    public void testGetChunkOffset() throws Exception {
        assertThat(Utils.getChunkOffset(0, 1), equalTo(0));
        assertThat(Utils.getChunkOffset(1, 1), equalTo(1));
        assertThat(Utils.getChunkOffset(2, 1), equalTo(2));
        assertThat(Utils.getChunkOffset(3, 1), equalTo(3));

        assertThat(Utils.getChunkOffset(0, 2), equalTo(0));
        assertThat(Utils.getChunkOffset(1, 2), equalTo(2));
        assertThat(Utils.getChunkOffset(2, 2), equalTo(4));
        assertThat(Utils.getChunkOffset(3, 2), equalTo(6));

        assertThat(Utils.getChunkOffset(0, 3), equalTo(0));
        assertThat(Utils.getChunkOffset(1, 3), equalTo(3));
        assertThat(Utils.getChunkOffset(2, 3), equalTo(6));
        assertThat(Utils.getChunkOffset(3, 3), equalTo(9));
    }

    @Test
    public void testGetChunkIndex() throws Exception {
        assertThat(Utils.getChunkIndex(0, 1), equalTo(0));
        assertThat(Utils.getChunkIndex(1, 1), equalTo(1));
        assertThat(Utils.getChunkIndex(2, 1), equalTo(2));
        assertThat(Utils.getChunkIndex(3, 1), equalTo(3));

        assertThat(Utils.getChunkIndex(0, 2), equalTo(0));
        assertThat(Utils.getChunkIndex(1, 2), equalTo(0));
        assertThat(Utils.getChunkIndex(2, 2), equalTo(1));
        assertThat(Utils.getChunkIndex(3, 2), equalTo(1));
        assertThat(Utils.getChunkIndex(4, 2), equalTo(2));
        assertThat(Utils.getChunkIndex(5, 2), equalTo(2));

        assertThat(Utils.getChunkIndex(0, 3), equalTo(0));
        assertThat(Utils.getChunkIndex(1, 3), equalTo(0));
        assertThat(Utils.getChunkIndex(2, 3), equalTo(0));
        assertThat(Utils.getChunkIndex(3, 3), equalTo(1));
        assertThat(Utils.getChunkIndex(4, 3), equalTo(1));
        assertThat(Utils.getChunkIndex(5, 3), equalTo(1));
        assertThat(Utils.getChunkIndex(6, 3), equalTo(2));
    }
}