package edu.uno.cs.tjfs.master;

import edu.uno.cs.tjfs.common.ChunkDescriptor;
import edu.uno.cs.tjfs.common.FileDescriptor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface IMasterStorage {
    FileDescriptor getFile(Path path);
    void putFile(Path path, FileDescriptor file) throws IOException;
    List<FileDescriptor> getLog(int startingLogID);
    void allocateChunks(List<ChunkDescriptor> chunks);
    void init();
}
