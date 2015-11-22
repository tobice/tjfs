package edu.uno.cs.tjfs.common;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Dummy local fs client that simulates local file system using in-memory storage (hash map).
 * It's meant just for testing purposes.
 */
public class DummyLocalFsClient implements ILocalFsClient {

    Map<Path, byte[]> storage;

    public DummyLocalFsClient() {

        storage = new HashMap<>();
    }
    public InputStream readFile(Path path) throws IOException {
        if (!storage.containsKey(path)) {
            throw new IOException("File not found");
        }
        return new ByteArrayInputStream(storage.get(path));
    }

    public void writeFile(Path path, InputStream data) throws IOException {
        storage.put(path, IOUtils.toByteArray(data));
    }

    @Override
    public byte[] readBytesFromFile(Path path) throws IOException {
        return new byte[0];
    }

    @Override
    public void writeBytesToFile(Path path, byte[] data) throws IOException {

    }

    @Override
    public String[] listFiles(Path path) {
        return new String[0];
    }

    @Override
    public void deleteFile(Path path) throws IOException {
        //TODO: If required
    }
}
