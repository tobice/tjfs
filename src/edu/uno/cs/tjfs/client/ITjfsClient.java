package edu.uno.cs.tjfs.client;

import java.io.InputStream;
import java.util.Date;

public interface ITjfsClient {
    /**
     * Get file contents at given path.
     * @param path file path
     * @return file content
     */
    InputStream get(String path);

    /**
     * Get file contents starting at the given byte offset.
     * @param path file path
     * @param byteOffset position in the file
     * @return file content
     */
    InputStream get(String path, int byteOffset);

    /**
     * Get specified length of bytes starting at the given byte offset.
     * @param path file path
     * @param byteOffset position in the file
     * @param numberOfBytes number of bytes
     * @return file content
     */
    InputStream get(String path, int byteOffset, int numberOfBytes);

    /**
     * Write data to the file at given path starting from the beginning. Create a new file if
     * it is not there.
     * @param path file path
     * @param data data to be written
     */
    void put(String path, InputStream data);

    /**
     * Write data to the file at the given path starting at the given byte offset. If the file
     * does not exist an exception is thrown.
     * @param path file path
     * @param data data to be written
     * @param byteOffset position in the file
     */
    void put(String path, InputStream data, int byteOffset);

    /**
     * Delete a file at given path.
     * @param path file path
     */
    void delete(String path);

    /**
     * Get size of a file at given path.
     * @param path file path
     * @return number of bytes
     */
    int getSize(String path);

    /**
     * Get last update time of a file at given path.
     * @param path file path
     * @return date and time of the last update
     */
    Date getTime(String path);

    /**
     * Return a list of files and directories in given directory.
     * @param path directory path
     * @return list of paths
     */
    String[] list(String path);

    /**
     * Move a file from one location to another.
     * @param sourcePath path to the file
     * @param destinationPath path to where the file should be moved
     */
    void move(String sourcePath, String destinationPath);
}
