package edu.uno.cs.tjfs.common.messages;

import edu.uno.cs.tjfs.common.Machine;
import edu.uno.cs.tjfs.common.MessageParser;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class MessageClient implements IMessageClient {
    public Response send(Machine machine, Request request) throws BadRequestException, BadResponseException, ConnectionFailureException{
        InetAddress address;
        Socket socket;
        InputStream socketInStream;
        OutputStream socketOutStream;
        InputStream outGoingMessage;
        Response result;
        MessageParser parser = new MessageParser();
        try{
            outGoingMessage = parser.toStreamFromRequest(request);
        }
        catch(Exception e){
            throw new BadRequestException(e.getMessage(), request);
        }

        try {
            address = InetAddress.getByName(machine.ip);
            socket = new Socket(address, machine.port);

            socketOutStream = socket.getOutputStream();
            IOUtils.copy(outGoingMessage, socketOutStream);
            socketInStream = socket.getInputStream();

        }
        catch (Exception e){
            throw new ConnectionFailureException(e.getMessage());
        }

        try{
            result = parser.fromStreamToResponse(socketInStream, request.header.responseClass);
        }
        catch (Exception e){
            throw new BadResponseException(e.getMessage(), null);
        }

        
        return result;
    }

    public void sendAsync(Machine machine, Request request) throws BadRequestException, ConnectionFailureException{
        InetAddress address;
        Socket socket;
        OutputStream socketOutStream;
        InputStream outGoingMessage;
        MessageParser parser = new MessageParser();
        try{
            outGoingMessage = parser.toStreamFromRequest(request);
        }
        catch(Exception e){
            throw new BadRequestException(e.getMessage(), request);
        }

        try {
            address = InetAddress.getByName(machine.ip);
            socket = new Socket(address, machine.port);

            socketOutStream = socket.getOutputStream();
            IOUtils.copy(outGoingMessage, socketOutStream);

            socketOutStream.flush();
            socketOutStream.close();

            socket.close();
        }
        catch (Exception e){
            throw new ConnectionFailureException(e.getMessage());
        }
    }
}
