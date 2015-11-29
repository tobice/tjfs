package edu.uno.cs.tjfs.master;

import com.google.gson.Gson;
import edu.uno.cs.tjfs.common.*;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class MasterStorage implements IMasterStorage {

    final static Logger logger = BaseLogger.getLogger(MasterStorage.class);

    protected final ILocalFsClient localFsClient;
    protected final IMasterClient masterClient;

    /** Folder were the actual physical files are stored */
    protected final Path storageFolder;

    /** How often should the shadow replicate the master */
    protected final int replicationIntervalTime;

    /** In-memory copy of the filesystem */
    protected Map<Path, FileDescriptor> fileSystem = new HashMap<>();

    /** Current version of the filesystem. Increases with every change. Zero means empty fs */
    protected int version = 0;

    /** Thread holding the replication worker */
    protected Thread replicationThread;

    public MasterStorage(Path storageFolder, ILocalFsClient localFsClient, IMasterClient masterClient, int replicationIntervalTime) {
        this.localFsClient = localFsClient;
        this.storageFolder = storageFolder;
        this.masterClient = masterClient;
        this.replicationIntervalTime = replicationIntervalTime;
    }

    /**
     * Initialize the filesystem by creating necessary folder structure and loading the
     * latest filesystem state into memory.
     * @throws IOException
     */
    @Override
    public void init() throws IOException {
        if (!localFsClient.exists(getLogFolder())) {
            localFsClient.mkdir(getLogFolder());
        }
        if (!localFsClient.exists(getSnapshotsFolder())) {
            localFsClient.mkdir(getSnapshotsFolder());
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
                while (true) {
                    try {
                        // Wait first so that the actual master server boots up and registers.
                        Thread.sleep(replicationIntervalTime);
                        updateLog(masterClient.getLog(this.version));
                    } catch (TjfsException e) {
                        logger.error("Replication failed. " + e.getMessage(), e);
                    }
                }
            } catch (InterruptedException e) {
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

    protected int getLastSnapshot() {
        return 0;
    }
}
