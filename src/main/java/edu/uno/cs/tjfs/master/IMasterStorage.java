package edu.uno.cs.tjfs.master;

import edu.uno.cs.tjfs.common.ChunkDescriptor;
import edu.uno.cs.tjfs.common.FileDescriptor;
import edu.uno.cs.tjfs.common.TjfsException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface IMasterStorage {
    void deleteFile(Path path) throws TjfsException;

    FileDescriptor getFile(Path path);
    void putFile(Path path, FileDescriptor file) throws IOException, TjfsException;
    List<FileDescriptor> getLog(int startingLogID);
    void init();
}
