package edu.uno.cs.tjfs.common.messages.arguments;

public class GetLogRequestArgs implements IMessageArgs{
    public int lastVersion;
    public GetLogRequestArgs(int lastVersion){
        this.lastVersion = lastVersion;
    }
}
