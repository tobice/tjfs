package edu.uno.cs.tjfs.common;

import edu.uno.cs.tjfs.common.messages.*;
import edu.uno.cs.tjfs.common.messages.arguments.*;
import edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient;
import edu.uno.cs.tjfs.master.IMasterStorage;

import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.nio.file.Path;
import java.util.List;

public class MasterClient implements IMasterClient {
    private IZookeeperClient zkClient;
    private IMessageClient messageClient;

    public MasterClient(IMessageClient messageClient, IZookeeperClient zkClient) {
        this.messageClient = messageClient;
        this.zkClient = zkClient;
    }

    private Machine getMasterServer() throws TjfsException {
        Machine masterServer = zkClient.getMasterServer();
        if (masterServer == null) {
            throw new TjfsException("Master server is down!");
        }

        return masterServer;
    }

    @Override
    public List<ChunkDescriptor> allocateChunks(int number) throws TjfsException {
        Request request = new Request(MCommand.ALLOCATE_CHUNKS, new AllocateChunksRequestArgs(number));
        Response response = this.messageClient.send(getMasterServer(), request);
        return ((AllocateChunkResponseArgs) response.args).chunks;
    }

    @Override
    public FileDescriptor getFile(Path path) throws TjfsException {
        Request request = new Request(MCommand.GET_FILE, new GetFileRequestArgs(path));
        Response response = this.messageClient.send(getMasterServer(), request);
        return ((GetFileResponseArgs) response.args).file;
    }

    @Override
    public void putFile(FileDescriptor file) throws TjfsException {
        Request request = new Request(MCommand.PUT_FILE, new PutFileRequestArgs(file));
        this.messageClient.send(getMasterServer(), request);
    }

    @Override
    public List<IMasterStorage.LogItem> getLog(int lastVersion) throws TjfsException {
        Request request = new Request(MCommand.GET_LOG, new GetLogRequestArgs(lastVersion));
        Response response = this.messageClient.send(getMasterServer(), request);
        return ((GetLogResponseArgs)response.args).log;
    }

    @Override
    public String[] list(Path path) throws TjfsException {
        Request request = new Request(MCommand.LIST_FILE, new ListFileRequestArgs(path));
        Response response = this.messageClient.send(getMasterServer(), request);
        return ((ListFileResponseArgs) response.args).files;
    }

    @Override
    public IMasterStorage.Snapshot getLatestSnapshot() throws TjfsException {
        Request request = new Request(MCommand.GET_LATEST_SNAPSHOT, new GetLatestSnapshotRequestArgs());
        Response response = this.messageClient.send(getMasterServer(), request);
        return ((GetLatestSnapshotsResponseArgs) response.args).snapshot;
    }
}
