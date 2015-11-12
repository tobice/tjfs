package edu.uno.cs.tjfs.common.threads;

import edu.uno.cs.tjfs.common.TjfsException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class JobExecutorTest {

    /** The jobs will write their results to this array */
    int[] result;

    /**
     * Id of the next job that will be produced (that means that it corresponds to the number of
     * already produced jobs
     */
    int nextJobId;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        result = new int[20];
        nextJobId = 0;
    }

    @Test
    public void testPerfectRun() throws TjfsException {
        // All 20 tests should run smoothly and finish.

        IJobProducer producer = () ->
            nextJobId == 20 ? null : new Job() {
                private int jobId = nextJobId++;
                @Override
                public void run() {
                    try {
                        Thread.sleep(10);
                        result[jobId] = jobId;
                    } catch (InterruptedException e) {
                        // Do nothing
                    }
                }
            };

        JobExecutor executor = new JobExecutor(producer, 3, 3);
        executor.execute();

        for (int i = 0; i < 20; i++) {
            assertThat(result[i], equalTo(i));
        }
    }

    @Test
    public void testImmediateFailure() throws TjfsException {
        // All jobs fail immediately which means that the executor should shut down soon and
        // leave most of the jobs not produced yet.

        IJobProducer producer = () -> {
            nextJobId++;
            return new Job() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(100);
                        notifyFailure(new TjfsException("Failed"));
                    } catch (InterruptedException e) {
                        // Do nothing
                    }
                }
            };
        };

        exception.expect(TjfsException.class);
        exception.expectMessage("Failed");

        try {
            JobExecutor executor = new JobExecutor(producer, 3, 3);
            executor.execute();
        } catch (TjfsException e) {
            // 3 Jobs are running, 3 Jobs are waiting in the queue and 1 job has been produced
            // and is waiting to be put in the queue. Therefore the id of the next job should be 7.
            assertThat(nextJobId, is(7));
            throw e;
        }
    }

    @Test
    public void testPartialFailure() throws TjfsException {
        // Jobs in the second half start failing which means that some should finish but most
        // shouldn't and also some should still remain not produced (although it's hard to reason
        // about exact numbers).

        IJobProducer producer = () ->
            new Job() {
                private int jobId = nextJobId++;
                @Override
                public void run() {
                    try {
                        Thread.sleep(100);
                        if (jobId >= 10) {
                            notifyFailure(new TjfsException("Failed"));
                        } else {
                            result[jobId] = jobId;
                        }
                    } catch (InterruptedException e) {
                        // Do nothing
                    }
                }
            };

        exception.expect(TjfsException.class);
        exception.expectMessage("Failed");

        try {
            JobExecutor executor = new JobExecutor(producer, 3, 3);
            executor.execute();
        } catch (TjfsException e) {
            // *Some* of the first jobs should be able to finish and I guess it's safe to expect
            // that they finished in this test environment.
            for (int i = 0; i < 5; i++) {
                assertThat(result[i], is(i));
            }
            // But all jobs from 10 and further should fail.
            for (int i = 10; i < 20; i++) {
                assertThat(result[i], is(0));
            }
            // Since we start failing pretty early, we can expect that not *all* jobs have been
            // already produced.
            assertThat(nextJobId, lessThan(20));
            throw e;
        }
    }

    @Test
    public void testProducerFailure() throws TjfsException {
        // In this scenario, the jobs are okay but it's the producer that fails some time along
        // the way.

        IJobProducer producer = () -> {
            if (nextJobId == 14) {
                throw new UnableToProduceJobException("Failed to produce next job");
            }
            return new Job() {
                private int jobId = nextJobId++;

                @Override
                public void run() {
                    try {
                        Thread.sleep(100);
                        result[jobId] = jobId;
                    } catch (InterruptedException e) {
                        // Do nothing
                    }
                }
            };
        };

        exception.expect(TjfsException.class);
        exception.expectMessage("Failed to produce next job");
        exception.expectCause(is(instanceOf(UnableToProduceJobException.class)));

        try {
            JobExecutor executor = new JobExecutor(producer, 3, 3);
            executor.execute();
        } catch (TjfsException e) {
            // *Some* of the first jobs should be able to finish and I guess it's safe to expect
            // that they finished in this test environment.
            for (int i = 0; i < 5; i++) {
                assertThat(result[i], is(i));
            }
            // But all jobs from 14 and further should fail (and possibly some before)
            for (int i = 14; i < 20; i++) {
                assertThat(result[i], is(0));
            }
            // Exactly the point when jobs should fail.
            assertThat(nextJobId, is(14));
            throw e;
        }
    }
}