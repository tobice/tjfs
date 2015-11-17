package edu.uno.cs.tjfs.common.threads;

abstract public class WaitingJob extends Job {

    /** Job to wait for before we finish this job */
    protected WaitingJob previousJob;

    /** Job status */
    protected boolean finished = false;

    public WaitingJob(WaitingJob previousJob) {
        this.previousJob = previousJob;
    }

    @Override
    public void run() {
        // Perform the actual job (which might decide to wait for the previous one)
        runWithWaiting();

        // Wake up any job that's waiting for us.
        synchronized (this) {
            finished = true;
            notifyAll();
        }

        // Release memory (manually, to break the chain).
        previousJob = null;
    }

    /** If the previous job is not finished yet, let's wait for it. */
    protected void waitForPreviousJob() throws InterruptedException {
        if (previousJob != null) {
            synchronized (previousJob) {
                while (!previousJob.finished) {
                    previousJob.wait();
                }
            }
        }
    }

    /** Perform the job with the possibility to wait on the previous one. */
    protected abstract void runWithWaiting();
}
