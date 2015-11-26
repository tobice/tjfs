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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.ArrayList;

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

    @Before
    public void setUp() throws IOException {
        initMocks(this);
        LocalFsClient localFsClient = new LocalFsClient();
        Config config = new Config();
        chunkServerService = new ChunkServerService(zookeeperClient, chunkClient);
        MasterStorage masterStorage = new MasterStorage(folder.getRoot().toPath(), localFsClient, masterClient, config.getMasterReplicationIntervalTime());
        this.masterServer = new MasterServer(masterStorage, chunkServerService, zookeeperClient);
    }

    @Test
    public void getIPTest() throws TjfsException, UnknownHostException {
        String result = this.masterServer.getCurrentIPAddress();
        assertTrue(!result.isEmpty());
    }

    @Test
    public void startTest() throws TjfsException {
        //should try to register as the client
        Machine machine = new Machine(this.masterServer.getCurrentIPAddress(), 6002);
        this.masterServer.start();
        verify(zookeeperClient).registerMasterServer(machine);
    }

    @Test
    public void startReplicationTest() throws TjfsException {
//        when(this.masterClient.getLog(0)).thenReturn(new ArrayList<>());
//        this.masterServer.becomeShadow();
        //verify(masterClient).getLog(0); TODO:// how to do this? new thread
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
