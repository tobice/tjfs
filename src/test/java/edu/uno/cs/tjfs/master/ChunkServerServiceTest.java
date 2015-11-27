package edu.uno.cs.tjfs.master;

import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.lang.reflect.Array;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ChunkServerServiceTest {
    private ChunkServerService chunkServerService;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Mock
    private IZookeeperClient zookeeperClient;

    @Mock
    private IChunkClient chunkClient;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        this.chunkServerService = new ChunkServerService(zookeeperClient, chunkClient);
    }

    @Test
    public void onChunkServerUpTest() throws TjfsException {
        chunkServerService.synchronization = true;

        ArrayList<Machine> machines = new ArrayList<>();
        when(zookeeperClient.getChunkServers()).thenReturn(machines);
        assertEquals(this.chunkServerService.getChunkServers().size(), 0);

        Machine machine = new Machine("some ip", 5555);
        String[] listOfChunkNames = { "Hello", "World", "I", "Have", "These", "Chunks" };
        when(chunkClient.list(machine)).thenReturn(listOfChunkNames);

        Machine machine2 = new Machine("some ip 2", 5555);
        String[] listOfChunkNames2 = { "Hello", "World", "I", "Have", "These", "Chunks", "Also", "new", "Chunkies" };
        when(chunkClient.list(machine2)).thenReturn(listOfChunkNames2);

        this.chunkServerService.onChunkServerUp(machine);
        machines.add(machine);
        List<Machine> listofMachinesFromChunkServers = new ArrayList<Machine>();
        listofMachinesFromChunkServers.add(machine);
        when(zookeeperClient.getChunkServers()).thenReturn(listofMachinesFromChunkServers);
        assertEquals(this.chunkServerService.getChunkServers().size(), 1);
        assertEquals(this.chunkServerService.getChunkServers().get(0), machine);
        assertNotSame(this.chunkServerService.getChunkServers().get(0), machine2); // why test this ???

        assertEquals(listOfChunkNames.length, chunkServerService.chunks.size());
        for(int counter = 0; counter < chunkServerService.chunks.size(); counter++){
            assertTrue(chunkServerService.chunks.get(listOfChunkNames[0]).chunkServers.size() == 1);
            assertTrue(chunkServerService.chunks.get(listOfChunkNames[0]).chunkServers.get(0).equals(machine));
        }

        this.chunkServerService.onChunkServerUp(machine2);
        listofMachinesFromChunkServers.add(machine2);
        when(zookeeperClient.getChunkServers()).thenReturn(listofMachinesFromChunkServers);

        assertEquals(this.chunkServerService.getChunkServers().size(), 2);
        assertEquals(this.chunkServerService.getChunkServers().get(0), machine);
        assertEquals(this.chunkServerService.getChunkServers().get(1), machine2);

        assertEquals(9, chunkServerService.chunks.size());

        assertEquals(chunkServerService.chunks.get(listOfChunkNames[0]).chunkServers.size(), 2);
        assertEquals(chunkServerService.chunks.get(listOfChunkNames[5]).chunkServers.size(), 2);
        assertEquals(chunkServerService.chunks.get("Chunkies").chunkServers.size(), 1);


        //adding the same machine should not make a difference
        this.chunkServerService.onChunkServerUp(machine);
        //all previous tests should pass

        assertEquals(this.chunkServerService.getChunkServers().size(), 2);
        assertEquals(this.chunkServerService.getChunkServers().get(0), machine);
        assertEquals(this.chunkServerService.getChunkServers().get(1), machine2);

        assertEquals(9, chunkServerService.chunks.size());

        assertEquals(chunkServerService.chunks.get(listOfChunkNames[0]).chunkServers.size(), 2);
        assertEquals(chunkServerService.chunks.get(listOfChunkNames[5]).chunkServers.size(), 2);
        assertEquals(chunkServerService.chunks.get(listOfChunkNames2[8]).chunkServers.size(), 1);

        //A machine cannot contain two chunks with the same name. So I would not test that
    }

    @Test
    public void onChunkServerDownTest() throws TjfsException {
        // Machine 3 goes down

        Machine machine1 = Machine.fromString("127.0.0.1:8000");
        Machine machine2 = Machine.fromString("127.0.0.2:8000");
        Machine machine3 = Machine.fromString("127.0.0.3:8000");
        when(zookeeperClient.getChunkServers()).thenReturn(Arrays.asList(machine1, machine2));

        ChunkDescriptor chunk1 = new ChunkDescriptor("1", new ArrayList<>(Arrays.asList(machine1, machine2)));
        ChunkDescriptor chunk2 = new ChunkDescriptor("2", new ArrayList<>(Arrays.asList(machine2, machine3)));
        ChunkDescriptor chunk3 = new ChunkDescriptor("3", new ArrayList<>(Arrays.asList(machine3, machine1)));

        chunkServerService.chunks.put("1", chunk1);
        chunkServerService.chunks.put("2", chunk2);
        chunkServerService.chunks.put("3", chunk3);

        chunkServerService.synchronization = true;
        chunkServerService.onChunkServerDown(machine3);

        verify(chunkClient).replicateSync(machine2, machine1, "2");
        verify(chunkClient).replicateSync(machine1, machine2, "3");

        assertThat(chunkServerService.chunks.get("2").chunkServers, hasItems(machine1, machine2));
        assertThat(chunkServerService.chunks.get("3").chunkServers, hasItems(machine1, machine2));
    }

    @Test
    public void updateChunkServerTest() throws TjfsException {
        Machine machine = new Machine("some ip", 5555);
        String[] listOfChunkNames = { "Hello", "World", "I", "Have", "These", "Chunks" };
        when(chunkClient.list(machine)).thenReturn(listOfChunkNames);

        Machine machine2 = new Machine("some ip 2", 5555);
        String[] listOfChunkNames2 = { "Hello", "World", "I", "Have", "These", "Chunks", "Also", "new", "Chunkies" };
        when(chunkClient.list(machine2)).thenReturn(listOfChunkNames2);

        Machine machine3 = new Machine("some ip 3", 5555);
        String[] listOfChunkNames3 = { "Hello", "World", "I", "Have", "These", "Chunks", "Also", "new", "Chunkies", "new one" };
        when(chunkClient.list(machine3)).thenReturn(listOfChunkNames3);

        chunkServerService.synchronization = true;
        this.chunkServerService.onChunkServerUp(machine);
        this.chunkServerService.onChunkServerUp(machine2);
        this.chunkServerService.onChunkServerUp(machine3);

        //Initially chunk hello is in nowhere
        ChunkDescriptor chunkDescriptor = new ChunkDescriptor("Hello", null);
        ArrayList<ChunkDescriptor> chunks = new ArrayList<ChunkDescriptor>() {{
            add(chunkDescriptor);
        }};
        FileDescriptor file = new FileDescriptor(Paths.get(""), new Date(), chunks);

        file = this.chunkServerService.updateChunkServers(file);
        assertEquals(file.chunks.get(0).chunkServers.size(), 3);

        //Testing with two initial servers
        //Initially chunk hello is in nowhere
        ChunkDescriptor chunkDescriptor2 = new ChunkDescriptor("Hello", new ArrayList<Machine>(){{
            add(machine);
            add(machine2);
        }});
        chunks = new ArrayList<ChunkDescriptor>() {{
            add(chunkDescriptor2);
        }};
        file = new FileDescriptor(Paths.get(""), new Date(), chunks);
        assertEquals(file.chunks.get(0).chunkServers.size(), 2);
        file = this.chunkServerService.updateChunkServers(file);
        assertEquals(file.chunks.get(0).chunkServers.size(), 3);
    }

    @Test
    public void testGenerateReplicateJobs() {
        // Machine 3 goes down

        Machine machine1 = Machine.fromString("127.0.0.1:8000");
        Machine machine2 = Machine.fromString("127.0.0.2:8000");
        Machine machine3 = Machine.fromString("127.0.0.3:8000");
        when(zookeeperClient.getChunkServers()).thenReturn(Arrays.asList(machine1, machine2));

        ChunkDescriptor chunk1 = new ChunkDescriptor("1", new ArrayList<>(Arrays.asList(machine1, machine2)));
        ChunkDescriptor chunk2 = new ChunkDescriptor("2", new ArrayList<>(Arrays.asList(machine2, machine3)));
        ChunkDescriptor chunk3 = new ChunkDescriptor("3", new ArrayList<>(Arrays.asList(machine3, machine1)));

        chunkServerService.chunks.put("1", chunk1);
        chunkServerService.chunks.put("2", chunk2);
        chunkServerService.chunks.put("3", chunk3);

        List<ReplicateChunkJob> jobs = chunkServerService.generateReplicateJobs(machine3);

        // Chunk 2 has to be replicated to Machine 1
        assertThat(jobs.get(0).chunk, equalTo(chunk2));
        assertThat(jobs.get(0).targetServers, hasItem(machine1));

        // Chunk 3 has to be replicated to Machine 2
        assertThat(jobs.get(1).chunk, equalTo(chunk3));
        assertThat(jobs.get(1).targetServers, hasItem(machine2));
    }
}
