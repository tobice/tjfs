package edu.uno.cs.tjfs.common.messages.arguments;

import edu.uno.cs.tjfs.common.ChunkDescriptor;

import java.util.List;

public class AllocateChunkResponseArgs implements IMessageArgs {
    public List<ChunkDescriptor> chunks;
    public String status;
    public AllocateChunkResponseArgs(List<ChunkDescriptor> chunks, String status){
        this.status = status;
        this.chunks = chunks;
    }
}
