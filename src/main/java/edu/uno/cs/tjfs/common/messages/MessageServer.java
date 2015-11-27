package edu.uno.cs.tjfs.common.messages;

import edu.uno.cs.tjfs.common.BaseLogger;
import edu.uno.cs.tjfs.common.IServer;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MessageServer {
    private IServer server;
    public MessageServer(IServer server){
        this.server = server;
    }
    final static Logger logger = BaseLogger.getLogger(MessageServer.class);
    public void start(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        try
        {
            int id = 0;
            logger.info("MessageServer.start - server started and listening to the port " + port);
            while(true)
            {
                Socket clientSocket = serverSocket.accept();
                MessageServerWorkerThread cliThread = new MessageServerWorkerThread
                        (this.server, clientSocket, id++);
                cliThread.start();
            }
        }
        catch (Exception e)
        {
            logger.error("MessageServer.start - Chunkserver start error " + e.getMessage());
        }
        finally
        {
            try
            {
                //serverSocket.close();
            }
            catch(Exception e){
                logger.error("Server socket cannot be closed " + e.getMessage());
            }
        }
    }
}
