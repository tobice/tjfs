package edu.uno.cs.tjfs.common.messages.arguments;

public class PutFileResponseArgs implements IMessageArgs {
    public String status;
    public PutFileResponseArgs(String status){
        this.status = status;
    }
}

