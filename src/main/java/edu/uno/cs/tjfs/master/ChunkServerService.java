package edu.uno.cs.tjfs.master;

import edu.uno.cs.tjfs.chunkserver.ChunkServer;
import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient;
import edu.uno.cs.tjfs.common.zookeeper.ZookeeperDownException;

import java.util.*;
import java.util.stream.Collectors;


public class ChunkServerService implements IZookeeperClient.IChunkServerUpListener,
        IZookeeperClient.IChunkServerDownListener {
    IZookeeperClient zkClient;
    IChunkClient chunkClient;
    List<Machine> chunkServers;
    /** The up-to-date chunk descriptors with running chunk servers */
    List<ChunkDescriptor> chunks;

    public ChunkServerService(IZookeeperClient zkClient, IChunkClient chunkClient) {
        this.zkClient = zkClient;
        this.chunkClient = chunkClient;
        this.chunkServers = new ArrayList<>();
        this.chunks = new ArrayList<>();
    }

    public void start() throws ZookeeperDownException {
        zkClient.addOnChunkServerDownListener(this);
        zkClient.addOnChunkServerUpListener(this);
        chunkServers = zkClient.getChunkServers();

        // For each chunk server, get chunks and add them to mappings
        for(Machine machine : chunkServers){
            try {
                updateChunkMappingsFromMachine(machine);
            } catch (TjfsException e) {
                BaseLogger.error("ChunkServerService.start - ", e);
            }
        }
    }

    @Override
    public void onChunkServerDown(Machine machine) {
        chunkServers.remove(machine);
        //This is going to go through all the chunks not just the ones on that machine
        List<ChunkDescriptor> tempChunks = chunks.stream().filter(x->x.chunkServers.contains(machine)).collect(Collectors.toList());
        for (ChunkDescriptor chunk : tempChunks) {
            chunk.chunkServers.remove(machine);
            if (chunk.chunkServers.size() < 2) {
                try {
                    //Get a new machine to replicate to
                    Machine machineToReplicateTo = getRandomChunkServerNotEqualTo(chunk.chunkServers.get(0));
                    //Replicate synchronously
                    this.chunkClient.replicateSync(
                            chunk.chunkServers.get(0),
                            machineToReplicateTo,
                            chunk.name);
                    //After the chunk is replicated to that server..add it to the chunkservers list of chunks
                    chunk.chunkServers.add(machineToReplicateTo);
                } catch (TjfsException e) {
                    BaseLogger.error("ChunkServerService.onChunkServerDown - Cannot Run the Chunk Replication");
                    BaseLogger.error("ChunkServerService.onChunkServerDown - ", e);
                }
            }
        }
        // TODO: init chunk replication??
    }

    @Override
    public void onChunkServerUp(Machine machine) {
        if (!chunkServers.contains(machine)) chunkServers.add(machine);
        try {
            updateChunkMappingsFromMachine(machine);
        }catch(Exception e){
            BaseLogger.error("ChunkServerService.onChunkServerUp - Cannot get chunks.");
            BaseLogger.error("ChunkServerService.onChunkServerUp - ", e);
        }
    }

    private void updateChunkMappingsFromMachine(Machine machine) throws TjfsException {
        String[] listOfChunks = this.chunkClient.list(machine);
        for(String chunkName : listOfChunks){
            ChunkDescriptor chunkDescriptor = this.chunks.stream().filter(x->x.name.equals(chunkName)).findFirst().orElse(null);
            if (chunkDescriptor != null && !chunkDescriptor.chunkServers.contains(machine)){
                chunkDescriptor.chunkServers.add(machine);

                this.chunks.set(this.chunks.indexOf(chunkDescriptor), chunkDescriptor);
            }
            else if (chunkDescriptor == null){
                ArrayList<Machine> machines = new ArrayList<>();
                machines.add(machine);
                this.chunks.add(new ChunkDescriptor(chunkName, machines));
            }
        }
    }

    public List<Machine> getChunkServers() {
        return chunkServers;
    }

    public List<Machine> getRandomChunkServers(int number) {
        Collections.shuffle(chunkServers);
        return new LinkedList<>(chunkServers.subList(0, number));
    }

    public Machine getRandomChunkServerNotEqualTo(Machine machine) {
        Collections.shuffle(chunkServers);
        Machine returnMachine = chunkServers.subList(0, 1).get(0);
        while (returnMachine.equals(machine)) {
            Collections.shuffle(chunkServers);
            returnMachine = chunkServers.subList(0, 1).get(0);
        }
        return returnMachine;
    }

    public FileDescriptor updateChunkServers(FileDescriptor fileDescriptor) {
        // TODO: maybe create new FileDescriptor since it's immutable
        for (ChunkDescriptor chunk : fileDescriptor.chunks) {
            // update chunk.chunkServers based on current mappings
        }
        return null;
    }
}
