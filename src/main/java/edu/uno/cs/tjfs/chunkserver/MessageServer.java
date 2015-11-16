package edu.uno.cs.tjfs.chunkserver;

import edu.uno.cs.tjfs.common.MessageParser;
import edu.uno.cs.tjfs.common.messages.Response;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
            System.out.println("ChunkServer Started and listening to the port " + port);
            while(true)
            {
                //Reading the message from the client
                Socket socket = serverSocket.accept();
                InputStream socketInputStream = socket.getInputStream();
                OutputStream socketOutputStream = socket.getOutputStream();

                MessageParser parser = new MessageParser();
                Response response = this.server.process(parser.fromStream(socketInputStream));


                try {
                    IOUtils.copy(parser.toStreamFromResponse(response), socketOutputStream);
                }catch(Exception e){
                    //TODO: what should i do
                }



                socketOutputStream.flush();

                //socketInputStream.close();
                //socketOutputStream.close();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                //serverSocket.close();
            }
            catch(Exception e){}
        }
    }
}
