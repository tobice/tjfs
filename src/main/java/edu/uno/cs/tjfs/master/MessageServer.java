package edu.uno.cs.tjfs.master;

import edu.uno.cs.tjfs.chunkserver.IServer;
import edu.uno.cs.tjfs.common.BaseLogger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MessageServer {
    private IServer server;
    public MessageServer(IServer server){
        this.server = server;
    }
    public void start(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        try
        {
            int id = 0;
            BaseLogger.info("MessageServer.start - Master server Started and listening to the port " + port);
            while(true)
            {
                //Reading the message from the client
                Socket clientSocket = serverSocket.accept();
                MessageServerWorkerThread cliThread = new MessageServerWorkerThread
                        (this.server, clientSocket, id++);
                cliThread.start();

                //socketInputStream.close();
                //socketOutputStream.close();
            }
        }
        catch (Exception e)
        {
            BaseLogger.error("MessageServer.start - Master start error " + e.getMessage());
        }
        finally
        {
            try
            {
                //serverSocket.close();
            }
            catch(Exception e){
                BaseLogger.error("Server socket cannot be closed " + e.getMessage());
            }
        }
    }
}
