package edu.uno.cs.tjfs.chunkserver;

import edu.uno.cs.tjfs.common.messages.MessageServer;

import java.io.IOException;

/**
 * Created by janak on 11/6/2015.
 */
public class ServerLauncher {
    public static void main(String[] args) throws IOException {
        System.out.println("Starting the chunk server");

        Server server = new Server();

        MessageServer msgServer = new MessageServer("", 6001, server);

        msgServer.listen();
    }
}
