package edu.uno.cs.tjfs.master;

import edu.uno.cs.tjfs.common.ChunkDescriptor;
import edu.uno.cs.tjfs.common.IChunkClient;
import edu.uno.cs.tjfs.common.Machine;
import edu.uno.cs.tjfs.common.TjfsException;
import edu.uno.cs.tjfs.common.threads.IJobFailureListener;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class ReplicateChunkJobTest {

    Machine machine1 = Machine.fromString("127.0.0.1:8000");
    Machine machine2 = Machine.fromString("127.0.0.2:8000");
    Machine machine3 = Machine.fromString("127.0.0.3:8000");

    @Mock
    IChunkClient chunkClient;

    @Mock
    private IJobFailureListener failureListener;

    ChunkDescriptor chunk;
    ReplicateChunkJob job;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        chunk = new ChunkDescriptor("1", new ArrayList<>(Collections.singletonList(machine1)));
        job = new ReplicateChunkJob(chunkClient, chunk, new ArrayList<>(Arrays.asList(machine2, machine3)));
    }

    @Test
    public void testReplicateSuccessfully() throws TjfsException {
        job.run();
        verify(chunkClient).replicateSync(chunk.chunkServers.get(0), machine2, chunk.name);
        assertThat(chunk.chunkServers.get(1), equalTo(machine2));
    }

    @Test
    public void testReplicateWithPartialFailure() throws TjfsException {
        doThrow(new TjfsException("Replication failed")).when(chunkClient)
            .replicateSync(chunk.chunkServers.get(0), machine2, chunk.name);
        job.run();
        verify(chunkClient).replicateSync(chunk.chunkServers.get(0), machine2, chunk.name);
        verify(chunkClient).replicateSync(chunk.chunkServers.get(0), machine3, chunk.name);
        assertThat(chunk.chunkServers.get(1), equalTo(machine3));
    }

    @Test
    public void testReplicateWithTotalFailure() throws TjfsException {
        doThrow(new TjfsException("Replication failed")).when(chunkClient)
            .replicateSync(chunk.chunkServers.get(0), machine2, chunk.name);
        doThrow(new TjfsException("Replication failed")).when(chunkClient)
            .replicateSync(chunk.chunkServers.get(0), machine3, chunk.name);
        job.setJobFailureListener(failureListener);
        job.run();
        verify(chunkClient).replicateSync(chunk.chunkServers.get(0), machine2, chunk.name);
        verify(chunkClient).replicateSync(chunk.chunkServers.get(0), machine3, chunk.name);
        assertThat(chunk.chunkServers.size(), equalTo(1));

        // We have to test that the IJobFailureListener#onJobFailure was called and that the
        // correct error message is generated.
        ArgumentCaptor<TjfsException> argument = ArgumentCaptor.forClass(TjfsException.class);
        verify(failureListener).onJobFailure(eq(job), argument.capture());
        assertThat(argument.getValue().getMessage(), equalTo("Failed to replicate chunk " + chunk.name));
    }
}