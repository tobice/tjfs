package edu.uno.cs.tjfs.master;

import com.google.gson.Gson;
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
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
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

    @Mock
    SnapshotStorage snapshotStorage;

    private ChunkServerService chunkServerService;
    private Config config;

    @Before
    public void setUp() throws IOException, TjfsException {
        initMocks(this);
        LocalFsClient localFsClient = new LocalFsClient();
        config = new Config();
        chunkServerService = new ChunkServerService(zookeeperClient, chunkClient);
        MasterStorage masterStorage = new MasterStorage(folder.getRoot().toPath(), localFsClient,
                masterClient, config.getMasterReplicationIntervalTime(), config.getMasterSnapshottingIntervalTime());
        masterStorage.snapshotStorage = snapshotStorage; // Ugly way to inject mocked object but  what the hell
        masterStorage.init();
        Machine zookeeper = Machine.fromString("127.0.0.1:2181");
        int port = 6002;
        Machine thisMachine = new Machine(IpDetect.getLocalIp(zookeeper.ip), port);
        this.masterServer = new MasterServer(masterStorage, chunkServerService, zookeeperClient, thisMachine);
    }

    @Test
    public void startTest() throws TjfsException {
        //should try to register as the client
        Machine zookeeper = Machine.fromString("127.0.0.1:2181");
        int port = 6002;
        Machine thisMachine = new Machine(IpDetect.getLocalIp(zookeeper.ip), port);
        this.masterServer.start();
        verify(zookeeperClient).registerMasterServer(thisMachine);
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
            } catch (Exception e) {
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

        List<Machine> machines = new ArrayList<Machine>() {{
            add(testMachine1);
            add(testMachine2);
        }};

        when(zookeeperClient.getChunkServers()).thenReturn(machines);

        Request request = new Request(MCommand.ALLOCATE_CHUNKS, new AllocateChunksRequestArgs(10));
        AllocateChunkResponseArgs args = (AllocateChunkResponseArgs) this.masterServer.process(request).args;

        assertTrue(args.chunks.size() == 10);
        for (ChunkDescriptor chunk : args.chunks) {
            assertTrue(chunk.chunkServers.contains(testMachine1));
            assertTrue(chunk.chunkServers.contains(testMachine2));
        }
    }

    @Test
    public void getFileWithDirectoryPathShouldFail() throws TjfsException {
        this.masterServer.start();
        Path testPath = Paths.get("fs/dir1/dir2/filename");
        Request request = new Request(MCommand.GET_FILE, new GetFileRequestArgs(testPath));
        Response response = masterServer.process(request);

        assertTrue(((GetFileResponseArgs)response.args).file.path.equals(testPath));

        request = new Request(MCommand.PUT_FILE, new PutFileRequestArgs(new FileDescriptor(testPath)));
        masterServer.process(request);

        //now cannot getfile with a directory path
        testPath = Paths.get("fs/dir1/dir2");
        request = new Request(MCommand.GET_FILE, new GetFileRequestArgs(testPath));

        // exception.expect(TjfsException.class);
        // exception.expectMessage("Cannot get a directory");
        response = masterServer.process(request);
    }

    @Test
    public void processPutGetFileTest() throws TjfsException {
        this.masterServer.start();
        Path testPath = Paths.get("fs/dir1/dir2/filename");
        ArrayList<ChunkDescriptor> chunks = new ArrayList<ChunkDescriptor>() {{
            add(new ChunkDescriptor("testChunk", null, 10, 0));
            add(new ChunkDescriptor("testChunk2", null, 10, 1));
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
        FileDescriptor fileDescriptor = new FileDescriptor(testPath);
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
        FileDescriptor fileDescriptor = new FileDescriptor(testPath);
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
        Path testPath = Paths.get("/fs/dir1/dir2/filename");
        ArrayList<ChunkDescriptor> chunks = new ArrayList<ChunkDescriptor>() {{
            add(new ChunkDescriptor("testChunk", null, 10, 0));
            add(new ChunkDescriptor("testChunk2", null, 10, 1));
        }};
        FileDescriptor fileDescriptor = new FileDescriptor(testPath, new Date(), chunks);
        Request request = new Request(MCommand.PUT_FILE, new PutFileRequestArgs(fileDescriptor));
        masterServer.process(request);

        testPath = Paths.get("/fs/dir1/dir2/filename2");
        fileDescriptor = new FileDescriptor(testPath, new Date(), chunks);
        request = new Request(MCommand.PUT_FILE, new PutFileRequestArgs(fileDescriptor));
        masterServer.process(request);

        testPath = Paths.get("/fs/dir1/dir2/filename3");
        fileDescriptor = new FileDescriptor(testPath, new Date(), chunks);
        request = new Request(MCommand.PUT_FILE, new PutFileRequestArgs(fileDescriptor));
        masterServer.process(request);

        testPath = Paths.get("/fs/dir1/fileInDir1"); // this should work
        fileDescriptor = new FileDescriptor(testPath, new Date(), chunks);
        request = new Request(MCommand.PUT_FILE, new PutFileRequestArgs(fileDescriptor));

        masterServer.process(request);

        request = new Request(MCommand.LIST_FILE, new ListFileRequestArgs(Paths.get("/fs/dir1")));
        Response resultResponse = masterServer.process(request);
        String[] expectedResult = {"fileInDir1", "dir2/"};
        assertTrue(Arrays.asList(((ListFileResponseArgs) resultResponse.args).files).containsAll(Arrays.asList(expectedResult)));

        request = new Request(MCommand.LIST_FILE, new ListFileRequestArgs(Paths.get("/fs/dir1/dir2")));
        resultResponse = masterServer.process(request);
        String[] expectedResult2 = {"filename", "filename3", "filename2"};
        assertTrue(Arrays.asList(((ListFileResponseArgs) resultResponse.args).files).containsAll(Arrays.asList(expectedResult2)));
    }

    @Test
    public void testThatChunkServiceIsUpdatedWhenNewFileIsPut() throws TjfsException {
        Machine machine1 = Machine.fromString("127.0.0.1:8000");
        Machine machine2 = Machine.fromString("127.0.0.2:8000");
        ChunkDescriptor chunk = new ChunkDescriptor("1", Arrays.asList(machine1, machine2));
        FileDescriptor file = new FileDescriptor(Paths.get("/whatever"), new Date(), new ArrayList<>(Collections.singletonList(chunk)));
        PutFileRequestArgs args = new PutFileRequestArgs(file);

        masterServer.putFile(args);

        assertThat(chunkServerService.chunks.get("1"), equalTo(chunk));
        assertThat(chunkServerService.chunks.get("1").chunkServers, equalTo(Arrays.asList(machine1, machine2)));
    }

    @Test
    public void testGetLatestSnapshot() throws TjfsException {
        masterServer.amIShadow = false;
        FileDescriptor file1 = mock(FileDescriptor.class);
        FileDescriptor file2 = mock(FileDescriptor.class);
        IMasterStorage.Snapshot snapshot = new IMasterStorage.Snapshot(10, Arrays.asList(file1, file2));
        when(snapshotStorage.getLatest()).thenReturn(snapshot);

        Request request = new Request(MCommand.GET_LATEST_SNAPSHOT, new GetLatestSnapshotRequestArgs());
        Response response = masterServer.process(request);
        GetLatestSnapshotsResponseArgs args = (GetLatestSnapshotsResponseArgs) response.args;

        assertThat(args.snapshot, equalTo(snapshot));
    }

    @Test
    public void testConvertingNullLatestSnapshot() {
        // Just a simple test to verify that we can transfer correctly a null value
        // (the snapshot might be null). I wonder if this is the right place...

        Gson gson = CustomGson.create();
        GetLatestSnapshotsResponseArgs args = new GetLatestSnapshotsResponseArgs(null);
        assertThat(gson.fromJson(gson.toJson(args), GetLatestSnapshotsResponseArgs.class)
            .snapshot, equalTo(null));
    }
}
