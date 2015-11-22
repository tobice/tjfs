package edu.uno.cs.tjfs.common.messages.arguments;

import edu.uno.cs.tjfs.common.FileDescriptor;

public class GetFileResponseArgs implements IMessageArgs {
    public FileDescriptor file;
    public String status;
    public GetFileResponseArgs(FileDescriptor file, String status){
        this.status = status;
        this.file = file;
    }
    public GetFileResponseArgs(FileDescriptor file){
        this.file = file;
        this.status = "";
    }
}
