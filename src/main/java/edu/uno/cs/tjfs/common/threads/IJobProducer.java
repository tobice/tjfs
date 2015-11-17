package edu.uno.cs.tjfs.common.threads;

/**
 * Produces Jobs. When there are no more jobs available, starts returning null.
 */
public interface IJobProducer {
    /**
     * Produce next job.
     * @return new Job or null if there are no jobs available.
     * @throws UnableToProduceJobException
     */
    Job getNext() throws UnableToProduceJobException;
}
