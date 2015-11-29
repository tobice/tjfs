package edu.uno.cs.tjfs.master;

import edu.uno.cs.tjfs.common.FileDescriptor;
import edu.uno.cs.tjfs.common.TjfsException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface IMasterStorage {
    class LogItem {
        public final int version;
        public final FileDescriptor file;

        public LogItem(int version, FileDescriptor file) {
            this.version = version;
            this.file = file;
        }
    }

    class Snapshot {
        public final int version;
        public final List<FileDescriptor> files;

        public Snapshot(int version, List<FileDescriptor> files) {
            this.version = version;
            this.files = files;
        }
    }

    FileDescriptor getFile(Path path) throws TjfsException;
    void putFile(FileDescriptor file) throws TjfsException;
    List<LogItem> getLog(int startingLogID);
    void init() throws IOException;
}
