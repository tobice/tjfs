package edu.uno.cs.tjfs.common.messages;

import edu.uno.cs.tjfs.common.BaseLogger;
import edu.uno.cs.tjfs.common.Machine;
import edu.uno.cs.tjfs.common.MessageParseException;
import edu.uno.cs.tjfs.common.MessageParser;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.Socket;

public class MessageClient implements IMessageClient {
    public Response send(Machine machine, Request request) throws BadRequestException, BadResponseException, ConnectionFailureException{
        BaseLogger.info("MessageClient.send - Sending a message to " + machine.ip + " at " + machine.port);
        InetAddress address;
        Socket socket = null;
        InputStream socketInStream = null;
        OutputStream socketOutStream = null;
        InputStream outGoingMessage;
        Response result;
        try {
            MessageParser parser = new MessageParser();
            outGoingMessage = parser.toStreamFromRequest(request);
            address = InetAddress.getByName(machine.ip);
            socket = new Socket(address, machine.port);

            socketOutStream = socket.getOutputStream();
            IOUtils.copy(outGoingMessage, socketOutStream);
            socketInStream = socket.getInputStream();

            socketOutStream.flush();

            result = parser.fromStreamToResponse(socketInStream, request.header.responseClass);
        }
        catch(BadRequestException e){
            BaseLogger.error("MessageClient.send - the request could not be converted into the stream.");
            BaseLogger.error("MessageClient.send", e);
            throw e;
        }
        catch (MessageParseException e){
            BaseLogger.error("MessageClient.send - the stream cannot be parsed to response.");
            BaseLogger.error("MessageClient.send", e);
            throw new BadResponseException(e.getMessage(), null);
        }
        catch (Exception e){
            BaseLogger.error("MessagClient.send - error connecting to the server.");
            //BaseLogger.error(e);
            throw new ConnectionFailureException(e.getMessage());
        }
        finally {
            try {
                socketInStream.close();
                socketOutStream.close();
                socket.close();
            }catch(Exception e){
//                BaseLogger.error("MessageClient.send - The client socket cannot be closed.");
//                BaseLogger.error("MessageClient.send", e);
            }
        }
        return result;
    }

    public void sendAsync(Machine machine, Request request) throws BadRequestException, ConnectionFailureException{
        BaseLogger.trace("MessageClient.SendAsyc - Sending the message asynchronously.");
        Thread thread = new Thread(() -> {
            try {
                send(machine, request);
            } catch (Exception e) {
                BaseLogger.error("SendAsync - " + e.getMessage());
            }
            BaseLogger.trace("MessageClient.SendAsyc - Finished sending the message asynchronously.");
        });
        thread.start();
    }
}
