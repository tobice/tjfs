package edu.uno.cs.tjfs.client;

import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.threads.Job;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/**
 * Job that will put a new chunk to a chunk server. If necessary, it will first download the old
 * chunk, merge with the new chunk and push the final data to a chunk server.
 */
public class PutChunkJob extends Job {
    final static Logger logger = Logger.getLogger(PutChunkJob.class);

    /** Chunk client to access chunk servers */
    protected final IChunkClient chunkClient;

    /** File that the chunk belongs to */
    protected final FileDescriptor file;

    /**
     * Descriptor of the old chunk that we have to overwrite. If not null, the old chunk is
     * actually fetched and its content is merged with the new data. If null, no chunk is fetched
     * and the new data are simply written to a new chunk
     **/
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
            logger.info("Putting a new chunk " + chunk.name);

            // If there is an original chunk that should be updated, we have to get it and then
            // overwrite it / update it with new data.
            byte[] content;
            if (oldChunk != null) {
                byte[] oldData = chunkClient.get(oldChunk);
                content = Utils.mergeChunks(oldData, data, byteOffset);
            } else {
                content = data;
            }

            // Push the chunk (and try to replicate it)
            chunkClient.put(chunk, content);
            logger.info("The chunk " + chunk.name + " was written");

            // Update the file descriptor. We need to create new descriptor containing the
            // updated length and the index. FileDescriptor#replaceChunk is synchronized so we
            // can afford to call it from separate thread (plus each thread corresponds to a
            // different chunk index which means that in theory, no conflicts should happen).
            file.replaceChunk(chunk.withSizeAndNumber(content.length, index));
        } catch (TjfsException e) {
            logger.error("Putting the chunk " + chunk.name + " failed", e);
            notifyFailure(new TjfsException("Put chunk job failed. Reason: " + e.getMessage(), e));
        }
    }
}
