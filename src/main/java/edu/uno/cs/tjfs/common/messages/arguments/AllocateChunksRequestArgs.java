package edu.uno.cs.tjfs.common.messages.arguments;

public class AllocateChunksRequestArgs implements IMessageArgs{
    public int number;
    public AllocateChunksRequestArgs(int number){
        this.number = number;
    }
}
