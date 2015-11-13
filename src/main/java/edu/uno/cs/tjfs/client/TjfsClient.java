package edu.uno.cs.tjfs.client;

import edu.uno.cs.tjfs.Config;
import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.threads.JobExecutor;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Date;

public class TjfsClient implements ITjfsClient {
    private Config config;
    private IMasterClient masterClient;
    private IChunkClient chunkClient;

    public TjfsClient(Config config, IMasterClient masterClient, IChunkClient chunkClient) {
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

            // Push chunks to chunk servers in parallel using put jobs. Each job will update the
            // file descriptor with updated chunk descriptor.
            PutChunkJobProducer producer = new PutChunkJobProducer(masterClient, chunkClient,
                config.getChunkSize(), file, data, byteOffset);
            JobExecutor executor = new JobExecutor(producer, config.getExecutorPoolSize(),
                config.getExecutorQueueSize());
            executor.execute(); // ...might throw an exception.

            // Update the descriptor.
            masterClient.putFile(file);

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
