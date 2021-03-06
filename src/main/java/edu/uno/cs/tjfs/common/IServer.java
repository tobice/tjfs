package edu.uno.cs.tjfs.common;

import edu.uno.cs.tjfs.common.messages.Request;
import edu.uno.cs.tjfs.common.messages.Response;

public interface IServer {
    Response process(Request request) throws TjfsException;
}
