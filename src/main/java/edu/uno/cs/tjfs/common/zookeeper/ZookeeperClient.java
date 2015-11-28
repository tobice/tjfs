package edu.uno.cs.tjfs.common.zookeeper;

import edu.uno.cs.tjfs.common.Machine;
import org.apache.log4j.Level;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;

import static edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient.LockType.*;

/**
 * ZookeeperClient implementation. Provides API for remote operations like locking files,
 * registering chunk servers and electing new master server.
 *
 * The client maintains an up-to-date list of currently active chunk servers and the currently
 * elected master server. The synchronization is achieved via Zookeeper event system. The result
 * is that using the client to get current master or the chunk servers is really fast as local
 * values are used and no remote calls are made. The event system should guarantee that the local
 * values are reasonably "fresh".
 */
public class ZookeeperClient implements IZookeeperClient {

    /** Base znodes in the Zookeepers file structure */
    enum Znode {
        /** Znode where the master is registered */
        MASTER("/master"),

        /** Znode where the chunk servers are registered */
        CHUNKSERVERS("/chunkservers"),

        /** Znode where the individual file locks are registered */
        FS("/fs");

        protected final String value;
        Znode(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    final static Logger logger = Logger.getLogger(ZookeeperClient.class);

    /** Actual active Zookeeper client instance */
    protected final ZooKeeper zk;

    /** Data monitor receiving events from Zookeeper */
    protected final DataMonitor dataMonitor;

    /**
     * Currently held locks. Pairs of node path and actual path (with the counter suffix). It's
     * necessary to remember it so that we can which node to remove when releasing locks.
     */
    protected final Map<String, String> currentLocks = new HashMap<>();

    /** List of currently registered chunk servers. Synchronized with Zookeeper */
    protected List<Machine> chunkServers = new LinkedList<>();

    /** Currently registered master server. Synchronized with Zookeeper */
    protected Machine masterServer;

    // Event listeners
    List<IChunkServerUpListener> chunkServerUpListeners = new LinkedList<>();
    List<IChunkServerDownListener> chunkServerDownListeners = new LinkedList<>();
    List<IMasterServerUpListener> masterServerUpListeners = new LinkedList<>();
    List<IMasterServerDownListener> masterServerDownListeners = new LinkedList<>();
    List<IConnectionLostListener> connectionLostListeners = new LinkedList<>();

    protected ZookeeperClient(ZooKeeper zk, DataMonitor dataMonitor) {
        this.zk = zk;
        this.dataMonitor = dataMonitor;
    }

    /** Start the client, initialize the base structure and start watching for changes */
    public void start() throws ZookeeperException {
        setupStructure();
        setupWatches();
    }


    // Public API implementing the IZookeeper interface

    @Override
    public void addOnChunkServerUpListener(IChunkServerUpListener listener) {
        chunkServerUpListeners.add(listener);
    }

    @Override
    public void addOnChunkServerDownListener(IChunkServerDownListener listener) {
        chunkServerDownListeners.add(listener);
    }

    @Override
    public void addOnMasterServerUpListener(IMasterServerUpListener listener) {
        masterServerUpListeners.add(listener);
    }

    @Override
    public void addOnMasterServerDownListener(IMasterServerDownListener listener) {
        masterServerDownListeners.add(listener);
    }

    @Override
    public void addOnConnectionLostListener(IConnectionLostListener listener) {
        connectionLostListeners.add(listener);
    }

    /**
     * Get current master server.
     *
     * This method is really fast as it returns a local value that
     * is being synchronized with Zookeeper via event system.
     *
     * @return current master's IP and port
     * @throws ZookeeperException.NoMasterRegisteredException if no master is registered with Zookeeper
     */
    @Override
    public Machine getMasterServer() throws ZookeeperException.NoMasterRegisteredException {
        if (masterServer == null) {
            throw new ZookeeperException.NoMasterRegisteredException();
        }
        return masterServer;
    }

    /**
     * Get list of chunk servers.
     *
     * This method is really fast as it returns a local value that
     * is being synchronized with Zookeeper via event system.
     *
     * @return list of chunk server currently registered with Zookeeper
     */
    @Override
    public List<Machine> getChunkServers() {
        return chunkServers;
    }

    /**
     * Register machine as new master.
     * @param machine that should be registered as new master
     * @throws ZookeeperException.MasterAlreadyExistsException if there is already a master registered
     * @throws ZookeeperException general Zookeeper failure
     */
    @Override
    public void registerMasterServer(Machine machine) throws ZookeeperException {
        try {
            acquireLock(Znode.MASTER.toString(), MACHINE, null, machine.getBytes());
        } catch (ZookeeperException.NodeLockedException e) {
            throw new ZookeeperException.MasterAlreadyExistsException(e);
        }
    }

    /**
     * Register machine as a new chunk server
     * @param machine that should be registered as a new chunk server
     * @throws ZookeeperException.ChunkServerExistsException if there is already the same chunk server registered
     * @throws ZookeeperException general Zookeeper failure
     */
    @Override
    public void registerChunkServer(Machine machine) throws ZookeeperException {
        try {
            create(Znode.CHUNKSERVERS + "/" + machine, new byte[0], CreateMode.EPHEMERAL);
        } catch (ZookeeperException.NodeExistsException e) {
            // This is not really a true synchronization mechanism. But since a chunk server is
            // defined by an ip and a port, it's safe to expect that collisions won't happen
            // frequently and if they do then there is something seriously wrong with the setup.
            throw new ZookeeperException.ChunkServerExistsException(e);
        }
    }

    /**
     * Acquire lock for given file.
     * @param path to the file that should be locked
     * @param lockType type of lock, either READ or WRITE
     * @throws ZookeeperException.FileLockedException if the file is already locked
     * @throws ZookeeperException general Zookeeper failure
     */
    @Override
    public void acquireFileLock(Path path, LockType lockType) throws ZookeeperException {
        try {
            String zkPath = makeZkPath(path);
            createIfMissing(zkPath);
            if (lockType == READ) {
                // Allow concurrent READ locks. There can be multiple READ locks on a file but only as
                // long as there are no WRITE locks.
                acquireLock(zkPath, READ, READ);
            } else {
                // WRITE locks are 100% exclusive with everything else.
                acquireLock(zkPath, WRITE);
            }
        } catch (ZookeeperException.NodeLockedException e) {
            throw new ZookeeperException.FileLockedException(e);
        }
    }

    /**
     * Release lock from given file.
     * @param path to the file whose lock should be removed
     * @param lockType type of lock, either READ or WRITE
     * @throws ZookeeperException general Zookeeper failure
     */
    @Override
    public void releaseFileLock(Path path, LockType lockType) throws ZookeeperException {
        String zkPath = makeZkPath(path);
        releaseLock(zkPath, lockType);
    }


    // Watcher stuff

    /**
     * Initialize the state without firing any events and establish the watches
     * @throws ZookeeperException
     */
    protected void setupWatches() throws ZookeeperException {
        updateChunkServers(false);
        updateMasterServer(false);
    }

    /**
     * Process a watched event coming from Zookeeper
     * @param event watched event that happened in Zookeeper
     */
    public synchronized void process(WatchedEvent event) {
        String path = event.getPath();
        if (event.getType() == Watcher.Event.EventType.None) {
            // We are are being told that the state of the
            // connection has changed
            switch (event.getState()) {
                case Disconnected:
                case Expired:
                    // It's all over.
                    connectionLostListeners.forEach(IConnectionLostListener::onConnectionLost);
                    break;
            }
        } else {
            try {
                if (path != null) {
                    if (path.equals(Znode.MASTER.toString())) {
                        updateMasterServer(true);
                    } else if (path.equals(Znode.CHUNKSERVERS.toString())) {
                        updateChunkServers(true);
                    } else {
                        logger.info("Zookeeper event dispatched for unknown node " + path);
                    }
                }
            } catch (ZookeeperException e) {
                logger.error("Failed while handling Zookeeper event", e);
            }
        }
    }

    /**
     * Synchronize with Zookeeper the list of active chunk servers.
     * @param fireTriggers whether listeners should be notified of possible changes
     * @throws ZookeeperException
     */
    protected void updateChunkServers(boolean fireTriggers) throws ZookeeperException {
        // Get list of current chunk servers and reset the watch.
        List<Machine> updated = getChildren(Znode.CHUNKSERVERS.toString(), true).stream()
            .map(Machine::fromString)
            .collect(Collectors.toList());

        // We have to update the local variable first so that all listeners have that value
        // available.
        List<Machine> old = chunkServers;
        chunkServers = updated;

        if (fireTriggers) {
            // Detect chunk servers that have been removed (are down)
            for (Machine machine : subtract(old, updated)) {
                logger.info("Chunk server " + machine + " is down");
                for (IChunkServerDownListener listener : chunkServerDownListeners) {
                    listener.onChunkServerDown(machine);
                }
            }

            // Detect chunk server that have been added (are up)
            for (Machine machine : subtract(updated, old)) {
                logger.info("Chunk server " + machine + " is up");
                for (IChunkServerUpListener listener : chunkServerUpListeners) {
                    listener.onChunkServerUp(machine);
                }
            }
        }
    }

    /**
     * Synchronize with Zookeeper currently registered master server.
     * @param fireTriggers whether listeners should be notified of possible changes
     * @throws ZookeeperException
     */
    protected void updateMasterServer(boolean fireTriggers) throws ZookeeperException {
        // Get the current master server and reset the watch
        List<String> children = getChildren(Znode.MASTER.toString(), true)
            .stream().sorted().collect(Collectors.toList());
        Machine newMaster = null;

        // The child might have disappeared in between, so let's find to lowest child that
        // can give as the new master
        for (String child : children) {
            try {
                newMaster = Machine.fromBytes(getData(Znode.MASTER.toString() + "/" + child, false));
                break;
            } catch (ZookeeperException.NodeNotFoundException e) { }
        }

        // We have to update the local variable first so that all listeners have that value
        // available.
        Machine oldMaster = masterServer;
        masterServer = newMaster;

        if (fireTriggers) {
            // Detect if a new master server was registered (is up)
            if (newMaster != null) {
                if (!newMaster.equals(oldMaster)) {
                    logger.info("Master server " + newMaster + " is up");
                    for (IMasterServerUpListener listener : masterServerUpListeners) {
                        listener.onMasterServerUp(newMaster);
                    }
                }
            // Detect if the old master server was removed (is down)
            } else if (oldMaster != null) {
                logger.info("Master server " + oldMaster + " is down");
                masterServerDownListeners.forEach(IMasterServerDownListener::onMasterServerDown);
            }
        }
    }


    // Internal helping methods

    /** Create base ZooKeeper file structure */
    protected void setupStructure() throws ZookeeperException {
        createIfMissing(Znode.MASTER.toString());
        createIfMissing(Znode.CHUNKSERVERS.toString());
        createIfMissing(Znode.FS.toString());
    }

    protected void createIfMissing(String path) throws ZookeeperException {
        createIfMissing(path, new byte[0]);
    }

    /**
     * Create persistent znode unless it already exists.
     * @param path znode to be created
     * @param data to be stored within the znode
     * @throws ZookeeperException
     */
    protected void createIfMissing(String path, byte[] data) throws ZookeeperException {
        try {
            if (exists(path, false) == null) {
                create(path, data, CreateMode.PERSISTENT);
            }
        } catch (ZookeeperException.NodeExistsException e) {
            // The point is to create the nodes so if they are created in between our calls,
            // it doesn't really matter.
        }
    }

    /**
     * Create new znode.
     * @param path znode to be created
     * @param data to be stored in the znode
     * @param createMode persistent/sequential/ephemeral
     * @throws ZookeeperException.NodeExistsException
     * @throws ZookeeperException
     * @return actual path of the new znode
     */
    protected String create(String path, byte[] data, CreateMode createMode) throws ZookeeperException {
        try {
            return zk.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode);
        } catch (KeeperException|InterruptedException e) {
            throw ZookeeperException.create(e);
        }
    }

