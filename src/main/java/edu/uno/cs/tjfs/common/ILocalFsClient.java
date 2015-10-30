package edu.uno.cs.tjfs.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public interface ILocalFsClient {
    InputStream readFile(Path path) throws IOException;
    void writeFile(Path path, InputStream data) throws IOException;
}
