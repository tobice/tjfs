package edu.uno.cs.tjfs.common.messages.arguments;

public class PutChunkRequestArgs implements IMessageArgs {
    public String chunkName;

    public PutChunkRequestArgs(String chunkName){
        this.chunkName = chunkName;
    }
}
