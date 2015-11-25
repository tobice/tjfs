package edu.uno.cs.tjfs.common.messages;

import edu.uno.cs.tjfs.common.BaseLogger;
import edu.uno.cs.tjfs.common.IServer;
import edu.uno.cs.tjfs.common.MessageParser;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

class MessageServerWorkerThread extends Thread {
    Socket clientSocket;
    int clientID = -1;
    IServer server;

    MessageServerWorkerThread(IServer server, Socket s, int i) {
        this.server = server;
        clientSocket = s;
        clientID = i;
    }

    public void run() {
        BaseLogger.info("MessageServerWorkerThread.run : Accepted Client : ID - " + clientID + " : Address - "
                + clientSocket.getInetAddress().getHostName());
        try {
            //Reading the message from the client
            InputStream socketInputStream = clientSocket.getInputStream();
            OutputStream socketOutputStream = clientSocket.getOutputStream();
            try {
                MessageParser parser = new MessageParser();
                Response response;
                try {
                    response = this.server.process(parser.fromStream(socketInputStream));
                }catch (Exception e){
                    BaseLogger.error("MessageServerWorkerThread.run: Error while processing the chunk. Replying with the error message");
                    BaseLogger.error("MessageServerWorkerThread.run: ", e);
                    response = Response.Error(e.getMessage());
                }
                IOUtils.copy(parser.toStreamFromResponse(response), socketOutputStream);
            }catch(Exception e){
                BaseLogger.error("MessageServerWorkerThread.run" + e.getMessage());
                BaseLogger.error(e.getStackTrace().toString());
            }
            socketOutputStream.flush();
        } catch (Exception e) {
            BaseLogger.error("MessageServerWorkerThread.run : " + clientID);
        }
        BaseLogger.info("MessageServerWorkerThread.run : Finished running the client - ID -> " + clientID);
    }
}
