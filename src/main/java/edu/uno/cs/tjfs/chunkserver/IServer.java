package edu.uno.cs.tjfs.chunkserver;

import edu.uno.cs.tjfs.common.messages.SocketMessage;

/**
 * Created by janak on 11/6/2015.
 */
public interface IServer {
    SocketMessage process(SocketMessage message);
}
