package edu.uno.cs.tjfs.common.threads;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Special implementation of blocking queue that overrides the BlockingQueue#offer method to make
 * it blocking. It's used in ThreadPoolExecutor to efficiently limit the number of running
 * parallel jobs.
 * http://stackoverflow.com/questions/4521983/java-executorservice-that-blocks-on-submission-after-a-certain-queue-size
 */
public class JobQueue extends LinkedBlockingQueue<Runnable> {

    public JobQueue(int maxSize) {
        super(maxSize);
    }

    @Override
    public boolean offer(Runnable job) {
        // Turn offer() and add() into a blocking calls (unless interrupted)
        try {
            put(job);
            return true;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        return false;
    }
}

