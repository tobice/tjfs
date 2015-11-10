package edu.uno.cs.tjfs.client;

import edu.uno.cs.tjfs.common.ChunkDescriptor;
import edu.uno.cs.tjfs.common.IMasterClient;
import edu.uno.cs.tjfs.common.TjfsException;

import java.util.LinkedList;

/**
 * Allocates chunk names from the master server. It maintains its own buffer which allows to
 * allocate multiple chunk names using single remote call and then provide them one by one.
 */
public class ChunkNameAllocator {
    protected IMasterClient masterClient;
    protected int bufferSize;

    /** The buffer */
    protected LinkedList<ChunkDescriptor> allocated = new LinkedList<>();

    /**
     * @param masterClient connection to the master client
     * @param bufferSize number of chunk names that should be allocated in one remote call
     */
    public ChunkNameAllocator(IMasterClient masterClient, int bufferSize) {
        this.masterClient = masterClient;
        this.bufferSize = bufferSize;
    }

    /**
     * Get new allocated chunk name. If the inner buffer is empty (there are no more preloaded
     * chunk names), a remote call is made to the master server to get new chunk names. It may
     * take a while or even fail
     * @return new allocated chunk name together with target chunk servers
     * @throws TjfsException
     */
    public ChunkDescriptor getOne() throws TjfsException {
        if (allocated.size() == 0) {
            allocated.addAll(masterClient.allocateChunks(bufferSize));
        }

        return allocated.remove();
    }
}
