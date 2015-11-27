package edu.uno.cs.tjfs.common.messages.arguments;

import java.nio.file.Path;

public class DeleteFileRequestArgs implements IMessageArgs{
    public Path path;
    public DeleteFileRequestArgs(Path path){
        this.path = path;
    }
}
