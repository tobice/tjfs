package edu.uno.cs.tjfs.common.messages.arguments;

public class DeleteChunkRequestArgs implements IMessageArgs {
    public String chunkName;
    public DeleteChunkRequestArgs(String chunkName){
        this.chunkName = chunkName;
    }
}
