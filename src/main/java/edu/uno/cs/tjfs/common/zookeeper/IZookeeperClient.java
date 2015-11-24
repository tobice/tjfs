package edu.uno.cs.tjfs.common.zookeeper;

import edu.uno.cs.tjfs.common.Machine;

import java.nio.file.Path;
import java.util.List;

public interface IZookeeperClient {
    enum LockType {
        READ("read"), WRITE("write"), MACHINE("machine");
        protected String value;

        LockType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

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
     * @throws ZookeeperException.NoMasterRegisteredException if no master is registered with Zookeeper
     * @throws ZookeeperException general Zookeeper failure
     */
    Machine getMasterServer() throws ZookeeperException;

    /**
     * Get list of chunk servers.
     * @return list of chunk server currently registered with Zookeeper
     * @throws ZookeeperException general Zookeeper failure
     */
    List<Machine> getChunkServers() throws ZookeeperException;

    /**
     * Register machine as new master.
     * @param machine that should be registered as new master
     * @throws ZookeeperException.MasterAlreadyExistsException if there is already a master registered
     * @throws ZookeeperException general Zookeeper failure
     */
    void registerMasterServer(Machine machine) throws ZookeeperException;

    /**
     * Register machine as a new chunk server
     * @param machine that should be registered as a new chunk server
     * @throws ZookeeperException.ChunkServerExistsException if there is already the same chunk server registered
     * @throws ZookeeperException general Zookeeper failure
     */
    void registerChunkServer(Machine machine) throws ZookeeperException;

    /**
     * Acquire lock for given file.
     * @param path to the file that should be locked
     * @param lockType type of lock, either READ or WRITE
     * @throws ZookeeperException.FileLockedException if the file is already locked
     * @throws ZookeeperException general Zookeeper failure
     */
    void acquireFileLock(Path path, LockType lockType) throws ZookeeperException;

    /**
     * Release lock from given file.
     * @param path to the file whose lock should be removed
     * @param lockType type of lock, either READ or WRITE
     * @throws ZookeeperException general Zookeeper failure
     */
    void releaseFileLock(Path path, LockType lockType) throws ZookeeperException;
}
