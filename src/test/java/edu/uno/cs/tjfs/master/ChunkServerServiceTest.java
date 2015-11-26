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
import java.util.Date;
import java.util.List;

import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;
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
        List<Machine> listofMachinesFromChunkServers = new ArrayList<Machine>();
        listofMachinesFromChunkServers.add(machine);
        when(zookeeperClient.getChunkServers()).thenReturn(listofMachinesFromChunkServers);
        assertEquals(this.chunkServerService.getChunkServers().size(), 1);
        assertEquals(this.chunkServerService.getChunkServers().get(0), machine);
        assertNotSame(this.chunkServerService.getChunkServers().get(0), machine2);

        assertEquals(listOfChunkNames.length, chunkServerService.chunks.size());
        for(int counter = 0; counter < chunkServerService.chunks.size(); counter++){
            assertTrue(chunkServerService.chunks.get(counter).chunkServers.size() == 1);
            assertTrue(chunkServerService.chunks.get(counter).chunkServers.get(0).equals(machine));
        }

        this.chunkServerService.onChunkServerUp(machine2);
        listofMachinesFromChunkServers.add(machine2);
        when(zookeeperClient.getChunkServers()).thenReturn(listofMachinesFromChunkServers);

        assertEquals(this.chunkServerService.getChunkServers().size(), 2);
        assertEquals(this.chunkServerService.getChunkServers().get(0), machine);
        assertEquals(this.chunkServerService.getChunkServers().get(1), machine2);

        assertEquals(9, chunkServerService.chunks.size());

        assertEquals(chunkServerService.chunks.get(0).chunkServers.size(), 2);
        assertEquals(chunkServerService.chunks.get(5).chunkServers.size(), 2);
        assertEquals(chunkServerService.chunks.get(8).chunkServers.size(), 1);


        //adding the same machine should not make a difference
        this.chunkServerService.onChunkServerUp(machine);
        //all previous tests should pass

        assertEquals(this.chunkServerService.getChunkServers().size(), 2);
        assertEquals(this.chunkServerService.getChunkServers().get(0), machine);
        assertEquals(this.chunkServerService.getChunkServers().get(1), machine2);

        assertEquals(9, chunkServerService.chunks.size());

        assertEquals(chunkServerService.chunks.get(0).chunkServers.size(), 2);
        assertEquals(chunkServerService.chunks.get(5).chunkServers.size(), 2);
        assertEquals(chunkServerService.chunks.get(8).chunkServers.size(), 1);

        //A machine cannot contain two chunks with the same name. So I would not test that
    }

    @Test
    public void onChunkServerDownTest() throws TjfsException {
        //I need at least three chunk servers to run this test
        //Running the test in an opposite of what we did in the previous tests
        ArrayList<Machine> machines = new ArrayList<>();
        when(zookeeperClient.getChunkServers()).thenReturn(machines);
        assertEquals(this.chunkServerService.getChunkServers().size(), 0);

        Machine machine = new Machine("some ip", 5555);
        String[] listOfChunkNames = { "Hello", "World", "I", "Have", "These", "Chunks" };
        when(chunkClient.list(machine)).thenReturn(listOfChunkNames);

        Machine machine2 = new Machine("some ip 2", 5555);
        String[] listOfChunkNames2 = { "Hello", "World", "I", "Have", "These", "Chunks", "Also", "new", "Chunkies" };
        when(chunkClient.list(machine2)).thenReturn(listOfChunkNames2);

        Machine machine3 = new Machine("some ip 3", 5555);
        String[] listOfChunkNames3 = { "Hello", "World", "I", "Have", "These", "Chunks", "Also", "new", "Chunkies", "new one" };
        when(chunkClient.list(machine3)).thenReturn(listOfChunkNames3);

        this.chunkServerService.onChunkServerUp(machine);
        this.chunkServerService.onChunkServerUp(machine2);
        this.chunkServerService.onChunkServerUp(machine3);

        machines.add(machine);
        machines.add(machine2);
        machines.add(machine3);

        assertEquals(this.chunkServerService.getChunkServers().size(), 3);
        assertEquals(this.chunkServerService.getChunkServers().get(0), machine);
        assertEquals(this.chunkServerService.getChunkServers().get(1), machine2);
        assertEquals(this.chunkServerService.getChunkServers().get(2), machine3);

        assertEquals(10, chunkServerService.chunks.size());

        assertEquals(chunkServerService.chunks.get(0).chunkServers.size(), 3);
        assertEquals(chunkServerService.chunks.get(5).chunkServers.size(), 3);
        assertEquals(chunkServerService.chunks.get(8).chunkServers.size(), 2);
        assertEquals(chunkServerService.chunks.get(9).chunkServers.size(), 1);

//        when(chunkClient.replicateSync(Mockito.any(Machine.class), Mockito.any(Machine.class), Mockito.any(String.class))).then(doNothing());
        Mockito.doThrow(new TjfsException("")).doNothing().when(chunkClient).replicateAsync(Mockito.any(Machine.class), Mockito.any(Machine.class), Mockito.any(String.class));
        this.chunkServerService.onChunkServerDown(machine2);

        machines.remove(machine2);
        when(zookeeperClient.getChunkServers()).thenReturn(machines);
        //number of chunks should be the same but the chunk server should not be there any more
        assertEquals(10, chunkServerService.chunks.size());
        assertEquals(this.chunkServerService.getChunkServers().size(), 2);
        assertEquals((this.chunkServerService.getChunkServers().contains(machine)
                && this.chunkServerService.getChunkServers().contains(machine3)), true);

        System.out.println(chunkServerService.chunks.get(9).name);
        assertEquals(chunkServerService.chunks.get(0).chunkServers.size(), 2);
        assertEquals(chunkServerService.chunks.get(6).chunkServers.size(), 2);
        assertEquals(chunkServerService.chunks.get(9).chunkServers.size(), 1);
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
}
