package edu.uno.cs.tjfs.common;

public interface IChunkClient {
    byte[] get(Machine machine, String name) throws TjfsException;
    byte[] get(ChunkDescriptor chunkDescriptor) throws TjfsException;
    void put(Machine machine, String name, byte[] data) throws TjfsException;
    void put(ChunkDescriptor chunkDescriptor, byte[] data) throws TjfsException;
    void putAsync(Machine machine, String name, byte[] data) throws TjfsException;
    void replicateAsync(Machine machineFrom, Machine machineTo, String chunkName) throws TjfsException;
    void replicateSync(Machine machineFrom, Machine machineTo, String chunkName) throws TjfsException;
    void delete(Machine machine, String name) throws TjfsException;
    String[] list(Machine machine) throws TjfsException;
}