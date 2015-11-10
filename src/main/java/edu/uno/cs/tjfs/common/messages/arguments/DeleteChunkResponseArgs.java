package edu.uno.cs.tjfs.common.messages.arguments;

public class DeleteChunkResponseArgs implements IMessageArgs {
    public String status;
    public DeleteChunkResponseArgs(String status){
        this.status = status;
    }
}
