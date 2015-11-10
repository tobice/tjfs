package edu.uno.cs.tjfs.common.messages.arguments;

public class GetChunkResponseArgs implements IMessageArgs {
    public String status;

    public GetChunkResponseArgs(String status){
        this.status = status;
    }
}
