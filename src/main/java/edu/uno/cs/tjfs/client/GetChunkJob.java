package edu.uno.cs.tjfs.client;

import edu.uno.cs.tjfs.common.ChunkDescriptor;
import edu.uno.cs.tjfs.common.IChunkClient;
import edu.uno.cs.tjfs.common.TjfsException;
import edu.uno.cs.tjfs.common.threads.WaitingJob;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Job that will get a chunk from a chunk server and write the contents into an output stream.
 * The job will wait for the previous job before writing to the output stream. This way we
 * achieve that the data is written in a correct order.
 */
public class GetChunkJob extends WaitingJob {
    final static Logger logger = Logger.getLogger(GetChunkJob.class);

    /** Chunk client to access chunk servers */
    protected final IChunkClient chunkClient;

    /** Output stream where the chunk data should be written */
    protected final OutputStream outputStream;

    /** Chunk to download */
    protected final ChunkDescriptor chunk;

    /** Where to start reading data from the new chunk */
    protected final int byteOffset;

    /** How many bytes should be read from the chunk starting at byteOffset */
    protected final int length;

    /** Should we close the stream when we're done? */
    protected final boolean closeStream;

    public GetChunkJob(IChunkClient chunkClient, OutputStream outputStream, ChunkDescriptor
            chunk, int byteOffset, int length, boolean closeStream, GetChunkJob previousJob) {
        super(previousJob);
        this.chunkClient = chunkClient;
        this.outputStream = outputStream;
        this.chunk = chunk;
        this.byteOffset = byteOffset;
        this.length = length;
        this.closeStream = closeStream;
    }

    @Override
    public void runWithWaiting() {
        try {
            logger.info("Getting a chunk " + chunk.name);
            byte[] data = chunkClient.get(chunk);

            // If the previous job is not finished yet, let's wait for it.
            waitForPreviousJob();

            // Write the stuff to the output stream.
            outputStream.write(data, byteOffset, length);
            logger.info("The chunk " + chunk.name + " was received and written to the output");

            if (closeStream) {
                outputStream.close();
            }
        } catch (TjfsException|IOException e) {
            logger.error("Getting chunk " + chunk.name + " failed", e);
            notifyFailure(new TjfsException("Get chunk job failed. Reason: " + e.getMessage(), e));
        } catch (IndexOutOfBoundsException e) {
            notifyFailure(new TjfsException("Get chunk job failed. Too little incoming data", e));
        } catch (InterruptedException e) {
            // We got interrupted while waiting for the previous job to finish. Do nothing.
        }
    }
}
