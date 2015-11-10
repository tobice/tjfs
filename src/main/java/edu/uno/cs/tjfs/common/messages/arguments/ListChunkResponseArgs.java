package edu.uno.cs.tjfs.common.messages.arguments;

public class ListChunkResponseArgs implements IMessageArgs{
    public String[] chunks;
    public String status;

    public ListChunkResponseArgs(String[] chunks, String status){
        this.chunks = chunks;
        this.status = status;
    }
}
