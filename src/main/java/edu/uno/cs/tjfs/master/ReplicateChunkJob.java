package edu.uno.cs.tjfs.master;

import edu.uno.cs.tjfs.common.ChunkDescriptor;
import edu.uno.cs.tjfs.common.IChunkClient;
import edu.uno.cs.tjfs.common.Machine;
import edu.uno.cs.tjfs.common.TjfsException;
import edu.uno.cs.tjfs.common.threads.Job;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Job that will attempt to replicate chunk from one server to another. If the job succeeds it will
 * update the chunk descriptor with the target chunk server that the chunk has been replicated to.
 */
public class ReplicateChunkJob extends Job {
    final static Logger logger = Logger.getLogger(ReplicateChunkJob.class);

    /** Chunk client to access chunk servers */
    protected final IChunkClient chunkClient;

    /** Chunk that needs to be replicated. */
    protected final ChunkDescriptor chunk;

    /** List of possible target servers. The replication has to succeed at least to one of them */
    protected final List<Machine> targetServers;

    public ReplicateChunkJob(IChunkClient chunkClient, ChunkDescriptor chunk, List<Machine> targetServers) {
        this.chunkClient = chunkClient;
        this.chunk = chunk;
        this.targetServers = targetServers;
    }

    @Override
    public void run() {
        Machine sourceServer = chunk.chunkServers.get(0);

        // Go through all available target servers and try to replicate the chunk until
        // we succeed with one of the servers.
        for (Machine targetServer : targetServers) {
            try {
                logger.info("Replicating chunk " + chunk.name + " from " + sourceServer + " to " + targetServer);
                chunkClient.replicateSync(sourceServer, targetServer, chunk.name);

                // When we succeed to replicate the chunk, we have to update the descriptor. And
                // then we finish by breaking the loop.
                synchronized (chunk) {
                    chunk.chunkServers.add(targetServer);
                }
                return;
            } catch (TjfsException e) {
                logger.info("Failed replicating " + chunk.name + " to " + targetServer + ": " + e.getMessage());
            }
        }

        // If we get down here it means that replication failed for all servers and we declare
        // the job as failing.
        notifyFailure(new TjfsException("Failed to replicate chunk " + chunk.name));
    }
}
