package edu.uno.cs.tjfs.common.threads;

/**
 * As jobs are Runnables and can't throw exceptions, we have to use another way how to propagate
 * errors. The way we use is to use a listener. Each job will be started with a listener that
 * will allow it to notify about errors and failures.
 */
public interface IJobFailureListener {
    public void onJobFailure(Job job, Exception e);
}
