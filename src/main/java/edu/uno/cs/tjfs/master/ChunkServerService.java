package edu.uno.cs.tjfs.master;

import edu.uno.cs.tjfs.chunkserver.ChunkServer;
import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient;
import edu.uno.cs.tjfs.common.zookeeper.ZookeeperException;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;


public class ChunkServerService implements IZookeeperClient.IChunkServerUpListener,
        IZookeeperClient.IChunkServerDownListener {
    IZookeeperClient zkClient;
    IChunkClient chunkClient;
    /** The up-to-date chunk descriptors with running chunk servers */
    Map<String, ChunkDescriptor> chunks;
    final static Logger logger = BaseLogger.getLogger(ChunkServerService.class);

    public ChunkServerService(IZookeeperClient zkClient, IChunkClient chunkClient) {
        this.zkClient = zkClient;
        this.chunkClient = chunkClient;
        this.chunks = new HashMap<>();
    }

    public void start() throws ZookeeperException {
        zkClient.addOnChunkServerDownListener(this);
        zkClient.addOnChunkServerUpListener(this);
        // For each chunk server, get chunks and add them to mappings
        for(Machine machine : zkClient.getChunkServers()){
            try {
                updateChunkMappingsFromMachine(machine);
            } catch (TjfsException e) {
                logger.error("ChunkServerService.start - ", e);
            }
        }
    }

    private List<ChunkDescriptor> getChunksFromMachine(Machine machine){
        List<ChunkDescriptor> result = new ArrayList<>();
        Iterator it = chunks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();

            if (((ChunkDescriptor) pair.getValue()).chunkServers.contains(machine)){
                result.add((ChunkDescriptor) pair.getValue());
            }
        }
        return result;
    }

    @Override
    public void onChunkServerDown(Machine machine) {
        //This is going to go through all the chunks not just the ones on that machine
        List<ChunkDescriptor> tempChunks = getChunksFromMachine(machine);
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
                    chunk.chunkServers.add(machineToReplicateTo);
                } catch (TjfsException e) {
                    logger.error("ChunkServerService.onChunkServerDown - Cannot Run the Chunk Replication");
                    logger.error("ChunkServerService.onChunkServerDown - ", e);
                }
            }
        }
        // TODO: init chunk replication??
    }

    @Override
    public void onChunkServerUp(Machine machine) {
        try {
            updateChunkMappingsFromMachine(machine);
        }catch(Exception e){
            logger.error("ChunkServerService.onChunkServerUp - Cannot get chunks.");
            logger.error("ChunkServerService.onChunkServerUp - ", e);
        }
    }

    private void updateChunkMappingsFromMachine(Machine machine) throws TjfsException {
        String[] listOfChunks = this.chunkClient.list(machine);
        for(String chunkName : listOfChunks){
            ChunkDescriptor chunkDescriptor = this.chunks.get(chunkName);
            if (chunkDescriptor != null && !chunkDescriptor.chunkServers.contains(machine)){
                chunkDescriptor.chunkServers.add(machine);

                this.chunks.put(chunkName, chunkDescriptor);
            }
            else if (chunkDescriptor == null){
                ArrayList<Machine> machines = new ArrayList<>();
                machines.add(machine);
                this.chunks.put(chunkName, new ChunkDescriptor(chunkName, machines));
            }
        }
    }

    public List<Machine> getChunkServers() {
        return zkClient.getChunkServers();
    }

    public List<Machine> getRandomChunkServers(int number) {
        List<Machine> chunkServers = getChunkServers();
        Collections.shuffle(chunkServers);
        return new LinkedList<>(chunkServers.subList(0, number));
    }

    public Machine getRandomChunkServerNotEqualTo(Machine machine) {
        List<Machine> chunkServers = getChunkServers();
        Collections.shuffle(chunkServers);
        Machine returnMachine = chunkServers.subList(0, 1).get(0);
        while (returnMachine.equals(machine)) {
            Collections.shuffle(chunkServers);
            returnMachine = chunkServers.subList(0, 1).get(0);
        }
        return returnMachine;
    }

    public FileDescriptor updateChunkServers(FileDescriptor fileDescriptor) throws TjfsException {
        // TODO: maybe create new FileDescriptor since it's immutable
        ArrayList<ChunkDescriptor> updatedChunkMappings = new ArrayList<>();
        if (fileDescriptor.chunks != null)
            for (ChunkDescriptor chunk : fileDescriptor.chunks) {
                ChunkDescriptor updatedDescriptor = this.chunks.get(chunk.name);
                if (updatedDescriptor == null) throw new TjfsException("Invalid chunks in the file");
                updatedChunkMappings.add(updatedDescriptor);
            }
        return new FileDescriptor(fileDescriptor.path, fileDescriptor.time, updatedChunkMappings);
    }
}
