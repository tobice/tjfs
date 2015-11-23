package edu.uno.cs.tjfs.master;

import com.google.gson.Gson;
import edu.uno.cs.tjfs.Config;
import edu.uno.cs.tjfs.common.*;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MasterStorage implements IMasterStorage{
    private Map<Path, FileDescriptor> fileSystem;
    private Path fileSystemPath;
    private ILocalFsClient localFsClient;
    private IMasterClient masterClient;
    private Thread replicationThread;
    private int logFileCount;
    private ChunkServerService chunkServerService;

    public MasterStorage(Path path, ILocalFsClient localFsClient, ChunkServerService service){
        this.localFsClient = localFsClient;
        this.fileSystemPath = path;
        this.chunkServerService = service;
    }

    @Override
    public FileDescriptor getFile(Path path){
        FileDescriptor fileDescriptor = this.fileSystem.get(path);
        return fileDescriptor == null ? new FileDescriptor(path) : chunkServerService.updateChunkServers(this.fileSystem.get(path));
    }

    public synchronized void putFile(Path path, FileDescriptor file) throws IOException{
        Gson gson = new Gson();
        try {
            this.localFsClient.writeBytesToFile(
                    Paths.get(this.fileSystemPath.toString() + "/" + (this.logFileCount++)),
                    gson.toJson(file).getBytes());
            this.fileSystem.put(path, file);
        } catch (Exception e) {
            BaseLogger.error("Error while putting file in master.");
            BaseLogger.error("MasterStorage.putFile", e);
            throw e;
        }
    }

    public void startReplication() {
        Config config = new Config();
        int intervalTime = config.getMasterReplicationIntervalTime();
        replicationThread = new Thread(() -> {
            try {
                while (true) {
                    // Wait first so that the actual master server boots up and registers.
                    Thread.sleep(intervalTime);
                    try {
                        List<FileDescriptor> logFiles = this.masterClient.getLog(this.logFileCount);
                        updateLog(logFiles);
                    } catch (Exception e) {
                        BaseLogger.error("MasterStorage.startReplication - Error while getting the logs from the master");
                        BaseLogger.error("MasterStorage.startReplication - ", e);
                    }

                }
            } catch (InterruptedException e) {
                // Do nothing, we stopped
            }
        });
        replicationThread.start();
    }

    public void stopReplication()  {
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

    private synchronized void updateLog(List<FileDescriptor> logs) throws IOException {
        for (FileDescriptor log : logs){
            putFile(log.path, log);
        }
    }

    @Override
    public List<FileDescriptor> getLog(int startingLogID) {
        ArrayList<FileDescriptor> result = new ArrayList<>();
        Gson gson = new Gson();
        try {
            ArrayList<Integer> filesIntValues = new ArrayList<>();
            String[] files = this.localFsClient.listFiles(this.fileSystemPath);
            if (files == null) return null;
            for (String fileName : files) filesIntValues.add(Integer.valueOf(fileName));
            for (int fileNameInt : filesIntValues) {
                if (fileNameInt >= startingLogID) {
                    byte[] fileContent = this.localFsClient.readBytesFromFile(Paths.get(fileNameInt+""));
                    result.add(gson.fromJson(IOUtils.toString(fileContent, "UTF-8"), FileDescriptor.class));
                }
            }
        }catch(IOException e){
            BaseLogger.error("MasterStorage.getLog - Error while getting the log.");
            BaseLogger.error("MaterStorage.getLog - ", e);
        }
        return result;
    }

    @Override
    public void init() {
        this.fileSystem = new HashMap<>();
        List<FileDescriptor> allLogItems = getLog(0);
        if (allLogItems != null)
            for(FileDescriptor logItem : allLogItems){
                this.fileSystem.put(logItem.path, logItem);
            }
    }
}
