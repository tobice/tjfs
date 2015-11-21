package edu.uno.cs.tjfs.master;

import com.google.gson.Gson;
import edu.uno.cs.tjfs.common.ChunkDescriptor;
import edu.uno.cs.tjfs.common.FileDescriptor;
import org.apache.commons.io.IOUtils;
import org.xadisk.bridge.proxies.interfaces.*;
import org.xadisk.filesystem.exceptions.XAApplicationException;
import org.xadisk.filesystem.standalone.StandaloneFileSystemConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class MasterStorage implements IMasterStorage{
    private Map<Path, FileDescriptor> fileSystem;
    private Map<String, ChunkDescriptor> chunks;
    private Path fileSystemPath;

    public MasterStorage(Path path){
        this.fileSystemPath = path;
    }

    @Override
    public FileDescriptor getFile(Path path){
        return this.fileSystem.get(path);
    }

    @Override
    public void putFile(Path path, FileDescriptor file) throws IOException{
        String xadiskSystemDirectory = "/home/xadisk";
        XAFileSystem xafs = null;
        try {
            StandaloneFileSystemConfiguration configuration =
                    new StandaloneFileSystemConfiguration(xadiskSystemDirectory, "id-1");

            xafs = XAFileSystemProxy.bootNativeXAFileSystem(configuration);

            xafs.waitForBootup(-1);

            Session session = xafs.createSessionForLocalTransaction();

            try {
                ArrayList<Integer> filesIntValues = new ArrayList<>();
                String[] files = session.listFiles(new File(this.fileSystemPath.toString()), true);
                for(String fileName : files) filesIntValues.add(Integer.valueOf(fileName));
                String newFileName = Collections.max(filesIntValues).toString();
                File newFile = new File(this.fileSystemPath.toString(), newFileName);
                session.createFile(newFile, false);

                XAFileOutputStream outStream = session.createXAFileOutputStream(newFile, true);
                Gson gson = new Gson();
                outStream.write(gson.toJson(file).getBytes());
                outStream.flush();
                outStream.close();

                session.commit();

                this.fileSystem.put(path, file);
            } catch (XAApplicationException xaae) {
                session.rollback();
                throw xaae;
            }

        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if (xafs != null) {
                try {
                    xafs.shutdown();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
    }

    @Override
    public List<FileDescriptor> getLog(int startingLogID) {
        ArrayList<FileDescriptor> result = new ArrayList<>();
        String xadiskSystemDirectory = "/home/xadisk";
        XAFileSystem xafs = null;
        Gson gson = new Gson();
        try {
            StandaloneFileSystemConfiguration configuration =
                    new StandaloneFileSystemConfiguration(xadiskSystemDirectory, "id-1");

            xafs = XAFileSystemProxy.bootNativeXAFileSystem(configuration);

            xafs.waitForBootup(-1);

            Session session = xafs.createSessionForLocalTransaction();

            try {
                ArrayList<Integer> filesIntValues = new ArrayList<>();
                String[] files = session.listFiles(new File(this.fileSystemPath.toString()), true);
                for(String fileName : files) filesIntValues.add(Integer.valueOf(fileName));
                for(int fileNameInt: filesIntValues) {
                    if (fileNameInt >= startingLogID) {
                        File fileToRead = new File(this.fileSystemPath.toString(), fileNameInt + "");
                        XAFileInputStream inStream = session.createXAFileInputStream(fileToRead, true);
                        //This should never failed because file sizes are so small
                        byte[] fileContent = new byte[(int)session.getFileLength(fileToRead)];
                        inStream.read(fileContent);
                        result.add(gson.fromJson(IOUtils.toString(fileContent, "UTF-8"), FileDescriptor.class));
                        inStream.close();
                    }
                }
                session.commit();
            } catch (XAApplicationException xaae) {
                session.rollback();
                throw xaae;
            }

        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if (xafs != null) {
                try {
                    xafs.shutdown();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
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
        List<FileDescriptor> allLogItems = getLog(0);
        for(FileDescriptor logItem : allLogItems){
            this.fileSystem.put(logItem.path, logItem);
            for(ChunkDescriptor chunk : logItem.chunks){
                this.chunks.put(chunk.name, chunk);
            }
        }
    }
}
