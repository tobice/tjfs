package edu.uno.cs.tjfs.common.messages;

import edu.uno.cs.tjfs.common.messages.SocketMessage;

import java.io.IOException;

/**
 * Created by srjanak on 11/6/15.
 */
public interface IMessageServer {
    SocketMessage send(SocketMessage message) throws IOException;
    SocketMessage sendAsync(SocketMessage message) throws IOException;
}
