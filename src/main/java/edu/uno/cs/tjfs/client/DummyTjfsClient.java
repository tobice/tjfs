package edu.uno.cs.tjfs.client;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

/**
 *  Dummy tjfs client that simulates the distributed filesystem using local in-memory storage
 *  (hash map). The functionality is limited and is meant only for testing purposes.
 */
public class DummyTjfsClient implements ITjfsClient {

    Map<Path, byte[]> storage;

    public DummyTjfsClient() {
        storage = new HashMap<>();
    }

    public InputStream get(Path path) throws TjfsClientException {
        if (!storage.containsKey(path)) {
            throw new TjfsClientException("File not found");
        }
        return new ByteArrayInputStream(storage.get(path));
    }

    public InputStream get(Path path, int byteOffset) throws TjfsClientException {
        if (!storage.containsKey(path)) {
            throw new TjfsClientException("File not found");
        }
        byte[] data = storage.get(path);
        return new ByteArrayInputStream(Arrays.copyOfRange(data, byteOffset, data.length));
    }

    public InputStream get(Path path, int byteOffset, int numberOfBytes) throws TjfsClientException {
        if (!storage.containsKey(path)) {
            throw new TjfsClientException("File not found");
        }
        byte[] data = storage.get(path);
        return new ByteArrayInputStream(Arrays.copyOfRange(data, byteOffset, byteOffset + numberOfBytes));
    }

    public void put(Path path, InputStream data) throws TjfsClientException {
        try {
            storage.put(path, IOUtils.toByteArray(data));
        } catch (IOException e) {
            throw new TjfsClientException(e.getMessage());
        }
    }

    public void put(Path path, InputStream data, int byteOffset) throws TjfsClientException {
        try {
            byte[] originalData = storage.get(path);
            byte[] newData = IOUtils.toByteArray(data);

            if (originalData == null) {
                originalData = new byte[byteOffset];
            } else {
                originalData = Arrays.copyOfRange(originalData, 0, byteOffset);
            }

            storage.put(path, ArrayUtils.addAll(originalData, newData));
        } catch (IOException e) {
            throw new TjfsClientException(e.getMessage());
        }
    }

    public void delete(Path path) throws TjfsClientException {
        storage.remove(path);
    }

    public int getSize(Path path) throws TjfsClientException {
        return storage.get(path).length;
    }

    public Date getTime(Path path) throws TjfsClientException {
        return new Date();
    }

    public String[] list(Path path) throws TjfsClientException {
        String prefix = path.toString();
        List<String> result = new LinkedList<>();

        for (Path key : storage.keySet()) {
            String s = key.toString();
            if (s.startsWith(prefix)) {
                s = s.replaceFirst("^" + prefix, "");
                if (!s.matches("/")) {
                   result.add(s);
                } else {
                    result.add(s.replace("/.*$", "/"));
                }
            }
        }

        return result.toArray(new String[result.size()]);
    }

    public void move(Path sourcePath, Path destinationPath) throws TjfsClientException {
        if (!storage.containsKey(sourcePath)) {
            throw new TjfsClientException("File not found");
        }
        storage.put(destinationPath, storage.get(sourcePath));
        storage.remove(sourcePath);
    }
}
