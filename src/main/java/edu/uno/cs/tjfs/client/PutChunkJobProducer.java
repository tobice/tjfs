package edu.uno.cs.tjfs.client;

import edu.uno.cs.tjfs.Config;
import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.threads.IJobProducer;
import edu.uno.cs.tjfs.common.threads.Job;
import edu.uno.cs.tjfs.common.threads.UnableToProduceJobException;

import javax.rmi.CORBA.Util;
import java.io.InputStream;

public class PutChunkJobProducer implements IJobProducer {

    /** Master client to access master server */
    private final IMasterClient masterClient;

    /** Chunk client to access chunk servers */
    private final IChunkClient chunkClient;

    /** Standard size of one chunk */
    private final int chunkSize;

    /** File descriptor of the file we're writing to */
    private final FileDescriptor file;

    /** Position in the file where we want to start writing */
    private final int byteOffset;

    private ChunkNameAllocator allocator;
    private ChunkChopper chopper;

    /** Current chunk index in the file */
    private int currentIndex = -1; // Start at -1 so that our recursion works.

    /**
     * @param masterClient access to master server
     * @param chunkClient access to chunk servers
     * @param chunkSize size of one chunk
     * @param file file descriptor of the file we're writing to
     * @param data data to be written
     * @param byteOffset position in the file where to start write the data
     */
    public PutChunkJobProducer(IMasterClient masterClient, IChunkClient chunkClient, int chunkSize, FileDescriptor file, InputStream data, int byteOffset) {
        // TODO: remove master client dependency
        this.masterClient = masterClient;
        this.chunkClient = chunkClient;
        this.chunkSize = chunkSize;
        this.file = file;
        this.byteOffset = byteOffset;

        allocator = new ChunkNameAllocator(masterClient, 10);
        chopper = new ChunkChopper(chunkSize, data);
    }

    public Job getNext() throws UnableToProduceJobException {
        try {
            // Increase our cursor so that recursion works. That's why the initial value has to be -1
            currentIndex++;

            // Calculate the index of the first chunk that will contain our data
            int targetIndex = Utils.getChunkIndex(byteOffset, chunkSize);

            if (currentIndex < targetIndex) {
                // Using recursion iterate through the beginning of the file until we reach the point
                // where we have to start writing. If the file is actually shorter than our
                // byteOffset, pad the empty space with zeros.
                if (file.getChunk(currentIndex) == null) {
                    return createJobForEmptyChunk();
                } else if (file.getChunk(currentIndex).size < chunkSize) {
                    return createJobForPaddingChunk();
                } else {
                    // The chunk at this position is full and there is no
                    // reason to change it, so let's just jump to the next one.
                    return getNext();
                }
            } else if (currentIndex == targetIndex) {
                return createJobForInitialChunk();
            } else {
                return createJobForNewChunk();
            }
        } catch (TjfsException e) {
            throw new UnableToProduceJobException(e.getMessage(), e);
        }
    }

    /**
     * Job description: create empty chunk full of zeros at given position.
     * @return new job
     * @throws TjfsException
     */
    private PutChunkJob createJobForEmptyChunk() throws TjfsException {
        return createJob(null, allocator.getOne(), new byte[chunkSize], 0);
    }

    /**
     * Job description: current chunk is too short so pad the rest of it with zeros to get the
     * desired chunk length.
     * @return new job
     * @throws TjfsException
     */
    private PutChunkJob createJobForPaddingChunk() throws TjfsException {
        ChunkDescriptor oldChunk = file.getChunk(currentIndex);
        byte[] data = new byte[chunkSize - oldChunk.size];
        return createJob(oldChunk, allocator.getOne(), data, oldChunk.size);
    }

    /**
     * Job description: this is the chunk where we actually start writing. Beginning of the chunk
     * remains old but the rest of it is filled with the new data (we're on the edge).
     * @return new job
     * @throws TjfsException
     */
    private PutChunkJob createJobForInitialChunk() throws TjfsException{
        ChunkDescriptor oldChunk = file.getChunk(currentIndex);
        int inChunkOffset = byteOffset - Utils.getChunkOffset(currentIndex, chunkSize);
        byte[] data = chopper.chopNext(chunkSize - inChunkOffset);

        // We reached the end of the stream. No data to be written, no more jobs.
        if (data == null)  {
            return null;
        }

        // If there is no chunk to be overwritten, we have to pad the beginning of the chunk with
        // zeros (the length is defined by inChunkOffset
        if (oldChunk == null) {
            data = Utils.mergeChunks(new byte[inChunkOffset], data, inChunkOffset);
            return createJob(null, allocator.getOne(), data, 0);
        } else {
            return createJob(oldChunk, allocator.getOne(), data, inChunkOffset);
        }
    }

    /**
     * Job description: replace the original data with the new data. The new data might actually
     * be shorter in which case only the beginning of the chunk will be changed.
     * @return new job
     * @throws TjfsException
     */
    private PutChunkJob createJobForNewChunk() throws TjfsException {
        byte[] data = chopper.chopNext();

        if (data == null)  {
            // We reached the end of the stream. No data to be written, no more jobs.
            return null;
        } else if (data.length < chunkSize) {
            // The new data is actually shorter than a standard length in which case we have to
            // merge it with the old chunk (which might or might not exist)
            return createJob(file.getChunk(currentIndex), allocator.getOne(), data, 0);
        } else {
            // Insert brand new complete data chunk which will fill up the complete chunk length.
            // For that reason we don't need to fetch the old one (we save up one remote call)
            return createJob(null, allocator.getOne(), data, 0);
        }
    }

    private PutChunkJob createJob(ChunkDescriptor oldChunk, ChunkDescriptor newChunk, byte[] data, int byteOffset) {
        return new PutChunkJob(chunkClient, file, oldChunk, newChunk, currentIndex, data, byteOffset);
    }
}
