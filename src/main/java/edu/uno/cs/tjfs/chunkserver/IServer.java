package edu.uno.cs.tjfs.chunkserver;

import edu.uno.cs.tjfs.common.TjfsException;
import edu.uno.cs.tjfs.common.messages.Request;
import edu.uno.cs.tjfs.common.messages.Response;

public interface IServer {
    Response process(Request request) throws TjfsException;
    Response process(Request request, int threadID) throws TjfsException;
}