    /**
     * Check if a znode exists.
     * @param path znode to be checked
     * @param watch should initialize a watcher
     * @throws ZookeeperException
     * @return znode stat or null if it doesn't exist
     */
    protected Stat exists(String path, boolean watch) throws ZookeeperException {
        try {
            return zk.exists(path, watch);
        } catch (KeeperException|InterruptedException e) {
            throw ZookeeperException.create(e);
        }
    }

    /**
     * Delete znode.
     * @param path znode to be deleted
     * @throws ZookeeperException.NodeNotFoundException
     * @throws ZookeeperException
     */
    protected void delete(String path) throws ZookeeperException {
        try {
            zk.delete(path, -1);
        } catch (KeeperException|InterruptedException e) {
            throw ZookeeperException.create(e);
        }
    }

    /**
     * @throws ZookeeperException.NodeLockedException
     * @throws ZookeeperException
     */
    protected String acquireLock(String znode, LockType lock) throws ZookeeperException {
        return acquireLock(znode, lock, null, new byte[0]);
    }

    /**
     * @throws ZookeeperException.NodeLockedException
     * @throws ZookeeperException
     */
    protected String acquireLock(String znode, LockType lock, LockType ignore) throws ZookeeperException {
        return acquireLock(znode, lock, ignore, new byte[0]);
    }

