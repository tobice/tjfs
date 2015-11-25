package edu.uno.cs.tjfs.common.messages.arguments;

import edu.uno.cs.tjfs.common.FileDescriptor;

import java.nio.file.Path;

public class PutFileRequestArgs implements IMessageArgs {
    public FileDescriptor file;
    public PutFileRequestArgs(FileDescriptor file){
        this.file = file;
    }
}
