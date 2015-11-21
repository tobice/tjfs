package edu.uno.cs.tjfs.common.zookeeper;

import edu.uno.cs.tjfs.common.Machine;

import java.nio.file.Path;
import java.util.List;

public interface IZookeeperClient {
    enum LockType { READ, WRITE }

    interface IChunkServerUpListener {
        void onChunkServerUp(Machine machine);
    }
    interface IChunkServerDownListener {
        void onChunkServeDown(Machine machine);
    }
    interface IMasterServerUpListener {
        void onMasterServerUp(Machine machine);
    }
    interface IMasterServerDownListener {
        void onMasterServerDown();
    }

    void addOnChunkServerUpListener(IChunkServerUpListener listener);
    void addOnChunkServerDownListener(IChunkServerDownListener listener);
    void addOnMasterServerUpListener(IMasterServerUpListener listener);
    void addOnMasterServerDownListener(IMasterServerDownListener listener);

    /**
     * Get current master server.
     * @return current master's IP and port
     * @throws ZnodeMissingException if no master is registered with Zookeeper
     * @throws ZookeeperDownException if the connection with Zookeeper has been broken
     */
    Machine getMasterServer() throws ZnodeMissingException, ZookeeperDownException;

    /**
     * Get list of chunk servers.
     * @return list of chunk server currently registered with Zookeeper
     * @throws ZookeeperDownException if the connection with Zookeeper has been broken
     */
    List<Machine> getChunkServers() throws ZookeeperDownException;

    /**
     * Register machine as new master.
     * @param machine that should be registered as new master
     * @throws ZnodeTakenException if there is already a master registered
     * @throws ZookeeperDownException if the connection with Zookeeper has been broken
     */
    void registerMasterServer(Machine machine) throws ZnodeTakenException, ZookeeperDownException;

    /**
     * Register machine as a new chunk server
     * @param machine that should be registered as a new chunk server
     * @throws ZnodeTakenException if there is already the same chunk server registered
     * @throws ZookeeperDownException if the connection with Zookeeper has been broken
     */
    void registerChunkServer(Machine machine) throws ZnodeTakenException, ZookeeperDownException;

    /**
     * Acquire lock for given file.
     * @param path to the file that should be locked
     * @param lockType type of lock, either READ or WRITE
     * @throws ZnodeTakenException if the file is already locked
     * @throws ZookeeperDownException if the connection with Zookeeper has been broken
     */
    void acquireFileLock(Path path, LockType lockType) throws ZnodeTakenException, ZookeeperDownException;

    /**
     * Release lock from given file.
     * @param path to the file whose lock should be removed
     * @param lockType type of lock, either READ or WRITE
     * @throws ZookeeperDownException if the connection with Zookeeper has been broken
     */
    void releaseFileLock(Path path, LockType lockType) throws ZookeeperDownException;
}
