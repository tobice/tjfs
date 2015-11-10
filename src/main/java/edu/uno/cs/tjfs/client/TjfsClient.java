package edu.uno.cs.tjfs.client;

import edu.uno.cs.tjfs.Config;
import edu.uno.cs.tjfs.chunkserver.IChunkClient;
import edu.uno.cs.tjfs.common.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Date;

public class TjfsClient implements ITjfsClient {
    private Config config;
    private IMasterClient masterClient;
    private IChunkClient chunkClient;

    public TjfsClient(Config config) {
        this.config = config;
        this.masterClient = masterClient;
        this.chunkClient = chunkClient;
    }

    @Override
    public InputStream get(Path path) throws TjfsClientException {
        return null;
    }

    @Override
    public InputStream get(Path path, int byteOffset) throws TjfsClientException {
        return null;
    }

    @Override
    public InputStream get(Path path, int byteOffset, int numberOfBytes) throws TjfsClientException {
        return null;
    }

    @Override
    public void put(Path path, InputStream data) throws TjfsClientException {
        put(path, data, 0);
    }

    @Override
    public void put(Path path, InputStream data, int byteOffset) throws TjfsClientException {
        try {
            // TODO: Lock the file

            // Get file descriptor. Might by empty, doesn't matter.
            FileDescriptor file = masterClient.getFile(path);

            // Init tools that we're going to need
            ChunkNameAllocator allocator = new ChunkNameAllocator(masterClient, 10);
            // TODO: init job generator


            // TODO: use job generator...


            // TODO: Unlock the file
        } catch (TjfsException e) {
            String message = "File transfer failed. Reason: " + e.getMessage();
            throw new TjfsClientException(message, e);
        }
    }

    @Override
    public void delete(Path path) throws TjfsClientException {

    }

    @Override
    public int getSize(Path path) throws TjfsClientException {
        return 0;
    }

    @Override
    public Date getTime(Path path) throws TjfsClientException {
        return null;
    }

    @Override
    public String[] list(Path path) throws TjfsClientException {
        return new String[0];
    }

    @Override
    public void move(Path sourcePath, Path destinationPath) throws TjfsClientException {

    }
}
