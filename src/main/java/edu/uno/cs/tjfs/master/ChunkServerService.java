package edu.uno.cs.tjfs.master;

import edu.uno.cs.tjfs.chunkserver.ChunkServer;
import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.threads.JobExecutor;
import edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient;
import edu.uno.cs.tjfs.common.zookeeper.ZookeeperException;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;


public class ChunkServerService implements IZookeeperClient.IChunkServerUpListener, IZookeeperClient.IChunkServerDownListener {
    final static Logger logger = BaseLogger.getLogger(ChunkServerService.class);

    IZookeeperClient zkClient;
    IChunkClient chunkClient;

    /** The up-to-date chunk descriptors with running chunk servers */
    Map<String, ChunkDescriptor> chunks;

    /** Whether or not should the service keep in sync with the chunk servers */
    boolean synchronization = false;

    public ChunkServerService(IZookeeperClient zkClient, IChunkClient chunkClient) {
        this.zkClient = zkClient;
        this.chunkClient = chunkClient;
        this.chunks = new HashMap<>();
    }

    /** Init the service so that it start listening to Zookeeper. */
    public void start()  {
        zkClient.addOnChunkServerDownListener(this);
        zkClient.addOnChunkServerUpListener(this);
    }

    /**
     * Initialize synchronization between chunk servers and this service. The service will be
     * maintaining the availability of individual chunks on chunk servers.
     **/
    public void startSynchronization() {
        synchronization = true;

        // Get the current state (what chunks are available on chunk servers).
        chunks = new HashMap<>();
        for(Machine machine : zkClient.getChunkServers()){
            try {
                updateChunkMappingsFromMachine(machine);
            } catch (TjfsException e) {
                logger.error("ChunkServerService.start - ", e);
            }
        }
    }

    /** Stop synchronization */
    public void stopSynchronization() {
        synchronization = false;
    }

    /**
     * Takes a file descriptor and updates all chunks with latest known locations of the chunks on
     * chunk servers (so that the file can be downloaded).
     * @param fileDescriptor file
     * @return new file descriptor that contains up-to-date locations of chunks.
     * @throws TjfsException
     */
    public FileDescriptor updateChunkServers(FileDescriptor fileDescriptor) throws TjfsException {
        ArrayList<ChunkDescriptor> updatedChunkMappings = new ArrayList<>();
        if (fileDescriptor.chunks != null)
            for (ChunkDescriptor chunk : fileDescriptor.chunks) {
                if (chunks.get(chunk.name)== null)  {
                    throw new TjfsException("Invalid chunks in the file");
                }

                // We have to merge information from both sources.
                ChunkDescriptor updatedDescriptor = new ChunkDescriptor(
                        chunk.name, chunks.get(chunk.name).chunkServers, chunk.size, chunk.index);
                updatedChunkMappings.add(updatedDescriptor);
            }
        return new FileDescriptor(fileDescriptor.path, fileDescriptor.time, updatedChunkMappings);
    }

    /**
     * Add new chunks. The service simply trusts that the given information is alright, it doesn't
     * check that those chunks are actually available on those chunk servers.
     * @param chunks list of chunks with their availabilities on chunk servers.
     */
    public void addChunks(List<ChunkDescriptor> chunks) {
        if (chunks != null) {
            chunks.forEach(chunk -> this.chunks.put(chunk.name, chunk));
        }
    }

    @Override
    public void onChunkServerDown(Machine machine) {
        if (!synchronization) {
            return;
        }

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
        if (!synchronization) {
            return;
        }

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
}
