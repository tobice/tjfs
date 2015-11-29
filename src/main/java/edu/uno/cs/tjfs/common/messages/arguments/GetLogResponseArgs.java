package edu.uno.cs.tjfs.common.messages.arguments;

import edu.uno.cs.tjfs.common.FileDescriptor;
import edu.uno.cs.tjfs.master.IMasterStorage;

import java.util.List;

public class GetLogResponseArgs implements IMessageArgs{
    public List<IMasterStorage.LogItem> log;
    public GetLogResponseArgs(List<IMasterStorage.LogItem> log) {
        this.log = log;
    }
}
