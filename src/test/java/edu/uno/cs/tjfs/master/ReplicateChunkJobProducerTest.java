package edu.uno.cs.tjfs.master;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class ReplicateChunkJobProducerTest {

    @Test
    public void testGetNext() throws Exception {
        ReplicateChunkJob job1 = mock(ReplicateChunkJob.class);
        ReplicateChunkJob job2 = mock(ReplicateChunkJob.class);
        ReplicateChunkJob job3 = mock(ReplicateChunkJob.class);

        List<ReplicateChunkJob> jobs = Arrays.asList(job1, job2, job3);
        ReplicateChunkJobProducer producer = new ReplicateChunkJobProducer(jobs);

        assertThat(producer.getNext(), is(job1));
        assertThat(producer.getNext(), is(job2));
        assertThat(producer.getNext(), is(job3));
        assertThat(producer.getNext(), equalTo(null));
    }
}
