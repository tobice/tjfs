package edu.uno.cs.tjfs.master;

import edu.uno.cs.tjfs.chunkserver.ChunkServer;
import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.threads.JobExecutor;
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

    @Override
    public void onChunkServerDown(Machine machine) {
        try {
            // Replicate chunks so that there are two copies of each on different servers.
            List<ReplicateChunkJob> jobs = generateReplicateJobs(machine);
            ReplicateChunkJobProducer producer = new ReplicateChunkJobProducer(jobs);
            JobExecutor executor = new JobExecutor(producer, 20, 20);
            executor.execute();
        } catch (TjfsException e) {
            logger.error("Chunk replication failed. " + e.getMessage(), e);
        }
    }

    @Override
    public void onChunkServerUp(Machine machine) {
        try {
            updateChunkMappingsFromMachine(machine);
        } catch(Exception e){
            logger.error("ChunkServerService.onChunkServerUp - Cannot get chunks.");
            logger.error("ChunkServerService.onChunkServerUp - ", e);
        }
    }

    /**
     * Generate list of replicate jobs that will replicate those chunks whose copies disappeared
     * with the given machine going down
     * @param machineDown the machine that wen down
     * @return list of replicate chunk jobs that should bring back the balance
     */
    protected List<ReplicateChunkJob> generateReplicateJobs(Machine machineDown) {
        return chunks.values().stream()
            .filter(chunk -> chunk.chunkServers.contains(machineDown))
            .map(chunk -> {
                chunk.chunkServers.remove(machineDown);
                List<Machine> targetServers = getRandomChunkServers(2, chunk.chunkServers);
                return chunk.chunkServers.size() < 2 ?
                    new ReplicateChunkJob(chunkClient, chunk, targetServers) : null;
            })
            .filter(job -> job != null)
            .collect(Collectors.toList());
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

    protected List<Machine> getChunkServers() {
        return zkClient.getChunkServers();
    }

    /**
     * Get random chunk servers.
     * @param number how many chunk servers should be returned
     * @return list of random chunkservers of the maximum size of number
     */
    protected List<Machine> getRandomChunkServers(int number) {
        List<Machine> chunkServers = getChunkServers();
        Collections.shuffle(chunkServers);
        return new LinkedList<>(chunkServers.subList(0, Math.min(number, chunkServers.size())));
    }

    /**
     * Get list of random target chunk servers that are different from the ones contained in the
     * avoid collection
     * @param number how many chunk servers should be returned
     * @param avoid chunk servers that should be avoided
     * @return list of random chunkservers of the maximum size of number
     */
    protected List<Machine> getRandomChunkServers(int number, List<Machine> avoid) {
        List<Machine> chunkServers = getChunkServers().stream()
            .filter(server -> !avoid.contains(server))
            .collect(Collectors.toList());
        Collections.shuffle(chunkServers);
        return new LinkedList<>(chunkServers.subList(0, Math.min(number, chunkServers.size())));
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
