package edu.uno.cs.tjfs.client;

import edu.uno.cs.tjfs.chunkserver.IChunkClient;
import edu.uno.cs.tjfs.common.ChunkDescriptor;
import edu.uno.cs.tjfs.common.FileDescriptor;
import edu.uno.cs.tjfs.common.TjfsException;
import edu.uno.cs.tjfs.common.Utils;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class PutChunkJob implements Runnable {

    /** Chunk client to access chunk servers */
    protected final IChunkClient chunkClient;

    /** File that the chunk belongs to */
    protected final FileDescriptor file;

    /** Descriptor of the old chunk that we have to overwrite */
    protected final ChunkDescriptor oldChunk;

    /** New chunk descriptor */
    protected final ChunkDescriptor chunk;

    /** Number of the chunk within the file */
    protected final int index;

    /** Chunk data to be written */
    protected final byte[] data;

    /** Byte offset where we start to write the data */
    protected final int byteOffset;

    public PutChunkJob(IChunkClient chunkClient, FileDescriptor file, ChunkDescriptor oldChunk, ChunkDescriptor chunk, int index, byte[] data, int byteOffset) {
        this.chunkClient = chunkClient;
        this.file = file;
        this.oldChunk = oldChunk;
        this.chunk = chunk;
        this.index = index;
        this.data = data;
        this.byteOffset = byteOffset;
    }

    @Override
    public void run() {
        try {
            // If there is an original chunk that should be updated, we have to get it and then
            // overwrite it / update it with new data.
            byte[] content;
            if (oldChunk != null) {
                byte[] oldData = IOUtils.toByteArray(chunkClient.getChunk(oldChunk));
                content = Utils.mergeChunks(oldData, data, byteOffset);
            } else {
                content = data;
            }

            // Push the chunk (and try to replicate it)
            InputStream stream = new ByteArrayInputStream(content);
            chunkClient.putChunk(chunk, content.length, stream);

            // Update the file descriptor. We need to create new descriptor containing the
            // updated length and the number.
            file.chunks.add(index, chunk.withSizeAndNumber(content.length, index));
        } catch (TjfsException|IOException e) {
            e.printStackTrace();
            // TODO: ??
        }
    }
}
