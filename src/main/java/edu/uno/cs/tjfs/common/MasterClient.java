package edu.uno.cs.tjfs.common;

import edu.uno.cs.tjfs.common.messages.IMessageClient;
import edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient;

import java.nio.file.Path;
import java.util.List;

public class MasterClient implements IZookeeperClient.IMasterServerDownListener, IZookeeperClient
        .IMasterServerUpListener, IMasterClient {
    /** Zookeeper client instance */
    private IZookeeperClient zkClient;

    /** Currently elected master */
    private Machine masterServer;

    public MasterClient(IMessageClient messageClient, IZookeeperClient zkClient) {
        this.zkClient = zkClient;
    }

    public void start() {
        zkClient.addOnMasterServerUpListener(this);
        zkClient.addOnMasterServerDownListener(this);
    }

    @Override
    public void onMasterServerUp(Machine machine) {
        masterServer = machine;
    }

    @Override
    public void onMasterServerDown() {
        masterServer = null;
    }

    @Override
    public List<ChunkDescriptor> allocateChunks(int number) throws TjfsException {
        return null;
    }

    @Override
    public FileDescriptor getFile(Path path) throws TjfsException {
        return null;
    }

    @Override
    public void putFile(FileDescriptor file) throws TjfsException {

    }
}
