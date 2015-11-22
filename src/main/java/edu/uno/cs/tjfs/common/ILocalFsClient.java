package edu.uno.cs.tjfs.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface ILocalFsClient {
    InputStream readFile(Path path) throws IOException;
    void writeFile(Path path, InputStream data) throws IOException;
    byte[] readBytesFromFile(Path path) throws IOException;
    void writeBytesToFile(Path path, byte[] data) throws IOException;
    String[] listFiles(Path path);

    void deleteFile(Path path) throws IOException;
}
