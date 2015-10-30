package edu.uno.cs.tjfs.client;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Date;

public interface ITjfsClient {
    /**
     * Get file contents at given path.
     * @param path file path
     * @return file content
     */
    InputStream get(Path path) throws TjfsClientException;

    /**
     * Get file contents starting at the given byte offset.
     * @param path file path
     * @param byteOffset position in the file
     * @return file content
     */
    InputStream get(Path path, int byteOffset) throws TjfsClientException;

    /**
     * Get specified length of bytes starting at the given byte offset.
     * @param path file path
     * @param byteOffset position in the file
     * @param numberOfBytes number of bytes
     * @return file content
     */
    InputStream get(Path path, int byteOffset, int numberOfBytes) throws TjfsClientException;

    /**
     * Write data to the file at given path starting from the beginning. Create a new file if
     * it is not there.
     * @param path file path
     * @param data data to be written
     */
    void put(Path path, InputStream data) throws TjfsClientException;

    /**
     * Write data to the file at the given path starting at the given byte offset. If the file
     * does not exist an exception is thrown.
     * @param path file path
     * @param data data to be written
     * @param byteOffset position in the file
     */
    void put(Path path, InputStream data, int byteOffset) throws TjfsClientException;

    /**
     * Delete a file at given path.
     * @param path file path
     */
    void delete(Path path) throws TjfsClientException;

    /**
     * Get size of a file at given path.
     * @param path file path
     * @return number of bytes
     */
    int getSize(Path path) throws TjfsClientException;

    /**
     * Get last update time of a file at given path.
     * @param path file path
     * @return date and time of the last update
     */
    Date getTime(Path path) throws TjfsClientException;

    /**
     * Return a list of files and directories in given directory. Should return empty list when
     * the directory does not exist (= is empty).
     * @param path directory path
     * @return list of absolute paths. Folders end with a slash (/)
     */
    String[] list(Path path) throws TjfsClientException;

    /**
     * Move a file from one location to another.
     * @param sourcePath path to the file
     * @param destinationPath path to where the file should be moved
     */
    void move(Path sourcePath, Path destinationPath) throws TjfsClientException;
}
