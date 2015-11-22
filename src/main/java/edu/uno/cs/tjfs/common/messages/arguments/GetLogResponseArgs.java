package edu.uno.cs.tjfs.common.messages.arguments;

import edu.uno.cs.tjfs.common.FileDescriptor;

import java.util.List;

public class GetLogResponseArgs implements IMessageArgs{
    public List<FileDescriptor> logs;
    public GetLogResponseArgs(List<FileDescriptor> logs){
        this.logs = logs;
    }
}
