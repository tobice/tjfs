package edu.uno.cs.tjfs.common;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import edu.uno.cs.tjfs.common.messages.*;
import edu.uno.cs.tjfs.common.messages.arguments.IMessageArgs;
import edu.uno.cs.tjfs.common.messages.MCode;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MessageParser {
    final static Logger logger = BaseLogger.getLogger(MessageParser.class);
    public Request fromStream (InputStream stream) throws MessageParseException, IOException{
        Request result;
        try {
            String header = IOUtils.toString(IOUtils.toByteArray(stream, 2), "UTF-8");

            int argsLength = Integer.parseInt(IOUtils.toString(IOUtils.toByteArray(stream, 10), "UTF-8"));

            int rawLength = Integer.parseInt(IOUtils.toString(IOUtils.toByteArray(stream, 10), "UTF-8"));

            String jsonMessage = IOUtils.toString(IOUtils.toByteArray(stream, argsLength), "UTF-8");

            MCommand command;
            command = MCommand.of(header);

            Gson gson = CustomGson.create();
            IMessageArgs messageArgs = (IMessageArgs) gson.fromJson(jsonMessage, command.requestClass);
            if (messageArgs == null) throw new JsonSyntaxException("");
            result = new Request(command, messageArgs, IOUtils.toByteArray(stream, rawLength));
        }catch (IOException e){
            throw new MessageParseException("Invalid Stream,", e);
        }catch(IllegalArgumentException e){
            throw new MessageParseException("Invalid Header.", e);
        }catch (JsonSyntaxException e) {
            throw new MessageParseException("Invalid args.", e);
        }
        return result;
    }

    public InputStream toStreamFromRequest(Request request) throws BadRequestException{
        try {
            Gson gson = CustomGson.create();
            String jsonMessage = gson.toJson(request.args);
            //Create a message
            String message =
                    request.header.value +
                            String.format("%010d", jsonMessage.length()) +
                            String.format("%010d", request.dataLength) + jsonMessage;

            InputStream stream = IOUtils.toInputStream(message, StandardCharsets.UTF_8);

            if (request.data == null){
                return stream;
            }
            else {
                //Create one stream from the two streams
                List<InputStream> result = Arrays.asList(
                        stream,
                        new ByteArrayInputStream(request.data)
                );
                return new SequenceInputStream(Collections.enumeration(result));
            }
        }catch (Exception e){
            throw new BadRequestException(e.getMessage(), request);
        }
    }

    public Response fromStreamToResponse(InputStream stream, Class responseArgsClass) throws MessageParseException{
        Response result;
        try {
            String header = IOUtils.toString(IOUtils.toByteArray(stream, 2), "UTF-8");
            int argsLength = Integer.parseInt(IOUtils.toString(IOUtils.toByteArray(stream, 10), "UTF-8"));
            int rawLength = Integer.parseInt(IOUtils.toString(IOUtils.toByteArray(stream, 10), "UTF-8"));
            String jsonMessage = "";
            if (argsLength > 0) {
                jsonMessage = IOUtils.toString(IOUtils.toByteArray(stream, argsLength), "UTF-8");
            }

            MCode code = MCode.of(header);

            Gson gson = CustomGson.create();
            IMessageArgs messageArgs = jsonMessage.isEmpty() ? null : (IMessageArgs) gson.fromJson(jsonMessage, responseArgsClass);
            result = new Response(code, messageArgs, IOUtils.toByteArray(stream, rawLength));

        }catch(IOException e){
            logger.info(e.getMessage());
            throw new MessageParseException("Cannot consume message.", e);
        }catch(IllegalArgumentException e) {
            throw new MessageParseException("Invalid Header.", e);
        }catch(JsonSyntaxException e){
            throw new MessageParseException("Cannot parse Response.", e);
        }
        return result;
    }

    public InputStream toStreamFromResponse(Response response) throws BadResponseException{
        try {
            Gson gson = CustomGson.create();
            String jsonMessage = response.args == null ? "" : gson.toJson(response.args);

            //Create a message
            String message =
                    response.code.value +
                            String.format("%010d", jsonMessage.length()) +
                            String.format("%010d", response.dataLength) + jsonMessage;

            InputStream stream = IOUtils.toInputStream(message, StandardCharsets.UTF_8);
            if (response.data != null) {
                //Create one stream from the two streams
                List<InputStream> result = Arrays.asList(
                        stream,
                        new ByteArrayInputStream(response.data)
                );
                return new SequenceInputStream(Collections.enumeration(result));
            } else {
                return stream;
            }
        }catch(Exception e){
            throw new BadResponseException(e.getMessage(), response);
        }
    }
}
