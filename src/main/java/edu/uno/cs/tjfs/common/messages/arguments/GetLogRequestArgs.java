package edu.uno.cs.tjfs.common.messages.arguments;

public class GetLogRequestArgs implements IMessageArgs{
    public int logID;
    public GetLogRequestArgs(int logID){
        this.logID = logID;
    }
}
