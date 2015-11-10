package edu.uno.cs.tjfs.common.messages.arguments;

public class GetChunkRequestArgs implements IMessageArgs {
    public String chunkName;

    public GetChunkRequestArgs(String chunkName){
        this.chunkName = chunkName;
    }
}
