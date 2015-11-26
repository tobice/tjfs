package edu.uno.cs.tjfs.master;

import edu.uno.cs.tjfs.Config;
import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.messages.MCommand;
import edu.uno.cs.tjfs.common.messages.Request;
import edu.uno.cs.tjfs.common.messages.arguments.AllocateChunkResponseArgs;
import edu.uno.cs.tjfs.common.messages.arguments.AllocateChunksRequestArgs;
import edu.uno.cs.tjfs.common.messages.arguments.GetFileRequestArgs;
import edu.uno.cs.tjfs.common.messages.arguments.GetFileResponseArgs;
import edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class MasterServerTest {
    private MasterServer masterServer;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Mock
    private IZookeeperClient zookeeperClient;

    @Mock
    private IChunkClient chunkClient;

    @Mock
    private IMasterClient masterClient;

    private ChunkServerService chunkServerService;
    private Config config;

    @Before
    public void setUp() throws IOException {
        initMocks(this);
        LocalFsClient localFsClient = new LocalFsClient();
        config = new Config();
        chunkServerService = new ChunkServerService(zookeeperClient, chunkClient);
        MasterStorage masterStorage = new MasterStorage(folder.getRoot().toPath(), localFsClient, masterClient, config.getMasterReplicationIntervalTime());
        this.masterServer = new MasterServer(masterStorage, chunkServerService, zookeeperClient, config);
    }

    @Test
    public void startTest() throws TjfsException {
        //should try to register as the client
        Machine machine = new Machine(config.getCurrentIPAddress(), config.getMasterPort());
        this.masterServer.start();
        verify(zookeeperClient).registerMasterServer(machine);
    }

    @Test
    public void startReplicationTest() throws TjfsException, InterruptedException {
        when(this.masterClient.getLog(0)).thenReturn(new ArrayList<>());
        this.masterServer.becomeShadow();
        Thread replicationThread = new Thread(() -> {
            try {
                //Give it some time so that the replication actually starts
                Thread.sleep(config.getMasterReplicationIntervalTime() + 1000);
                verify(masterClient).getLog(0);
            }catch(Exception e){
                //do nothing
            }
        });
        replicationThread.start();
    }

    @Test
    public void processAllocateChunkTest() throws TjfsException {
        //become master
        this.masterServer.start();

        Machine testMachine1 = new Machine("127.0.0.1", 6002);
        Machine testMachine2 = new Machine("127.0.0.1", 6001);

        //add couple of dummy chunk servers
        chunkServerService.onChunkServerUp(testMachine1);
        chunkServerService.onChunkServerUp(testMachine2);

        List<Machine> machines = new ArrayList<Machine>(){{
            add(testMachine1);
            add(testMachine2);
        }};

        when(zookeeperClient.getChunkServers()).thenReturn(machines);

        Request request = new Request(MCommand.ALLOCATE_CHUNKS, new AllocateChunksRequestArgs(10), null, 0);
        AllocateChunkResponseArgs args =  (AllocateChunkResponseArgs)this.masterServer.process(request).args;

        assertTrue(args.chunks.size() == 10);
        for(ChunkDescriptor chunk : args.chunks){
            assertTrue(chunk.chunkServers.contains(testMachine1));
            assertTrue(chunk.chunkServers.contains(testMachine2));
        }
    }

    @Test
    public void processGetFileTest() throws TjfsException {
        this.masterServer.start();

        Request request = new Request(MCommand.GET_FILE, new GetFileRequestArgs(Paths.get("fs/testfile")), null, 0);
        GetFileResponseArgs args = (GetFileResponseArgs) this.masterServer.process(request).args;

        assertTrue(args.file.path.equals(Paths.get("fs/testfile")));
    }
}
