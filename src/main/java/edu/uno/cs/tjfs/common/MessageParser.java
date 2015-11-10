package edu.uno.cs.tjfs.common;

import com.google.gson.Gson;
import edu.uno.cs.tjfs.common.messages.*;
import edu.uno.cs.tjfs.common.messages.arguments.GetChunkRequestArgs;
import edu.uno.cs.tjfs.common.messages.arguments.IMessageArgs;
import edu.uno.cs.tjfs.common.messages.MCode;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.commons.io.IOUtils.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MessageParser {
    final static Logger logger = Logger.getLogger(MessageParser.class);
    public Request fromStream (InputStream stream) throws MessageParseException, IOException{
        String jsonMessage;
        String header;
        int rawLength;
        try {

            header = IOUtils.toString(IOUtils.toByteArray(stream, 2), "UTF-8");

            int argsLength = Integer.parseInt(IOUtils.toString(IOUtils.toByteArray(stream, 10), "UTF-8"));

            rawLength = Integer.parseInt(IOUtils.toString(IOUtils.toByteArray(stream, 10), "UTF-8"));

            jsonMessage = IOUtils.toString(IOUtils.toByteArray(stream, argsLength), "UTF-8");

        }catch(Exception e){
            throw new MessageParseException("Cannot Consume Message");
        }

        MCommand command;
        try {
            command = MCommand.of(header);
        }catch(Exception e){
            throw new MessageParseException("Invalid header.");
        }

        Gson gson = new Gson();
        Request result;
        try {
            IMessageArgs messageArgs = (IMessageArgs) gson.fromJson(jsonMessage, command.requestClass);
            if (messageArgs == null) throw new Exception("");
            result = new Request(command, messageArgs, stream, rawLength);
        }catch (Exception e){
            throw new MessageParseException("Cannot parse Request.");
        }
        return result;
    }

    public InputStream toStreamFromRequest(Request request){
        Gson gson = new Gson();
        String jsonMessage = gson.toJson(request.args);

        //Create a message
        String message =
                        request.header.value +
                        String.format("%010d", jsonMessage.length()) +
                        String.format("%010d", request.dataLength) + jsonMessage;

        InputStream stream = IOUtils.toInputStream(message, StandardCharsets.UTF_8);

        //Create one stream from the two streams
        List<InputStream> result = Arrays.asList(
                stream,
                request.data
        );

       return new SequenceInputStream(Collections.enumeration(result));
    }

    public Response fromStreamToResponse(InputStream stream, Class responseArgsClass) throws MessageParseException{
        String jsonMessage;
        String header;
        int rawLength;
        try {

            header = IOUtils.toString(IOUtils.toByteArray(stream, 2), "UTF-8");

            int argsLength = Integer.parseInt(IOUtils.toString(IOUtils.toByteArray(stream, 10), "UTF-8"));

            rawLength = Integer.parseInt(IOUtils.toString(IOUtils.toByteArray(stream, 10), "UTF-8"));

            jsonMessage = IOUtils.toString(IOUtils.toByteArray(stream, argsLength), "UTF-8");

        }catch(Exception e){
            throw new MessageParseException("Cannot Consume Message");
        }

        MCode code;
        try {
            code = MCode.of(header);
        }catch(Exception e){
            throw new MessageParseException("Invalid header.");
        }

        Gson gson = new Gson();
        Response result;
        try {
            IMessageArgs messageArgs = (IMessageArgs) gson.fromJson(jsonMessage, responseArgsClass);
            if (messageArgs == null) throw new Exception("");
            result = new Response(code, messageArgs, stream, rawLength);
        }catch (Exception e){
            throw new MessageParseException("Cannot parse Response.");
        }
        return result;
    }

    public InputStream toStreamFromResponse(Response response){
        Gson gson = new Gson();
        String jsonMessage = gson.toJson(response.args);

        //Create a message
        String message =
                response.code.value +
                        String.format("%010d", jsonMessage.length()) +
                        String.format("%010d", response.dataLength) + jsonMessage;

        InputStream stream = IOUtils.toInputStream(message, StandardCharsets.UTF_8);

        //Create one stream from the two streams
        List<InputStream> result = Arrays.asList(
                stream,
                response.data
        );
        return new SequenceInputStream(Collections.enumeration(result));
    }
}
