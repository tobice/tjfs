package edu.uno.cs.tjfs.common.threads;

import edu.uno.cs.tjfs.common.TjfsException;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Executes all jobs provided through the JobExecutor#producer in parallel. Using the
 * JobExecutor#poolSize and JobExecutor#queueSize it is possible to configure number of jobs
 * being processed concurrently at a time. If the queue is full and all worker threads are busy,
 * then the executor will wait before taking a new job from the producer. That provides good
 * memory efficiency.
 *
 * The executor successes if all jobs provided by the producer are finished with success. In the
 * moment one the jobs fails, the whole executor is shut down and an exception with a reason is
 * thrown.
 */
public class JobExecutor implements IJobFailureListener {
    /** Producer responsible for generating the jobs */
    protected final IJobProducer producer;

    /** Executor that will run our jobs */
    protected final ThreadPoolExecutor executor;

    /** Thread running the producer */
    private Thread producerThread;

    /** If a job fails, the thrown exception will be stored in this variable */
    private Exception failureException = null;

    /**
     * @param producer Producer responsible for generating the jobs
     * @param poolSize Number of concurrent workers doing the jobs
     * @param queueSize Size of the queue that will hold waiting jobs
     */
    public JobExecutor(IJobProducer producer, int poolSize, int queueSize) {
        this.producer = producer;

        // When the number of threads is greater than the core, this is the maximum time that excess
        // idle threads will wait for new tasks before terminating.
        // (taken from the docs, we won't change that)
        long keepAliveTime = 0L;

        executor = new ThreadPoolExecutor(poolSize, poolSize,
            keepAliveTime, TimeUnit.MILLISECONDS,
            new JobQueue(queueSize));
    }

    /**
     * Start executing the jobs.
     * @throws TjfsException in case of any kind of failure.
     */
    public void execute() throws TjfsException {
        producerThread = new Thread(() -> {
            try {
                Job job;
                while ((job = producer.getNext()) != null) {
                    // Add a failure listener to the job. If the job fails, we will be able to
                    // detect the failure and stop executing other jobs.
                    job.setJobFailureListener(JobExecutor.this);

                    // This command is blocking. Thanks to our underlying JobQueue, this command
                    // will wait until there is space in the queue again.
                    executor.execute(job);
                }
            } catch (RejectedExecutionException e) {
                // Once the executor is shut down, it will refuse all incoming jobs by throwing
                // an exception. Let's ignore that. The only problem is when executor is running and
                // yet it refuses more jobs. The reason for that could be full queue or too
                // little threads but that shouldn't happen if properly configured.
                if (!executor.isShutdown()) {
                    failureException = new TjfsException("Executor refused to accept more jobs", e);
                    shutdownExecutorNow();
                }
            } catch (UnableToProduceJobException e) {
                failureException = e;
                shutdownExecutorNow();
            } finally {
                // Wait until all jobs are finished.
                shutdownExecutor();
                awaitExecutorTermination();
            }
        });
        producerThread.run();

        // Wait until the producer thread finishes. If this thread is interrupted, try to stop
        // the executor.
        try {
            producerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            shutdownExecutorNow();
            awaitExecutorTermination();
        }

        if (failureException != null) {
            throw new TjfsException(failureException.getMessage(), failureException);
        }
    }

    @Override
    public void onJobFailure(Job job, Exception e) {
        shutdownExecutorNow();
        failureException = e;
    }

    private void shutdownExecutor() {
        // "Initiates an orderly shutdown in which previously submitted tasks are executed, but no
        //  new tasks will be accepted. Invocation has no additional effect if already shut down."
        executor.shutdown();
    }

    private void shutdownExecutorNow() {
        // "Attempts to stop all actively executing tasks, halts the processing of waiting tasks,
        //  and returns a list of the tasks that were awaiting execution."
        executor.shutdownNow();

        // This will interrupt our blocking JobQueue.
        producerThread.interrupt();
    }

    private void awaitExecutorTermination() {
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            failureException = new TjfsException("Jobs took too long to finish");
        }
    }
}
