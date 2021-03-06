package edu.uno.cs.tjfs.common;

import edu.uno.cs.tjfs.client.TjfsClientException;
import edu.uno.cs.tjfs.common.messages.*;
import edu.uno.cs.tjfs.common.messages.arguments.*;
import edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient;
import edu.uno.cs.tjfs.common.zookeeper.ZookeeperException;
import edu.uno.cs.tjfs.master.IMasterStorage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class MasterClientTest {
    private MasterClient masterClient;

    @Mock
    IZookeeperClient zookeeperClient;

    @Mock
    IMessageClient messageClient;

    @Before
    public void setUp() throws IOException {
        initMocks(this);
        masterClient = new MasterClient(messageClient, zookeeperClient);
    }

    @Test
    public void allocateChunkShouldCallMessageClientTest() throws TjfsException {
        Machine machine = new Machine("127.0.0.1", 6002);
        when(zookeeperClient.getMasterServer()).thenReturn(machine);
        Request request = new Request(MCommand.ALLOCATE_CHUNKS, new AllocateChunksRequestArgs(10));
        ArrayList<ChunkDescriptor> chunks = new ArrayList<ChunkDescriptor>(){{
            add(new ChunkDescriptor("testChunk", null));
            add(new ChunkDescriptor("testChunk2", null));
        }};
        Response response = Response.Success(new AllocateChunkResponseArgs(chunks));
        when(messageClient.send(machine, request)).thenReturn(response);
        List<ChunkDescriptor> result = masterClient.allocateChunks(10);
        verify(messageClient).send(machine, request);
        assertTrue(result.size() == 2);
        assertTrue(result.get(0).name.equals("testChunk"));
        assertTrue(result.get(1).name.equals("testChunk2"));
    }

    @Test
    public void getFileShouldCallMessageClientTest() throws TjfsException {
        Machine machine = new Machine("127.0.0.1", 6002);
        Path testPath = Paths.get("testPath");
        Request request = new Request(MCommand.GET_FILE, new GetFileRequestArgs(testPath));
        when(zookeeperClient.getMasterServer()).thenReturn(machine);
        ArrayList<ChunkDescriptor> chunks = new ArrayList<ChunkDescriptor>(){{
            add(new ChunkDescriptor("testChunk", null));
            add(new ChunkDescriptor("testChunk2", null));
        }};
        Response response = Response.Success(new GetFileResponseArgs(new FileDescriptor(testPath, new Date(), chunks)));
        when(messageClient.send(machine, request)).thenReturn(response);
        FileDescriptor result = masterClient.getFile(testPath);
        verify(messageClient).send(machine, request);
        assertTrue(result.path.equals(testPath));
        assertTrue(result.chunks.get(0).name.equals("testChunk"));
        assertTrue(result.chunks.get(1).name.equals("testChunk2"));
    }

    @Test
    public void putFileShouldCallMessageClientTest() throws TjfsException {
        Machine machine = new Machine("127.0.0.1", 6002);
        Path testPath = Paths.get("testPath");
        ArrayList<ChunkDescriptor> chunks = new ArrayList<ChunkDescriptor>(){{
            add(new ChunkDescriptor("testChunk", null));
            add(new ChunkDescriptor("testChunk2", null));
        }};
        FileDescriptor fileDescriptor = new FileDescriptor(testPath, new Date(), chunks);
        Request request = new Request(MCommand.PUT_FILE, new PutFileRequestArgs(fileDescriptor));
        when(zookeeperClient.getMasterServer()).thenReturn(machine);

        Response response = Response.Success(new PutFileResponseArgs(""));
        when(messageClient.send(machine, request)).thenReturn(response);
        masterClient.putFile(fileDescriptor);
        verify(messageClient).send(machine, request);
    }

    @Test
    public void getLogShouldCallMessageClientTest() throws TjfsException {
        Machine machine = new Machine("127.0.0.1", 6002);
        Path testPath = Paths.get("testPath");
        ArrayList<ChunkDescriptor> chunks = new ArrayList<ChunkDescriptor>(){{
            add(new ChunkDescriptor("testChunk", null));
            add(new ChunkDescriptor("testChunk2", null));
        }};
        FileDescriptor fileDescriptor = new FileDescriptor(testPath, new Date(), chunks);
        ArrayList<IMasterStorage.LogItem> logs = new ArrayList<IMasterStorage.LogItem>(){{
            add(new IMasterStorage.LogItem(0, fileDescriptor));
        }};
        Request request = new Request(MCommand.GET_LOG, new GetLogRequestArgs(10));
        when(zookeeperClient.getMasterServer()).thenReturn(machine);

        Response response = Response.Success(new GetLogResponseArgs(logs));
        when(messageClient.send(machine, request)).thenReturn(response);
        List<IMasterStorage.LogItem> result = masterClient.getLog(10);
        verify(messageClient).send(machine, request);
        assertTrue(result.size() == 1);
        assertTrue(result.get(0).file.equals(fileDescriptor));
    }

    @Test
    public void listFileShouldCallMessageClientTest() throws TjfsException {
        Machine machine = new Machine("127.0.0.1", 6002);
        Path testPath = Paths.get("testPath");
        Request request = new Request(MCommand.LIST_FILE, new ListFileRequestArgs(testPath));
        when(zookeeperClient.getMasterServer()).thenReturn(machine);
        String[] testResult = {"file1", "file2"};
        Response response = Response.Success(new ListFileResponseArgs(testResult));
        when(messageClient.send(machine, request)).thenReturn(response);
        String[] result = masterClient.list(testPath);
        verify(messageClient).send(machine, request);
        assertTrue(result.length == 2);
        assertTrue(testResult.equals(result));
    }
}
