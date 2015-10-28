package edu.uno.cs.tjfs.client;

import edu.uno.cs.tjfs.common.ILocalFsClient;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;

/**
 * Client for transferring data between remote tjfs and local filesystem
 */
public class Client implements IClient {

    /** Client connected to tjfs */
    private ITjfsClient tjfsClient;

    /** Client for accessing local fs */
    private ILocalFsClient localFsClient;

    /** Current remote path in Tjfs */
    private Path pwd;

    public Client(ITjfsClient tjfsClient, ILocalFsClient localFsClient) {
        this.tjfsClient = tjfsClient;
        this.localFsClient = localFsClient;
    }

    /**
     * Get remote file and store it locally
     * @param source remote path
     * @param target local path
     * @throws IOException
     * @throws TjfsClientException
     */
    public void get(Path source, Path target) throws IOException, TjfsClientException {
        localFsClient.writeFile(target, tjfsClient.get(source));
    }

    /**
     * Get remote file content starting at byteOffset and store it locally
     * @param source remote path
     * @param target local path
     * @param byteOffset byte offset to use when reading remote file
     * @throws TjfsClientException
     * @throws IOException
     */
    public void get(Path source, Path target, int byteOffset) throws TjfsClientException, IOException {
        localFsClient.writeFile(target, tjfsClient.get(source, byteOffset));
    }

    /**
     * Get remote file content starting at byteOffset of given numberOfBytes length and store it
     * locally.
     * @param source remote path
     * @param target local path
     * @param byteOffset byte offset to use when reading remote file
     * @param numberOfBytes length of the content to be read
     * @throws TjfsClientException
     * @throws IOException
     */
    public void get(Path source, Path target, int byteOffset, int numberOfBytes) throws TjfsClientException, IOException {
        localFsClient.writeFile(target, tjfsClient.get(source, byteOffset, numberOfBytes));
    }

    /**
     * Write local file to a remote file
     * @param source local path
     * @param target remote path
     * @throws IOException
     * @throws TjfsClientException
     */
    public void put(Path source, Path target) throws IOException, TjfsClientException {
        tjfsClient.put(target, localFsClient.readFile(source));
    }

    /**
     * Write local file to a remote file at given byte offset
     * @param source local path
     * @param target remote path
     * @param byteOffset where to start writing in remote file
     * @throws IOException
     * @throws TjfsClientException
     */
    public void put(Path source, Path target, int byteOffset) throws IOException, TjfsClientException {
        tjfsClient.put(target, localFsClient.readFile(source), byteOffset);
    }

    /**
     * Remove remote file
     * @param path remote path
     * @throws TjfsClientException
     */
    public void delete(Path path) throws TjfsClientException {
        tjfsClient.delete(path);
    }

    /**
     * Get size of a file at given path.
     * @param path remote path
     * @return number of bytes
     */
    public int getSize(Path path) throws TjfsClientException {
        return tjfsClient.getSize(path);
    }

    /**
     * Get last update time of a file at given path.
     * @param path remote path
     * @return date and time of the last update
     */
    public Date getTime(Path path) throws TjfsClientException {
        return tjfsClient.getTime(path);
    }

    /**
     * Return a list of files and directories in given directory.
     * @param path remote directory path
     * @return list of paths
     */
    public String[] list(Path path) throws TjfsClientException {
        return tjfsClient.list(path);
    }

    /**
     * Move a file from one location to another.
     * @param source remote path to the file
     * @param destination remote path to where the file should be moved
     */
    public void move(Path source, Path destination) throws TjfsClientException {
        tjfsClient.move(source, destination);
    }
}
