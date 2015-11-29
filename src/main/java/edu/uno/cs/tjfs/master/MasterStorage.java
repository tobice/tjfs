package edu.uno.cs.tjfs.master;

import com.google.gson.Gson;
import edu.uno.cs.tjfs.common.*;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import sun.rmi.runtime.Log;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class MasterStorage implements IMasterStorage {
    final static Logger logger = BaseLogger.getLogger(MasterStorage.class);

    protected final ILocalFsClient localFsClient;
    protected final IMasterClient masterClient;

    protected SnapshotStorage snapshotStorage;

    /** Folder were the actual physical files are stored */
    protected final Path storageFolder;

    /** How often should the shadow replicate the master */
    protected final int replicationIntervalTime;

    /** How often should the current state be snapshotted */
    protected final int snapshottingIntervalTime;

    /** In-memory copy of the filesystem */
    protected Map<Path, FileDescriptor> fileSystem = new HashMap<>();

    /** Current version of the filesystem. Increases with every change. Zero means empty fs */
    protected int version = 0;

    /** Thread holding the replication worker */
    protected Thread replicationThread;

    /** Thread holding the snapshotting worker */
    protected Thread snapshottingThread;

    public MasterStorage(Path storageFolder, ILocalFsClient localFsClient, IMasterClient masterClient,
                         int replicationIntervalTime, int snapshottingIntervalTime) {
        this.localFsClient = localFsClient;
        this.storageFolder = storageFolder;
        this.masterClient = masterClient;
        this.replicationIntervalTime = replicationIntervalTime;
        this.snapshottingIntervalTime = snapshottingIntervalTime;

        // Okay, this should be injected from the outside, but right now, ain't nobody got time
        // for that.
        this.snapshotStorage = new SnapshotStorage(localFsClient, getSnapshotsFolder());
    }

    /**
     * Initialize the filesystem by creating necessary folder structure and loading the
     * latest filesystem state into memory.
     * @throws TjfsException
     */
    @Override
    public void init() throws TjfsException {
        try {
            if (!localFsClient.exists(getLogFolder())) {
                localFsClient.mkdir(getLogFolder());
            }
            if (!localFsClient.exists(getSnapshotsFolder())) {
                localFsClient.mkdir(getSnapshotsFolder());
            }

            restoreFileSystem();
        } catch (IOException e) {
            throw new TjfsException("Unable to initialize master storage. " + e.getMessage(), e);
        }

    }

    /** Restore filesystem from the disk into memory */
    protected void restoreFileSystem() throws TjfsException {
        // Load latest snapshot into memory
        Snapshot latest = snapshotStorage.getLatest();
        if (latest != null) {
            latest.files.forEach(file -> fileSystem.put(file.path, file));
            version = latest.version;
        }

        // Load log into memory
        for (LogItem item : getLog(0)) {
            if (item.file.isEmpty()) {
                fileSystem.remove(item.file.path);
            } else {
                fileSystem.put(item.file.path,item.file);
            }
            version = Math.max(version, item.version);
        }
    }

    /** Start replicating the state from master */
    public void startReplication() {
        replicationThread = new Thread(() -> {
            try {
                // Wait first so that the actual master server boots up and registers.
                Thread.sleep(replicationIntervalTime);

                // Then try to get and load a snapshot from the other master.
                Snapshot snapshot = masterClient.getLatestSnapshot();
                if (snapshot != null) {
                   loadSnapshot(snapshot);
                }

                // Even though we are a shadow, we still need to make a snapshot once in a while.
                startSnapshotting();

                // And finally start incremental synchronization with the other master.
                while (true) {
                    try {
                        updateLog(masterClient.getLog(this.version));
                        Thread.sleep(replicationIntervalTime);
                    } catch (TjfsException e) {
                        logger.error("Replication failed. " + e.getMessage(), e);
                    }
                }
            } catch (TjfsException e) {
                logger.error("Unable the to start replication as fetching snapshot failed.", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // Do nothing, we stopped
            }
        });
        replicationThread.start();
    }

    /** Stop replicating the state from master */
    public void stopReplication() {
        try {
            if (replicationThread != null) {
                replicationThread.interrupt(); // interrupt it
                replicationThread.join(); // wait until it finishes
                replicationThread = null; // delete it

                stopSnapshotting();
            }
        } catch (InterruptedException e) {
            // Do nothing, we're probably crashing.
        }
    }

    /**
     * Get file from the filesystem.
     * @param path of the file
     * @return file descriptor or null if not found
     */
    @Override
    public FileDescriptor getFile(Path path) throws TjfsException {
        if (list(path).length > 0) {
            throw new TjfsException("Cannot get a directory");
        }
        return fileSystem.get(path);
    }

    /**
     * Put file to the filesystem. If file already exists, it's replaced with the newer version.
     * @param file updated file descriptor
     * @throws TjfsException
     */
    @Override
    public synchronized void putFile(FileDescriptor file) throws TjfsException {
        if (list(file.path).length > 0) {
            throw new TjfsException("This file is a directory.");
        }

        Gson gson = CustomGson.create();
        try {
            version++;
            localFsClient.writeBytesToFile(
                getLogItemPath(version),
                gson.toJson(file.withoutChunkServers()).getBytes());
            if (file.isEmpty()) {
                fileSystem.remove(file.path);
            } else {
                fileSystem.put(file.path, file);
            }
        } catch (Exception e) {
            logger.error("Error while putting file to master.", e);
            throw new TjfsException("Error when storing the file. " + e.getMessage(), e);
        }
    }

    /**
     * Return items from current log.
     * @param lastVersion last version that the client has. Only newer items will be returned.
     * @return log items newer than lastVersion sorted by version
     */
    public List<LogItem> getLog(int lastVersion) {
        Gson gson = CustomGson.create();
        return Arrays.asList(localFsClient.list(getLogFolder())).stream()
            .map(Integer::parseInt)
            .filter(i -> i > lastVersion)
            .sorted()
            .map(i -> {
                try {
                    byte[] content = localFsClient.readBytesFromFile(getLogItemPath(i));
                    FileDescriptor file = gson.fromJson(IOUtils.toString(content, "UTF-8"), FileDescriptor.class);
                    return new LogItem(i, file);
                } catch (IOException e) {
                    logger.error("Unable to load log item " + i, e);
                    return null;
                }
            })
            .filter(item -> item != null)
            .collect(Collectors.toList());
    }

    /**
     * Returns contents of a folder ("all files with given prefix).
     * @param path folder path
     * @return list of file names and folder names within the folder
     */
    public String[] list(Path path) {
        // Get normalized prefix
        final String prefix = path.toString() + (path.toString().endsWith("/") ? "" : "/");

        Set<String> content = fileSystem.keySet().stream()
            .filter(p -> p.toString().startsWith(prefix))
            .map(Path::toString)
            .map(s -> {
                s = s.replaceFirst("^" + prefix, "");
                // Detect subfolders
                return !s.contains("/") ? s : s.replaceAll("/.*$", "/");
            })
            .collect(Collectors.toSet());

        return content.toArray(new String[content.size()]);
    }

    /**
     * Add incoming items to local log. Maintains the current version
     * @param log list of items to be added.
     * @throws TjfsException if we fail to write the log.
     */
    protected synchronized void updateLog(List<LogItem> log) throws TjfsException {
        try {
            Gson gson = CustomGson.create();
            for (LogItem item : log) {
                if (item.version <= version) {
                    throw new TjfsException("Incoming log is older than local data!");
                }
                localFsClient.writeBytesToFile(
                    getLogItemPath(item.version),
                    gson.toJson(item.file.withoutChunkServers()).getBytes());
                if (item.file.isEmpty()) {
                    fileSystem.remove(item.file.path);
                } else {
                    fileSystem.put(item.file.path,item.file);
                }
                version = Math.max(version, item.version);
            }
        } catch (IOException e) {
            throw new TjfsException("Unable to write a log item file. " + e.getMessage(), e);
        }
    }

    protected Path getLogFolder() {
        return storageFolder.resolve("log");
    }

    protected Path getSnapshotsFolder() {
        return storageFolder.resolve("snapshots");
    }

    protected Path getLogItemPath(int version) {
        return getLogFolder().resolve("" + version);
    }

    // Snapshotting stuff

    /** Start making snapshots of the local state */
    public void startSnapshotting() {
        snapshottingThread = new Thread(() -> {
            try {
                while (true) {
                    try {
                        makeSnapshot();
                        Thread.sleep(snapshottingIntervalTime);
                    } catch (TjfsException e) {
                        logger.error("Failed making a snapshot. " + e.getMessage(), e);
                    }
                }
            } catch (InterruptedException e ) {
                Thread.currentThread().interrupt();
                // Do nothing, we stopped
            }
        });
        snapshottingThread.start();
    }

    /** Stop making snapshots of the local state */
    public void stopSnapshotting() {
        try {
            if (snapshottingThread!= null) {
                snapshottingThread.interrupt();
                snapshottingThread.join();
                snapshottingThread = null;
            }
        } catch (InterruptedException e) {
            // Do nothing, we're probably crashing.
        }
    }


    /**
     * Get latest snapshot.
     * @return latest snapshot or null if not available.
     * @throws TjfsException
     */
    public Snapshot getLastSnapshot() throws TjfsException {
        return snapshotStorage.getLatest();
    }

    /**
     * Loads remote snapshot to the local filesystem.
     * @param snapshot to load
     * @throws TjfsException
     */
    protected void loadSnapshot(Snapshot snapshot) throws TjfsException {
        clearFilesystem();
        snapshotStorage.store(snapshot);
        snapshot.files.forEach(file -> fileSystem.put(file.path, file));
        version = snapshot.version;
    }

    /** VERY DANGEROUS. Utterly resets and cleans the local filesystem */
    protected void clearFilesystem() throws TjfsException {
        try {
            fileSystem = new HashMap<>();
            version = 0;
            for (String item : localFsClient.list(getLogFolder())) {
                localFsClient.deleteFile(getLogFolder().resolve(item));
            }
            for (String item : localFsClient.list(getSnapshotsFolder())) {
                localFsClient.deleteFile(getSnapshotsFolder().resolve(item));
            }
        } catch (IOException e) {
            throw new TjfsException("Failed to clear to filesystem. " + e.getMessage(), e);
        }
    }

    /**
     * Create new snapshot of the filesystem.
     * @throws TjfsException
     */
    protected synchronized void makeSnapshot() throws TjfsException {
        // Let's fix the version at this moment. The resulting snapshot will contain
        // everything up to this moment.
        int snapshotVersion = version;

        // Here we will store the actual fs content
        Map<Path, FileDescriptor> snapshotContent = new HashMap<>();

        // If there is a previous snapshot, let's use it as a base for the next snapshot
        Snapshot lastSnapshot = getLastSnapshot();
        List<LogItem> log;
        if (lastSnapshot != null) {
            lastSnapshot.files.forEach(file -> snapshotContent.put(file.path, file));
            log = getLog(lastSnapshot.version);
        } else {
            log = getLog(0);
        }

        // Update the snapshot content using the operations in the log. We ignore any newer
        // items that might have appeared in between.
        log.stream()
            .filter(item -> item.version <= snapshotVersion)
            .map(item -> item.file)
            .forEach(file -> {
                if (file.isEmpty()) {
                    snapshotContent.remove(file.path);
                } else {
                    snapshotContent.put(file.path, file);
                }
            });

        // Store the snapshot to the disk
        List<FileDescriptor> files = snapshotContent.values().stream().collect(Collectors.toList());
        Snapshot snapshot = new Snapshot(snapshotVersion, files);
        snapshotStorage.store(snapshot);
    }
}