    /**
     * Acquire new lock. The base idea is that we create a new SEQUENTIAL EPHEMERAL child node of
     * the node that we want to lock (the lock type is used as name) and then we check whether
     * the node that we created is "first" (i. e. its counter value is the lowest).
     * We might optionally specify type of locks to be ignored (i. e. we allow multiple READ locks).
     *
     * The obtained lock is remembered and is used later when releasing the lock.
     *
     * @param znode parent znode that we want to lock
     * @param lock type of lock
     * @param ignore type of lock to be ignored in the decision process, i. e. if there is a lock
     *               of this type with lower counter value, we still consider the lock as
     *               successfully acquired.
     * @param data to be stored in the lock node
     * @throws ZookeeperException.NodeLockedException
     * @throws ZookeeperException
     */
    protected String acquireLock(String znode, LockType lock, LockType ignore, byte[] data) throws ZookeeperException {
        // It might happen that we already hold a lock for this node
        String path = znode + "/" + lock;
        if (currentLocks.get(path) != null) {
            return currentLocks.get(path);
        }

        // Write a new sequential node and immediately afterwards get the list of current children.
        // (Remove ignored children if required. I. e. when acquiring new READ lock, we ignore
        // other READ locks as multiple clients can read the same file.
        String actualPath = create(path, data, CreateMode.EPHEMERAL_SEQUENTIAL);
        List<String> locks = getChildren(znode, false).stream()
            .filter(name -> !isIgnored(name, ignore))
            .sorted()
            .collect(Collectors.toList());

        // If there is a node present with lower counter value than the one that was assigned to us,
        // that means that the node is locked by somebody else and we fail. Otherwise we managed
        // to lock the file
        if (locks.size() > 0 && getCounter(actualPath) > getCounter(locks.get(0))) {
            delete(actualPath);
            throw new ZookeeperException.NodeLockedException(path);
        } else {
            // Remember the lock.
            currentLocks.put(path, actualPath);
            return actualPath;
        }
    }

