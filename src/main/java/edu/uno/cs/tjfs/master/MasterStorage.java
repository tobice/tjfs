package edu.uno.cs.tjfs.master;

import com.google.gson.Gson;
import edu.uno.cs.tjfs.Config;
import edu.uno.cs.tjfs.common.*;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

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
    private int replicationIntervalTime;
    final static Logger logger = BaseLogger.getLogger(MasterStorage.class);

    public MasterStorage(Path path, ILocalFsClient localFsClient, IMasterClient masterClient, int replicationIntervalTime){
        this.localFsClient = localFsClient;
        this.fileSystemPath = path;
        this.masterClient = masterClient;
        this.replicationIntervalTime = replicationIntervalTime;
    }

    public void startReplication() {
        replicationThread = new Thread(() -> {
            try {
                while (true) {
                    // Wait first so that the actual master server boots up and registers.
                    Thread.sleep(replicationIntervalTime);
                    try {
                        List<FileDescriptor> logFiles = this.masterClient.getLog(this.logFileCount);
                        updateLog(logFiles);
                    } catch (Exception e) {
                        logger.error("MasterStorage.startReplication - Error while getting the logs from the master");
                        logger.error("MasterStorage.startReplication - ", e);
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

    public synchronized void updateLog(List<FileDescriptor> logs) throws TjfsException {
        for (FileDescriptor log : logs){
            putFile(log.path, log);
        }
    }

    @Override
    public void deleteFile(Path path) throws TjfsException {
        if (getFile(path) == null)
            throw new TjfsException("File not found");
        this.fileSystem.remove(path); //TODO: how would remove file be replicated?
    }

    @Override
    public FileDescriptor getFile(Path path){
        return this.fileSystem.get(path);
    }

    @Override
    public synchronized void putFile(Path path, FileDescriptor file) throws TjfsException{
        Gson gson = CustomGson.create();
        try {
            this.localFsClient.writeBytesToFile(
                    getFilePath(logFileCount++),
                    gson.toJson(file).getBytes());
            this.fileSystem.put(path, file);
        } catch (Exception e) {
            logger.error("Error while putting file in master.");
            logger.error("MasterStorage.putFile", e);
            throw new TjfsException("Error while putting file in master", e);
        }
    }

    @Override
    public List<FileDescriptor> getLog(int startingLogID) {
        ArrayList<FileDescriptor> result = new ArrayList<>();
        Gson gson = CustomGson.create();
        try {
            ArrayList<Integer> filesIntValues = new ArrayList<>();
            String[] files = this.localFsClient.listFiles(this.fileSystemPath);
            if (files == null) return null;
            for (String fileName : files) filesIntValues.add(Integer.valueOf(fileName));
            Collections.sort(filesIntValues);
            for (int fileNameInt : filesIntValues) {
                if (fileNameInt >= startingLogID) {
                    byte[] fileContent = this.localFsClient.readBytesFromFile(getFilePath(fileNameInt));
                    result.add(gson.fromJson(IOUtils.toString(fileContent, "UTF-8"), FileDescriptor.class));
                }
            }
        }catch(IOException e){
            logger.error("MasterStorage.getLog - Error while getting the log.");
            logger.error("MaterStorage.getLog - ", e);
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
                this.logFileCount++;
            }
    }

    private Path getFilePath(int fileName){
        return Paths.get(this.fileSystemPath.toString() + "/" + fileName);
    }

    public Map<Path, FileDescriptor> getFileSystem(){
        return fileSystem;
    }
}
