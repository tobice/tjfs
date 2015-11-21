package edu.uno.cs.tjfs.master;

import com.google.gson.Gson;
import edu.uno.cs.tjfs.common.*;
import org.apache.commons.io.IOUtils;
import org.xadisk.bridge.proxies.interfaces.*;
import org.xadisk.filesystem.exceptions.XAApplicationException;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MasterStorage implements IMasterStorage{
    private Map<Path, FileDescriptor> fileSystem;
    private Map<String, ChunkDescriptor> chunks;
    private Path fileSystemPath;
    private ILocalFsClient localFsClient;
    private IMasterClient masterClient;
    private Thread replicationThread;

    public MasterStorage(Path path, ILocalFsClient localFsClient){
        this.localFsClient = localFsClient;
        this.fileSystemPath = path;
    }

    @Override
    public FileDescriptor getFile(Path path){
        FileDescriptor descriptor = this.fileSystem.get(path);
        ArrayList<ChunkDescriptor> updatedChunks = new ArrayList<>();
        if (descriptor != null && descriptor.chunks != null)
            for(ChunkDescriptor chunkDescriptor : descriptor.chunks){
                updatedChunks.add(this.chunks.get(chunkDescriptor.name));
            }
        //descriptor.chunks = updatedChunks;
        return descriptor;
    }

    public synchronized void putFile(Path path, FileDescriptor file) throws IOException{
        Gson gson = new Gson();
        try {
            ArrayList<Integer> filesIntValues = new ArrayList<>();
            String[] files = this.localFsClient.listFiles(this.fileSystemPath);
            String newFileName = "1";
            System.out.println(files.length);
            if (files != null && files.length > 0){
                for (int counter =0; counter < files.length ;counter++)
                    filesIntValues.add(Integer.valueOf(files[counter]));
                newFileName = Collections.max(filesIntValues).toString();
            }

            System.out.println(gson.toJson(file));

//            this.localFsClient.writeBytesToFile(
//                    Paths.get(this.fileSystemPath.toString() + "/" + newFileName),
//                    gson.toJson(file).getBytes());
//            this.fileSystem.put(path, file);
        } catch (Exception e) {
            BaseLogger.error("Error while putting file in master.");
            BaseLogger.error("MasterStorage.putFile", e);
            throw e;
        }
    }

    public void startReplication() {
        replicationThread = new Thread(() -> {
            try {
                while (true) {
                    // Wait first so that the actual master server boots up and registers.
                    Thread.sleep(1000); // TODO: use a value from Config
                    // TODO: use master client to load log from the actual master
                    // TODO: ignore failures, just log them
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

//    @Override
//    public void putFile(Path path, FileDescriptor file) throws IOException{
//        String xadiskSystemDirectory = "/home/xadisk";
//        XAFileSystem xafs = null;
//        try {
//            StandaloneFileSystemConfiguration configuration =
//                    new StandaloneFileSystemConfiguration(xadiskSystemDirectory, "id-1");
//
//            xafs = XAFileSystemProxy.bootNativeXAFileSystem(configuration);
//
//            xafs.waitForBootup(-1);
//
//            Session session = xafs.createSessionForLocalTransaction();
//
//            try {
//                ArrayList<Integer> filesIntValues = new ArrayList<>();
//                String[] files = session.listFiles(new File(this.fileSystemPath.toString()), true);
//                for(String fileName : files) filesIntValues.add(Integer.valueOf(fileName));
//                String newFileName = Collections.max(filesIntValues).toString();
//                File newFile = new File(this.fileSystemPath.toString(), newFileName);
//                session.createFile(newFile, false);
//
//                XAFileOutputStream outStream = session.createXAFileOutputStream(newFile, true);
//                Gson gson = new Gson();
//                outStream.write(gson.toJson(file).getBytes());
//                outStream.flush();
//                outStream.close();
//
//                session.commit();
//
//                this.fileSystem.put(path, file);
//            } catch (XAApplicationException xaae) {
//                session.rollback();
//                throw xaae;
//            }
//
//        } catch (Throwable t) {
//            t.printStackTrace();
//        } finally {
//            if (xafs != null) {
//                try {
//                    xafs.shutdown();
//                } catch (IOException ioe) {
//                    ioe.printStackTrace();
//                }
//            }
//        }
//    }

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
    public void allocateChunks(List<ChunkDescriptor> chunks) {
        for(ChunkDescriptor chunk : chunks){
            this.chunks.put(chunk.name, chunk);
        }
    }

    @Override
    public void init() {
        this.fileSystem = new HashMap<>();
        this.chunks = new HashMap<>();
        List<FileDescriptor> allLogItems = getLog(0);
        if (allLogItems != null)
            for(FileDescriptor logItem : allLogItems){
                this.fileSystem.put(logItem.path, logItem);
                for(ChunkDescriptor chunk : logItem.chunks){
                    this.chunks.put(chunk.name, chunk);
                }
            }
    }
}
