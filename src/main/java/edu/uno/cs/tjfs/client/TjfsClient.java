package edu.uno.cs.tjfs.client;

import edu.uno.cs.tjfs.Config;
import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.FileDescriptor;
import edu.uno.cs.tjfs.common.messages.MessageClient;
import edu.uno.cs.tjfs.common.threads.JobExecutor;
import edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient;
import edu.uno.cs.tjfs.common.zookeeper.ZookeeperClient;
import edu.uno.cs.tjfs.common.zookeeper.ZookeeperException;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;

import static edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient.LockType.*;

public class TjfsClient implements ITjfsClient {
    private Config config;
    private IMasterClient masterClient;
    private IChunkClient chunkClient;
    private IZookeeperClient zkClient;

    final static Logger logger = Logger.getLogger(TjfsClient.class);

    public TjfsClient(Config config, IMasterClient masterClient, IChunkClient chunkClient, IZookeeperClient zkClient) {
        this.config = config;
        this.masterClient = masterClient;
        this.chunkClient = chunkClient;
        this.zkClient = zkClient;
    }

    @Override
    public InputStream get(Path path) throws TjfsClientException {
        return this.get(path, 0, Integer.MAX_VALUE);
    }

    @Override
    public InputStream get(Path path, int byteOffset) throws TjfsClientException {
        return this.get(path, byteOffset, Integer.MAX_VALUE);
    }

    @Override
    public InputStream get(Path path, int byteOffset, int numberOfBytes) throws TjfsClientException {
        try {
            FileDescriptor file = masterClient.getFile(path);

            // If an "infinity" is used, it means read the whole file. I know, I know, naming
            // conventions...
            final int length = numberOfBytes == Integer.MAX_VALUE ? file.getSize() : numberOfBytes;

            if (file.isEmpty()) {
                throw new TjfsException("File is empty (it does not exist)");
            }
            if (byteOffset + length > file.getSize()) {
                throw new TjfsException("Reading out of file range");
            }

            // Lock the file so that nobody changes it while we're reading
            final String lock = zkClient.acquireFileLock(path, READ);

            // "Pipe" incoming data from the chunk servers directly to the user. The input stream
            // is wrapped by another stream that lets us pass a possible exception to the end user.
            final PipedInputStream inputStream = new PipedInputStream(config.getPipeBufferSize());
            final InputStreamWithFinalExceptionCheck inWithException = new InputStreamWithFinalExceptionCheck(inputStream);
            final PipedOutputStream outputStream = new PipedOutputStream(inputStream);

            Thread thread = new Thread(() -> {
                try {
                    // Use the producer to generate jobs that will fetch the chunks from the
                    // chunk servers in parallel and write them into the output stream
                    GetChunkJobProducer producer = new GetChunkJobProducer(chunkClient,
                        outputStream, config.getChunkSize(), file, byteOffset, length);
                    JobExecutor executor = new JobExecutor(producer, config.getExecutorPoolSize(),
                        config.getExecutorQueueSize());
                    executor.execute();
                } catch (TjfsException e) {
                    // If we fail, we pass the exception through the special stream to the end user.
                    try {
                        outputStream.close();
                        inWithException.fail(new TjfsException("Downloading file failed. " + e.getMessage(), e));
                    } catch (IOException e1) {
                        inWithException.fail(new TjfsException("Downloading file failed. Plus " +
                            "unable to close the stream with message: " + e1.getMessage(), e));
                    }
                } finally {
                    // Let the stream know that it can throw the exception (because he has it now).
                    inWithException.countDown();
                    try {
                        zkClient.releaseFileLock(lock);
                    } catch (ZookeeperException e) {
                        logger.warn("Unable to release the file lock: " + e.getMessage());
                    }
                }
            });
            thread.start();

            return inWithException;
        } catch (TjfsException|IOException e) {
            String message = "File transfer failed. Reason: " + e.getMessage();
            throw new TjfsClientException(message, e);
        }
    }

    @Override
    public void put(Path path, InputStream data) throws TjfsClientException {
        put(path, data, 0);
    }

    @Override
    public void put(Path path, InputStream data, int byteOffset) throws TjfsClientException {
        try {
            // Lock the file so that nobody tries to change the file at the same time
            final String lock = zkClient.acquireFileLock(path, WRITE);

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

            zkClient.releaseFileLock(lock);
        } catch (TjfsException e) {
            String message = "File transfer failed. Reason: " + e.getMessage();
            throw new TjfsClientException(message, e);
        }
    }

    @Override
    public void delete(Path path) throws TjfsClientException {
        String lock = null;
        try {
            lock = zkClient.acquireFileLock(path, WRITE);

            FileDescriptor deletedFile = new FileDescriptor(path);
            masterClient.putFile(deletedFile);

        } catch (TjfsException e) {
            String message = "Deleting file failed. Reason: " + e.getMessage();
            throw new TjfsClientException(message, e);
        } finally {
            try {
                zkClient.releaseFileLock(lock);
            } catch (ZookeeperException e) {
                logger.warn("Unable to release the file lock: " + e.getMessage());
            }
        }
    }

    @Override
    public int getSize(Path path) throws TjfsClientException {
        try {
            return masterClient.getFile(path).getSize();
        } catch (TjfsException e) {
            throw new TjfsClientException("Failed to obtain file size. Reason: " + e.getMessage(), e);
        }
    }

    @Override
    public Date getTime(Path path) throws TjfsClientException {
        try {
            return masterClient.getFile(path).time;
        } catch (TjfsException e) {
            throw new TjfsClientException("Failed to obtain time. Reason: " + e.getMessage(), e);
        }
    }

    @Override
    public String[] list(Path path) throws TjfsClientException {
        try {
            return masterClient.list(path);
        } catch (TjfsException e) {
            throw new TjfsClientException("Failed to obtain the list. Reason: " + e.getMessage(), e);
        }
    }

    @Override
    public void move(Path sourcePath, Path destinationPath) throws TjfsClientException {
        String lockSource = null;
        String lockDestination = null;
        try {
            lockSource = zkClient.acquireFileLock(sourcePath, WRITE);
            lockDestination = zkClient.acquireFileLock(destinationPath, WRITE);

            // Moving a file is a simple procedure. We take the original descriptor, move it
            // to a new location and delete the old one (by replacing it with an empty descriptor).
            // Chunks remain the same (no reason to do anything with them).
            FileDescriptor source = masterClient.getFile(sourcePath);
            FileDescriptor destination = new FileDescriptor(destinationPath, new Date(), source.chunks);
            masterClient.putFile(new FileDescriptor(sourcePath, new Date(), new ArrayList<>()));
            masterClient.putFile(destination);
        } catch (TjfsException e) {
            throw new TjfsClientException("Unable to move the file. Reason: " + e.getMessage(), e);
        } finally {
            try {
                zkClient.releaseFileLock(lockSource);
                zkClient.releaseFileLock(lockDestination);
            } catch (ZookeeperException e) {
                logger.warn("Unable to release the file lock: " + e.getMessage());
            }
        }
    }

    /** Initialize instance of TjfsClient */
    public static TjfsClient getInstance(Config config, Machine zookeeper) throws ZookeeperException {
        ZookeeperClient zkClient = ZookeeperClient.connect(zookeeper, config.getZookeeperSessionTimeout());
        MessageClient messageClient = new MessageClient();
        ChunkClient chunkClient = new ChunkClient(messageClient);
        MasterClient masterClient = new MasterClient(messageClient, zkClient);
        return new TjfsClient(config, masterClient, chunkClient, zkClient);
    }
}
