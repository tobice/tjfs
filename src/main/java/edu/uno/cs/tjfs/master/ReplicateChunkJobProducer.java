package edu.uno.cs.tjfs.master;

import edu.uno.cs.tjfs.common.threads.IJobProducer;
import edu.uno.cs.tjfs.common.threads.Job;
import edu.uno.cs.tjfs.common.threads.UnableToProduceJobException;

import java.util.Iterator;
import java.util.List;

/**
 * Simple wrapper around list of generated jobs. The ChunkServerService is responsible for the
 * actual job production.
 */
public class ReplicateChunkJobProducer implements IJobProducer{
    protected final List<ReplicateChunkJob> jobs;

    protected final Iterator<ReplicateChunkJob> iterator;

    public ReplicateChunkJobProducer(List<ReplicateChunkJob> jobs) {
        this.jobs = jobs;
        this.iterator = jobs.iterator();
    }

    @Override
    public Job getNext() throws UnableToProduceJobException {
        if (iterator.hasNext()) {
            return iterator.next();
        } else {
            return null;
        }
    }
}