    /**
     * Release lock from the node. If there is no lock currently held at this node, we silently
     * finish.
     * @param znode that we want to "unlock"
     * @param lock type of lock that we want to remove.
     * @throws ZookeeperException
     */
    protected void releaseLock(String znode, LockType lock) throws ZookeeperException {
        try {
            String path = znode + "/" + lock;
            String actualPath = currentLocks.get(path);
            if (actualPath != null) {
                delete(actualPath);
                currentLocks.remove(path);
            }
        } catch (ZookeeperException.NodeNotFoundException e) {
            // The point is to release the the lock. So if there is no node (it has been for
            // some reason already removed) it doesn't really matter.
        }
    }

    /**
     * @throws ZookeeperException.NodeNotFoundException
     * @throws ZookeeperException
     */
    protected List<String> getChildren(String path, boolean watch) throws ZookeeperException {
        try {
            return zk.getChildren(path, watch);
        } catch (KeeperException|InterruptedException e) {
            throw ZookeeperException.create(e);
        }
    }

    /**
     * Get actual content of a znode.
     * @param path of a znode
     * @param watch should we start watching
     * @return content of znode
     * @throws ZookeeperException
     */
    protected byte[] getData(String path, boolean watch) throws ZookeeperException {
        try {
            return zk.getData(path, watch, null);
        } catch (KeeperException|InterruptedException e) {
            throw ZookeeperException.create(e);
        }
    }

    // Helper methods

    /**
     * Convert tjfs file path into Zookeeper equivalent.
     * @param path tjfs path
     * @return Zookeeper path
     */
    protected static String makeZkPath(Path path) {
        return Znode.FS + "/" + path.toString().hashCode();
    }

    /**
     * Return if given lock node should be ignored.
     * @param name name of the node that corresponds to an existing lock
     * @param ignore type of lock that should be ignored
     * @return whether this lock should be ignored
     */
    protected static  boolean isIgnored(String name, LockType ignore) {
        return ignore != null && name.matches("^" + ignore + ".+$");
    }

    /**
     * Extract the counter value from a SEQUENTIAL node name.
     * @param node name of the node
     * @return counter value or zero if the format is incorrect.
     */
    protected static int getCounter(String node) {
        try {
            if (node.length() < 10) {
                return 0;
            }
            return Integer.parseInt(node.substring(node.length() - 10));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Subtract list b from the list a. (1, 2, 3) - (2, 3) = (1)
     * @param a original list
     * @param b list that we want to subtract from a
     * @return result of the subtraction
     */
    protected static <E> List<E> subtract(List<E> a, List<E> b) {
        List<E> copy = new LinkedList<>(a);
        copy.removeAll(b);
        return copy;
    }


    // Static initialization

    /**
     * Initialize connection to Zookeeper
     * @param machine computer running Zookeeper
     * @param sessionTimeout session timeout in milliseconds
     * @return initialized instance of Zookeeper client
     * @throws ZookeeperException
     */
    public static ZookeeperClient connect(Machine machine, int sessionTimeout) throws ZookeeperException {
        try {
            // Disable excessive logging of Zookeeper
            Logger.getLogger("org.apache.zookeeper").setLevel(Level.WARN);

            DataMonitor dataMonitor = new DataMonitor();
            ZooKeeper zk = new ZooKeeper(machine.toString(), sessionTimeout, dataMonitor);
            ZookeeperClient zkClient = new ZookeeperClient(zk, dataMonitor);
            dataMonitor.setZkClient(zkClient);
            zkClient.start();
            return zkClient;
        } catch (IOException e) {
            throw new ZookeeperException.ConnectionLostException(e);
        }
    }
}
