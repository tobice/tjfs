package edu.uno.cs.tjfs.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class FileDescriptorTest {

    @Test
    public void testReplaceChunk() throws Exception {
        FileDescriptor file = new FileDescriptor(Paths.get("random_file"), null,
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

    @Test
    public void testGetSize() {
        FileDescriptor file;

        file = new FileDescriptor(Paths.get("random_file"), null,
            new ArrayList<>(Arrays.asList(
                new ChunkDescriptor("0", new LinkedList<>(), 3, 0),
                new ChunkDescriptor("0", new LinkedList<>(), 3, 1),
                new ChunkDescriptor("0", new LinkedList<>(), 2, 2))));
        assertThat(file.getSize(), equalTo(8));

        file = new FileDescriptor(Paths.get("random_file"), null, new ArrayList<>());
        assertThat(file.getSize(), equalTo(0));
    }

    @Test
    public void testSerializeToJson() {
        FileDescriptor file = new FileDescriptor(Paths.get("/random/file"), new Date(),
            new ArrayList<>(Arrays.asList(
                new ChunkDescriptor("0", new LinkedList<>(), 3, 0),
                new ChunkDescriptor("0", new LinkedList<>(), 3, 1),
                new ChunkDescriptor("0", new LinkedList<>(), 2, 2))));

        Gson gson = CustomGson.create();
        FileDescriptor otherFile = gson.fromJson(gson.toJson(file), FileDescriptor.class);
        assertThat(file, equalTo(otherFile));
    }

    @Test
    public void testRemoveChunkServers() {
        List<Machine> chunkServers = Arrays.asList(
            Machine.fromString("127.0.0.1:80"), Machine.fromString("127.0.0.1:90"));
        FileDescriptor file = new FileDescriptor(Paths.get("/random/file"), new Date(),
            new ArrayList<>(Arrays.asList(
                    new ChunkDescriptor("0", chunkServers, 3, 0),
                    new ChunkDescriptor("1", chunkServers, 3, 1),
                    new ChunkDescriptor("2", chunkServers, 2, 2))));

        assertThat(file.getChunk(0).chunkServers.size(), is(2));
        assertThat(file.getChunk(1).chunkServers.size(), is(2));
        assertThat(file.getChunk(2).chunkServers.size(), is(2));

        FileDescriptor cleared = file.withoutChunkServers();
        assertThat(cleared.getChunk(0).chunkServers.size(), is(0));
        assertThat(cleared.getChunk(1).chunkServers.size(), is(0));
        assertThat(cleared.getChunk(2).chunkServers.size(), is(0));

    }
}