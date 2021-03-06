package edu.uno.cs.tjfs.common;

import edu.uno.cs.tjfs.master.IMasterStorage;
import edu.uno.cs.tjfs.master.MasterStorage;

import java.nio.file.Path;
import java.util.List;

public interface IMasterClient {
    /**
     * Ask master server to allocate new set of unique chunk names. The master server will reply
     * with the list of new chunk names together with distribution of those chunks among chunk
     * servers. That means that every chunk will have assigned list of target chunk servers where
     * they should be sent (each chunk will be replicated on multiple chunk servers).
     * @param number of chunks to allocate
     * @return set of chunk names and their indented target chunk servers.
     */
    List<ChunkDescriptor> allocateChunks(int number) throws TjfsException;

    /**
     * Return file descriptor for given file. If the file does not exist, an empty file
     * descriptor is returned (zero size, no chunks).
     * @param path location of the file
     * @return file descriptor
     * @throws TjfsException
     */
    FileDescriptor getFile(Path path) throws TjfsException;

    /**
     * Update file meta data on master server.
     * @param file descriptor containing all metadata including the updated chunks.
     * @throws TjfsException
     */
    void putFile(FileDescriptor file) throws TjfsException;

    /**
     * Gets the log for the shadow master from the actual master
     * @param logID the last log id to start from
     * @return list of file descriptors from the log of the actual master
     * @throws TjfsException
     */
    List<MasterStorage.LogItem> getLog(int logID) throws TjfsException;

    /**
     * Return a list of files and directories in given directory. Should return empty list when
     * the directory does not exist (= is empty).
     * @param path directory path
     * @return list file and directory names. Folders end with a slash (/)
     */
    String[] list(Path path) throws TjfsException;

    /**
     * Return latest snapshot from the master if available.
     * @return latest snapshot or null if there is no snapshot available
     * @throws TjfsException
     */
    IMasterStorage.Snapshot getLatestSnapshot() throws TjfsException;
}
