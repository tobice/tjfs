package edu.uno.cs.tjfs.common.messages.arguments;

import java.nio.file.Path;

public class GetFileRequestArgs implements IMessageArgs {
    public Path path;
    public GetFileRequestArgs(Path path){
        this.path = path;
    }
}
