package edu.uno.cs.tjfs.common;

import edu.uno.cs.tjfs.common.messages.*;
import edu.uno.cs.tjfs.common.messages.arguments.*;
import edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient;

import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.nio.file.Path;
import java.util.List;

public class MasterClient implements IZookeeperClient.IMasterServerDownListener, IZookeeperClient
        .IMasterServerUpListener, IMasterClient {
    /**
     * Zookeeper client instance
     */
    private IZookeeperClient zkClient;

    private MessageClient messageClient;

    /**
     * Currently elected master
     */
    private Machine masterServer;

    public MasterClient(MessageClient messageClient, IZookeeperClient zkClient) {
        this.messageClient = messageClient;
        this.zkClient = zkClient;
    }

    public void start() {
        zkClient.addOnMasterServerUpListener(this);
        zkClient.addOnMasterServerDownListener(this);
    }

    @Override
    public void onMasterServerUp(Machine masterServer) {
        this.masterServer = masterServer;
    }

    @Override
    public void onMasterServerDown() {
        masterServer = null;
    }

    private Machine getMasterServer() throws TjfsException {
        if (masterServer == null) {
            throw new TjfsException("Master server is down!");
        }

        return masterServer;
    }

    @Override
    public List<ChunkDescriptor> allocateChunks(int number) throws TjfsException {
        Request request = new Request(MCommand.ALLOCATE_CHUNKS, new AllocateChunksRequestArgs(number), null, 0);
        Response response = this.messageClient.send(getMasterServer(), request);
        return ((AllocateChunkResponseArgs) response.args).chunks;
    }

    @Override
    public FileDescriptor getFile(Path path) throws TjfsException {
        Request request = new Request(MCommand.GET_FILE, new GetFileRequestArgs(path), null, 0);
        Response response = this.messageClient.send(masterServer, request);
        return ((GetFileResponseArgs) response.args).file;
    }

    @Override
    public void putFile(FileDescriptor file) throws TjfsException {
        Request request = new Request(MCommand.PUT_FILE, new PutFileRequestArgs(file), null, 0);
        this.messageClient.send(masterServer, request);
    }

    @Override
    public List<FileDescriptor> getLog(int logID) throws TjfsException{
        Request request = new Request(MCommand.GET_LOG, new GetLogRequestArgs(logID));
        Response response = this.messageClient.send(masterServer, request);
        return ((GetLogResponseArgs)response.args).logs;
    }

    @Override
    public String[] list(Path path) throws TjfsException {
        return new String[0];
    }
}
