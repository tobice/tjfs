package edu.uno.cs.tjfs.common.messages.arguments;

public class PutChunkResponseArgs implements  IMessageArgs {
    public String status;
    public PutChunkResponseArgs(String status){
        this.status = status;
    }
}
