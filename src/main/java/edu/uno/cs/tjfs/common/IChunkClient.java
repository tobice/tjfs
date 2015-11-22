package edu.uno.cs.tjfs.common;

import java.io.InputStream;

public interface IChunkClient {
    InputStream get(Machine machine, String name) throws TjfsException;
    InputStream get(ChunkDescriptor chunkDescriptor) throws TjfsException;
    void put(Machine machine, String name, int length, InputStream data) throws TjfsException;
    void putAsync(Machine machine, String name, int length, InputStream data) throws TjfsException;
    void put(ChunkDescriptor chunkDescriptor, int length, InputStream data) throws TjfsException;
    void delete(Machine machine, String name) throws TjfsException;
    String[] list(Machine machine) throws TjfsException;
}