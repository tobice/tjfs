package edu.uno.cs.tjfs.master;

import edu.uno.cs.tjfs.common.ChunkClient;
import edu.uno.cs.tjfs.common.ChunkDescriptor;
import edu.uno.cs.tjfs.common.FileDescriptor;
import edu.uno.cs.tjfs.common.Machine;
import edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient;
import edu.uno.cs.tjfs.common.zookeeper.ZookeeperDownException;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class ChunkServerService implements IZookeeperClient.IChunkServerUpListener,
        IZookeeperClient.IChunkServerDownListener {
    IZookeeperClient zkClient;
    ChunkClient chunkClient;

    List<Machine> chunkServers;

    /** The up-to-date chunk descriptors with running chunk servers */
    List<ChunkDescriptor> chunks;

    public ChunkServerService(IZookeeperClient zkClient) {
        this.zkClient = zkClient;
    }

    public void start() throws ZookeeperDownException {
        zkClient.addOnChunkServerDownListener(this);
        zkClient.addOnChunkServerUpListener(this);
        chunkServers = zkClient.getChunkServers();

        // TODO: for each chunk server, get chunks and add them to mappings
    }

    @Override
    public void onChunkServeDown(Machine machine) {
        chunkServers.remove(machine);

        for (ChunkDescriptor chunk : chunks) {
            chunk.chunkServers.remove(machine);

            if (chunk.chunkServers.size() < 2) {
                // TODO: do the replication
            }
        }

        // TODO: init chunk replication
    }

    @Override
    public void onChunkServerUp(Machine machine) {
        chunkServers.add(machine);

        // TODO: load chunks from chunk server and add it to mappings
    }

    public List<Machine> getChunkServers() {
        return chunkServers;
    }

    public List<Machine> getRandomChunkServers(int number) {
        Collections.shuffle(chunkServers);
        return new LinkedList<>(chunkServers.subList(0, number));
    }

    public FileDescriptor updateChunkServers(FileDescriptor fileDescriptor) {
        // TODO: maybe create new FileDescriptor since it's immutable
        for (ChunkDescriptor chunk : fileDescriptor.chunks) {
            // update chunk.chunkServers based on current mappings
        }
        return null;
    }
}
