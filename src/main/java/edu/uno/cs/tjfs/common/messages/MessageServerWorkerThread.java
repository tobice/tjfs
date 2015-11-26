package edu.uno.cs.tjfs.common.messages;

import com.google.gson.Gson;
import edu.uno.cs.tjfs.common.BaseLogger;
import edu.uno.cs.tjfs.common.CustomGson;
import edu.uno.cs.tjfs.common.IServer;
import edu.uno.cs.tjfs.common.MessageParser;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

class MessageServerWorkerThread extends Thread {
    Socket clientSocket;
    int clientID = -1;
    IServer server;
    final static Logger logger = BaseLogger.getLogger(MessageServerWorkerThread.class);

    MessageServerWorkerThread(IServer server, Socket s, int i) {
        this.server = server;
        clientSocket = s;
        clientID = i;
    }

    public void run() {
        logger.info("MessageServerWorkerThread.run : Accepted Client : ID - " + clientID + " : Address - "
                + clientSocket.getInetAddress().getHostName());
        try {
            //Reading the message from the client
            InputStream socketInputStream = clientSocket.getInputStream();
            OutputStream socketOutputStream = clientSocket.getOutputStream();
            try {
                MessageParser parser = new MessageParser();
                Response response;
                try {
                    Request request = parser.fromStream(socketInputStream);
                    Gson gson = CustomGson.create();
                    logger.info("Processing following request ");
                    logger.info("Header is " + request.header);
                    logger.info("Json is " + gson.toJson(request.args));
                    logger.info("Data length is  is " + request.dataLength);
                    response = this.server.process(request);
                    logger.info("Processing following response ");
                    logger.info("Header is " + response.code);
                    logger.info("Json is " + gson.toJson(response.args));
                    logger.info("Data length is  is " + response.dataLength);
                }catch (Exception e){
                    logger.error("MessageServerWorkerThread.run: Error while processing the chunk. Replying with the error message");
                    logger.error("MessageServerWorkerThread.run: ", e);
                    response = Response.Error(e.getMessage());
                }
                IOUtils.copy(parser.toStreamFromResponse(response), socketOutputStream);
            }catch(Exception e){
                logger.error("MessageServerWorkerThread.run" + e.getMessage());
                logger.error(e.getStackTrace().toString());
            }
            socketOutputStream.flush();
        } catch (Exception e) {
            logger.error("MessageServerWorkerThread.run : " + clientID);
        }
        logger.info("MessageServerWorkerThread.run : Finished running the client - ID -> " + clientID);
    }
}
