package edu.uno.cs.tjfs.common;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FileDescriptor {
    /** File path */
    public final Path path;

    /** File size in bytes */
    public final int size;

    /** Time of the last update */
    public final Date time;

    /** List of chunks that the file consists of */
    public final List<ChunkDescriptor> chunks;

    public FileDescriptor(Path path, int size, Date time, List<ChunkDescriptor> chunks) {
        this.path = path;
        this.size = size;
        this.time = time;
        this.chunks = chunks;
    }

    public FileDescriptor(Path path) {
        this.path = path;
        this.size = 0;
        this.time = new Date();
        this.chunks = new ArrayList<>();
    }

    public ChunkDescriptor getChunk(int index) {
        if (chunks.size() < index + 1) {
            return null;
        } else {
            return chunks.get(index);
        }
    }
}
