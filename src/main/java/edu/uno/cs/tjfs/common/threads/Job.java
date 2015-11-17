package edu.uno.cs.tjfs.common.threads;


abstract public class Job implements Runnable {
    /** Listener that will be notified of failures. */
    protected IJobFailureListener failureListener;

    public void setJobFailureListener(IJobFailureListener failureListener) {
        this.failureListener = failureListener;
    }

    /**
     * Notify the listener of a failure
     * @param e failure that happened while running the Job
     */
    protected void notifyFailure(Exception e) {
        if (failureListener != null) {
            failureListener.onJobFailure(this, e);
        }
    }
}
