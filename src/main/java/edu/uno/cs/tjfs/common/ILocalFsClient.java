package edu.uno.cs.tjfs.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

public interface ILocalFsClient {
    InputStream readFile(Path path) throws IOException;
    void writeFile(Path path, InputStream data) throws IOException;
    byte[] readBytesFromFile(Path path) throws IOException;
    void writeBytesToFile(Path path, byte[] data) throws IOException;
    void deleteFile(Path path) throws IOException;

    String[] list(Path path);
    void mkdir(Path path) throws IOException;
    boolean exists(Path path);
}
