package edu.uno.cs.tjfs.client;

import edu.uno.cs.tjfs.common.ChunkDescriptor;
import edu.uno.cs.tjfs.common.FileDescriptor;
import edu.uno.cs.tjfs.common.IChunkClient;
import edu.uno.cs.tjfs.common.Utils;
import edu.uno.cs.tjfs.common.threads.IJobProducer;
import edu.uno.cs.tjfs.common.threads.Job;
import edu.uno.cs.tjfs.common.threads.UnableToProduceJobException;

import java.io.OutputStream;

public class GetChunkJobProducer implements IJobProducer {

    /** Chunk client to access chunk servers */
    protected final IChunkClient chunkClient;

    /** Output stream where the chunk data should be written */
    protected final OutputStream outputStream;

    /** Standard size of one chunk */
    private final int chunkSize;

    /** File that the chunk belongs to */
    protected final FileDescriptor file;

    /** Where to start reading data from the file */
    protected final int byteOffset;

    /** How many bytes should be read from the file */
    protected final int length;

    protected GetChunkJob previousJob = null;
    protected int currentIndex = 0;

    public GetChunkJobProducer(IChunkClient chunkClient, OutputStream outputStream, int chunkSize, FileDescriptor file, int byteOffset, int length) {
        this.chunkClient = chunkClient;
        this.outputStream = outputStream;
        this.chunkSize = chunkSize;
        this.file = file;
        this.byteOffset = byteOffset;
        this.length = length;

        currentIndex = Utils.getChunkIndex(byteOffset, chunkSize);
    }

    @Override
    public Job getNext() throws UnableToProduceJobException {
        if (isOver()) {
            return null;
        }

        int inChunkOffset = 0;
        int inChunkLength = chunkSize;

        if (isFirst()) {
            inChunkOffset = byteOffset - Utils.getChunkOffset(currentIndex, chunkSize);
            if (isLast()) {
                inChunkLength = byteOffset + length - Utils.getChunkOffset(currentIndex, chunkSize) - inChunkOffset;
            } else {
                inChunkLength = chunkSize - inChunkOffset;
            }
        } else {
            if (isLast()) {
                inChunkLength = byteOffset + length - Utils.getChunkOffset(currentIndex, chunkSize);
            }
        }

        boolean closeStream = isLast();
        ChunkDescriptor chunk = file.getChunk(currentIndex);

        if (chunk == null || inChunkOffset + inChunkLength > chunk.size) {
            throw new UnableToProduceJobException("Reading out of file range!");
        }

        currentIndex++;
        return createJob(chunk, inChunkOffset, inChunkLength, closeStream);
    }

    private GetChunkJob createJob(ChunkDescriptor chunk, int byteOffset, int length, boolean closeStream) {
        GetChunkJob job = new GetChunkJob(chunkClient, outputStream, chunk, byteOffset, length, closeStream, previousJob);
        previousJob = job;
        return job;
    }

    private boolean isFirst() {
        return currentIndex == Utils.getChunkIndex(byteOffset, chunkSize);
    }

    private boolean isLast() {
        return currentIndex == Utils.getChunkIndex(byteOffset + length, chunkSize);
    }

    private boolean isOver() {
        return currentIndex > Utils.getChunkIndex(byteOffset + length, chunkSize);
    }
}
