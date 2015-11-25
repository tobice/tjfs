package edu.uno.cs.tjfs.common.messages.arguments;

import edu.uno.cs.tjfs.common.Machine;

/**
 * Created by srjanak on 11/10/15.
 */
public class
        ReplicateChunkRequestArgs implements IMessageArgs{
    public String chunkName;
    public Machine machine;

    public ReplicateChunkRequestArgs(String chunkName, Machine machine){
        this.chunkName = chunkName;
        this.machine = machine;
    }
}
