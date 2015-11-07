package edu.uno.cs.tjfs.common.messages;

import edu.uno.cs.tjfs.chunkserver.Server;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;

/**
 * Created by janak on 11/6/2015.
 */
public class MessageServer implements IMessageServer{
    /* When sending a socket message this is the ip address the Message server uses. */
    private String ipAddress;
    /* When sending a socket message or listening on a server this socket is what the message server uses. */
    private int port;
    /* When a new message comes in, this server processes the message */
    private Server server;


    public MessageServer(String ipAddress, int port, Server chunkServer){
        this.ipAddress = ipAddress;
        this.port = port;
        this.server = chunkServer;
    }

    /**
     * Listens on the socket on the given port
     * @throws IOException
     */
    public void listen() throws IOException{
        ServerSocket serverSocket = new ServerSocket(this.port);
        try
        {
            System.out.println("Server Started and listening to the port " + this.port);
            while(true)
            {
                //Reading the message from the client
                Socket socket = serverSocket.accept();
                InputStream in = socket.getInputStream();

                SocketMessage msg = readMessageFromInStream(in);
                SocketMessage response = this.server.process(msg);

                writeMessageToOutStream(socket.getOutputStream(), response);
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
                serverSocket.close();
            }
            catch(Exception e){}
        }
    }

    /**
     * Sends the message over the socket using the ipAddress and the port
     * @param message message to be passed into the socket
     * @return SocketMessage received from the socket
     * @throws IOException
     */
    public SocketMessage send(SocketMessage message) throws IOException{
        InetAddress address = InetAddress.getByName(this.ipAddress);
        Socket socket = new Socket(address, this.port);
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();
        writeMessageToOutStream(out, message);

        SocketMessage response = readMessageFromInStream(in);

        out.close();
        in.close();
        return response;
    }

    /**
     * Reads from the given input stream and parses into a socket message
     * @param in the input stream to read from
     * @return encoded data into a SocketMessage from input stream
     * @throws IOException
     */
    private SocketMessage readMessageFromInStream(InputStream in) throws IOException{
        DataInputStream dis = new DataInputStream(in);

        String utfMessage = dis.readUTF();

        String header = utfMessage.substring(0, 4);

        System.out.println("Message received from client is "+ utfMessage);
        int jsonPartLength = Integer.parseInt(utfMessage.substring(4, 14));
        System.out.println("The json part length is " + jsonPartLength);

        String theJsonPart = utfMessage.substring(24, (24 + jsonPartLength));
        System.out.println("the json part is " + theJsonPart);

        int rawPartLength = Integer.parseInt(utfMessage.substring(14, 24));
        System.out.println("the raw part length is " + rawPartLength);

        SocketMessage returnResult = new SocketMessage(header, theJsonPart, rawPartLength, in);

        return returnResult;
    }

    /**
     * Writes the socket message to the given output stream
     * @param outStream the output stream to write to
     * @param message the message to be written to the output stream
     * @throws IOException
     */
    private void writeMessageToOutStream(OutputStream outStream, SocketMessage message) throws IOException{
        DataOutputStream dOut = new DataOutputStream(outStream);
        System.out.println("Writing message to socket " + message.toString());
        dOut.writeUTF(message.toString());
        if (message.data != null) IOUtils.copy(message.data, outStream);
        dOut.flush();
    }

    /**
     * This function should send the message without waiting for the reply
     * @param message
     * @return the SocketMessage received after asynchronous connection
     */
    public SocketMessage sendAsync(SocketMessage message){
        //TODO: this should be implemented for the async socket messaging
        return null;
    }
}
