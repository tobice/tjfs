package edu.uno.cs.tjfs.common.messages.arguments;

import java.nio.file.Path;

public class ListFileRequestArgs implements IMessageArgs {
    public Path path;
    public ListFileRequestArgs(Path path){
        this.path = path;
    }
}
