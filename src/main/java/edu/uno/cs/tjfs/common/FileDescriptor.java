package edu.uno.cs.tjfs.common;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class FileDescriptor {
    /** File path */
    public final Path path;

    /** Time of the last update */
    public final Date time;

    /** List of chunks that the file consists of */
    public final ArrayList<ChunkDescriptor> chunks;

    public FileDescriptor(Path path, Date time, ArrayList<ChunkDescriptor> chunks) {
        this.path = path;
        this.time = time;
        this.chunks = chunks;
    }

    public FileDescriptor(Path path) {
        this.path = path;
        this.time = new Date();
        this.chunks = new ArrayList<>();
    }

    /**
     * Get chunk descriptor of a chunk at given position in the file.
     * @param index position of a chunk in the file
     * @return chunk descriptor at given position or null
     */
    public ChunkDescriptor getChunk(int index) {
        if (chunks.size() < index + 1) {
            return null;
        } else {
            return chunks.get(index);
        }
    }

    /**
     * Replaces current chunk at given position with a new chunk. The position is taken from
     * ChunkDescriptor#index. If the array is too short, it is extended so that it can contain the
     * new index.
     *
     * The method is synchronized so that multiple threads can concurrently update the chunks.
     * @param chunk to be added to the file
     */
    public synchronized void replaceChunk(ChunkDescriptor chunk) {
        // If necessary, pad the array with zeros.
        for (int i = chunks.size(); i <= chunk.index; i++) {
            chunks.add(null);
        }
        chunks.set(chunk.index, chunk);
    }

    /**
     * Return the file size in bytes (based on the chunk descriptors).
     * @return size in bytes
     */
    public int getSize() {
        return chunks.stream().mapToInt(chunk -> chunk.size).sum();
    }

    /**
     * Returns if the file is empty or it doesn't exist which is in our case equivalent.
     * @return if it's empty
     */
    public boolean isEmpty() {
        return getSize() == 0;
    }

    /**
     * Returns new file descriptor which is identical except all information about chunk
     * servers has been removed
     * @return cleared file descriptor
     */
    public FileDescriptor withoutChunkServers() {
        List<ChunkDescriptor> clearedChunks = chunks.stream()
            .map(ChunkDescriptor::withoutChunkServers)
            .collect(Collectors.toList());

        return new FileDescriptor(path, time, new ArrayList<>(clearedChunks));
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof FileDescriptor)) {
            return false;
        }
        FileDescriptor otherFile = (FileDescriptor) object;
        return
            path.equals(otherFile.path) &&
            time.toString().equals(otherFile.time.toString()) &&
            chunks.equals(otherFile.chunks);
    }
}
