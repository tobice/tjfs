package edu.uno.cs.tjfs.common;

import edu.uno.cs.tjfs.client.AllocatedChunkName;

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
    List<AllocatedChunkName> allocateChunks(int number) throws TjfsException;
}
