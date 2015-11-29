package edu.uno.cs.tjfs.common;

import java.util.ArrayList;
import java.util.List;

public class ChunkDescriptor {
    /** Name of the chunk */
    public final String name;

    /** List of server where this chunk is or should be (eventually) available */
    public final List<Machine> chunkServers;

    /** Size of the chunk */
    public final int size;

    /** Position in the file */
    public final int index;

    public ChunkDescriptor(String name, List<Machine> chunkServers) {
        this.name = name;
        this.chunkServers = chunkServers;
        this.size = 0;
        this.index = 0;
    }

    public ChunkDescriptor(String name, List<Machine> chunkServers, int size, int index) {
        this.name = name;
        this.chunkServers = chunkServers;
        this.size = size;
        this.index = index;
    }

    public ChunkDescriptor withSizeAndNumber(int size, int index) {
        return new ChunkDescriptor(name, chunkServers, size, index);
    }

    public ChunkDescriptor withoutChunkServers() {
        return new ChunkDescriptor(name, new ArrayList<>(), size, index);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ChunkDescriptor)) {
            return false;
        }
        ChunkDescriptor otherChunk = (ChunkDescriptor) object;
        return
            // Ignore machines, they are volatile.
            name.equals(otherChunk.name) &&
            size == otherChunk.size &&
            index == otherChunk.index;
    }
}
