package edu.uno.cs.tjfs.chunkserver;

import edu.uno.cs.tjfs.common.Machine;
import edu.uno.cs.tjfs.common.TjfsException;

import java.io.IOException;
import java.io.InputStream;

public interface IChunkClient {
    InputStream get(String chunkName) throws IOException;
    void put(String chunkName, int dataLength, InputStream chunkInputStream) throws Exception;
    void delete(String chunkName);
    String[] list();

    // Hey Janak, this is the new improved API. I added *Chunk suffix so that it doesn't collide
    // with your current code and throw lots of parse errors. When its done and fixed, we can
    // refactor the names back to the shorter versions. Right lets agree on this.
    // TODO: move this file eventually to common package as it's going to be used on multiple places
    // (the same applies to the actual implementation)
    InputStream getChunk(Machine machine, String name) throws TjfsException;
    void putChunk(Machine machine, String name, int length, InputStream data) throws TjfsException;
    void deleteChunk(Machine machine, String name) throws TjfsException;
    String[] list(Machine machine) throws TjfsException;
}
