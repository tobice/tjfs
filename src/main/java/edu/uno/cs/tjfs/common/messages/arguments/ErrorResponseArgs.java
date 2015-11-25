package edu.uno.cs.tjfs.common.messages.arguments;

/**
 * Created by srjanak on 11/22/15.
 */
public class ErrorResponseArgs implements IMessageArgs {
    public String status;
    public ErrorResponseArgs(String status){
        this.status = status;
    }
}
