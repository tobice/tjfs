package edu.uno.cs.tjfs.client;

import edu.uno.cs.tjfs.common.IMasterClient;
import edu.uno.cs.tjfs.common.TjfsException;

import java.util.LinkedList;

public class ChunkNameAllocator {
    protected IMasterClient masterClient;
    protected int bufferSize;
    protected LinkedList<AllocatedChunkName> allocated = new LinkedList<>();

    public ChunkNameAllocator(IMasterClient masterClient, int bufferSize) {
        this.masterClient = masterClient;
        this.bufferSize = bufferSize;
    }

    public AllocatedChunkName getOne() throws TjfsException {
        if (allocated.size() == 0) {
            allocated.addAll(masterClient.allocateChunks(bufferSize));
        }

        return allocated.remove();
    }
}
