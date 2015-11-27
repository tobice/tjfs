package edu.uno.cs.tjfs.common.messages.arguments;

public class ListFileResponseArgs implements IMessageArgs {
    public String[] files;
    public ListFileResponseArgs(String[] files){
        this.files = files;
    }
}
