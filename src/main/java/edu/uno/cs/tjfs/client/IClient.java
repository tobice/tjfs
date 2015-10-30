package edu.uno.cs.tjfs.client;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;

/**
 * Client for transferring data between remote tjfs and local filesystem
 */
public interface IClient {
    /**
     * Get remote file and store it locally
     * @param source remote path
     * @param target local path
     * @throws IOException
     * @throws TjfsClientException
     */
    void get(Path source, Path target) throws IOException, TjfsClientException;

    /**
     * Get remote file content starting at byteOffset and store it locally
     * @param source remote path
     * @param target local path
     * @param byteOffset byte offset to use when reading remote file
     * @throws TjfsClientException
     * @throws IOException
     */
    void get(Path source, Path target, int byteOffset) throws TjfsClientException, IOException;

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
    void get(Path source, Path target, int byteOffset, int numberOfBytes) throws TjfsClientException, IOException;

    /**
     * Write local file to a remote file
     * @param source local path
     * @param target remote path
     * @throws IOException
     * @throws TjfsClientException
     */
    void put(Path source, Path target) throws IOException, TjfsClientException;

    /**
     * Write local file to a remote file at given byte offset
     * @param source local path
     * @param target remote path
     * @param byteOffset where to start writing in remote file
     * @throws IOException
     * @throws TjfsClientException
     */
    void put(Path source, Path target, int byteOffset) throws IOException, TjfsClientException;

    /**
     * Remove remote file
     * @param path remote path
     * @throws TjfsClientException
     */
    void delete(Path path) throws TjfsClientException;

    /**
     * Get size of a file at given path.
     * @param path remote path
     * @return number of bytes
     */
    int getSize(Path path) throws TjfsClientException;

    /**
     * Get last update time of a file at given path.
     * @param path remote path
     * @return date and time of the last update
     */
    Date getTime(Path path) throws TjfsClientException;

    /**
     * Return a list of files and directories in given directory.
     * @param path remote directory path
     * @return list of paths
     */
    String[] list(Path path) throws TjfsClientException;

    /**
     * Move a file from one location to another.
     * @param source remote path to the file
     * @param destination remote path to where the file should be moved
     */
    void move(Path source, Path destination) throws TjfsClientException;
}
