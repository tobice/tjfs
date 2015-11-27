package edu.uno.cs.tjfs.master;

import edu.uno.cs.tjfs.Config;
import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.messages.MCommand;
import edu.uno.cs.tjfs.common.messages.Request;
import edu.uno.cs.tjfs.common.messages.Response;
import edu.uno.cs.tjfs.common.messages.arguments.*;
import edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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

    @Rule
    public final ExpectedException exception = ExpectedException.none();

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

        Request request = new Request(MCommand.ALLOCATE_CHUNKS, new AllocateChunksRequestArgs(10));
        AllocateChunkResponseArgs args =  (AllocateChunkResponseArgs)this.masterServer.process(request).args;

        assertTrue(args.chunks.size() == 10);
        for(ChunkDescriptor chunk : args.chunks){
            assertTrue(chunk.chunkServers.contains(testMachine1));
            assertTrue(chunk.chunkServers.contains(testMachine2));
        }
    }

    @Test
    public void processPutGetFileTest() throws TjfsException {
        this.masterServer.start();
        Path testPath = Paths.get("fs/dir1/dir2/filename");
        ArrayList<ChunkDescriptor> chunks = new ArrayList<ChunkDescriptor>(){{
            add(new ChunkDescriptor("testChunk", null));
            add(new ChunkDescriptor("testChunk2", null));
        }};
        FileDescriptor fileDescriptor = new FileDescriptor(testPath, new Date(), chunks);
        Request request = new Request(MCommand.PUT_FILE, new PutFileRequestArgs(fileDescriptor));

        masterServer.process(request);

        testPath = Paths.get("fs/dir1/dir2/filename2");
        fileDescriptor = new FileDescriptor(testPath, new Date(), chunks);
        request = new Request(MCommand.PUT_FILE, new PutFileRequestArgs(fileDescriptor));

        masterServer.process(request);
        request = new Request(MCommand.GET_FILE, new GetFileRequestArgs(testPath));
        Response resultResponse = masterServer.process(request);
        assertTrue(((GetFileResponseArgs) resultResponse.args).file.equals(fileDescriptor));
        assertTrue(((GetFileResponseArgs) resultResponse.args).file.path.equals(fileDescriptor.path));

        testPath = Paths.get("fs/dir1/dir2");
        fileDescriptor = new FileDescriptor(testPath, new Date(), chunks);
        request = new Request(MCommand.PUT_FILE, new PutFileRequestArgs(fileDescriptor));

        exception.expect(TjfsException.class);
        exception.expectMessage("A directory with the same name already exists");
        //This should throw an exception
        masterServer.process(request);

        testPath = Paths.get("fs/dir1/dir2/dir1"); // this should work
        fileDescriptor = new FileDescriptor(testPath, new Date(), chunks);
        request = new Request(MCommand.PUT_FILE, new PutFileRequestArgs(fileDescriptor));

        masterServer.process(request);
        request = new Request(MCommand.GET_FILE, new GetFileRequestArgs(testPath));
        resultResponse = masterServer.process(request);
        assertTrue(((GetFileResponseArgs) resultResponse.args).file.equals(fileDescriptor));
        assertTrue(((GetFileResponseArgs) resultResponse.args).file.path.equals(fileDescriptor.path));

    }

    @Test
    public void shouldNotFailWithExtraSlashesAtTheEnd() throws TjfsException {
        this.masterServer.start();
        Path testPath = Paths.get("fs/dir1/dir2/dir3///"); // this should work
        FileDescriptor fileDescriptor = new FileDescriptor(testPath, new Date(), null);
        Request request = new Request(MCommand.PUT_FILE, new PutFileRequestArgs(fileDescriptor));

        masterServer.process(request);

        request = new Request(MCommand.GET_FILE, new GetFileRequestArgs(testPath));
        Response resultResponse = masterServer.process(request);

        assertTrue(((GetFileResponseArgs) resultResponse.args).file.path.equals(fileDescriptor.path));
    }

    @Test
    public void shouldNotFailWithExtraSlashesInTheMiddle() throws TjfsException {
        this.masterServer.start();
        Path testPath = Paths.get("fs/dir1////dir2////dir3///"); // this should work
        FileDescriptor fileDescriptor = new FileDescriptor(testPath, new Date(), null);
        Request request = new Request(MCommand.PUT_FILE, new PutFileRequestArgs(fileDescriptor));

        masterServer.process(request);

        request = new Request(MCommand.GET_FILE, new GetFileRequestArgs(testPath));
        Response resultResponse = masterServer.process(request);

        assertTrue(((GetFileResponseArgs) resultResponse.args).file.path.equals(Paths.get("fs/dir1/dir2/dir3")));
    }

    @Test
    public void shouldFailWhileputtingEmptyFileName() throws TjfsException {
        this.masterServer.start();
        Path testPath = Paths.get("");
        FileDescriptor fileDescriptor = new FileDescriptor(testPath, new Date(), null);
        Request request = new Request(MCommand.PUT_FILE, new PutFileRequestArgs(fileDescriptor));

        exception.expect(TjfsException.class);
        exception.expectMessage("Empty file name");
        masterServer.process(request);
    }

    @Test
    public void processListFileTest() throws TjfsException {
        this.masterServer.start();
        Path testPath = Paths.get("fs/dir1/dir2/filename");
        ArrayList<ChunkDescriptor> chunks = new ArrayList<ChunkDescriptor>(){{
            add(new ChunkDescriptor("testChunk", null));
            add(new ChunkDescriptor("testChunk2", null));
        }};
        FileDescriptor fileDescriptor = new FileDescriptor(testPath, new Date(), chunks);
        Request request = new Request(MCommand.PUT_FILE, new PutFileRequestArgs(fileDescriptor));
        masterServer.process(request);

        testPath = Paths.get("fs/dir1/dir2/filename2");
        fileDescriptor = new FileDescriptor(testPath, new Date(), chunks);
        request = new Request(MCommand.PUT_FILE, new PutFileRequestArgs(fileDescriptor));
        masterServer.process(request);

        testPath = Paths.get("fs/dir1/dir2/filename3");
        fileDescriptor = new FileDescriptor(testPath, new Date(), chunks);
        request = new Request(MCommand.PUT_FILE, new PutFileRequestArgs(fileDescriptor));
        masterServer.process(request);

        testPath = Paths.get("fs/dir1/fileInDir1"); // this should work
        fileDescriptor = new FileDescriptor(testPath, new Date(), chunks);
        request = new Request(MCommand.PUT_FILE, new PutFileRequestArgs(fileDescriptor));

        masterServer.process(request);

        request = new Request(MCommand.LIST_FILE, new ListFileRequestArgs(Paths.get("fs/dir1")));
        Response resultResponse = masterServer.process(request);
        String[] expectedResult = {"dir2/", "fileInDir1"};
        assertTrue(Arrays.asList(((ListFileResponseArgs) resultResponse.args).files).containsAll(Arrays.asList(expectedResult)));

        request = new Request(MCommand.LIST_FILE, new ListFileRequestArgs(Paths.get("fs/dir1/dir2")));
        resultResponse = masterServer.process(request);
        String[] expectedResult2 = {"filename", "filename3", "filename2"};
        assertTrue(Arrays.asList(((ListFileResponseArgs) resultResponse.args).files).containsAll(Arrays.asList(expectedResult2)));

    }
}
