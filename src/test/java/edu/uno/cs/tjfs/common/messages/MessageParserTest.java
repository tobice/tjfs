package edu.uno.cs.tjfs.common.messages;

import com.google.gson.Gson;

import edu.uno.cs.tjfs.common.MessageParser;
import edu.uno.cs.tjfs.common.MessageParseException;
import edu.uno.cs.tjfs.common.messages.arguments.GetChunkRequestArgs;
import edu.uno.cs.tjfs.common.messages.arguments.GetChunkResponseArgs;
import edu.uno.cs.tjfs.common.messages.MCode;

import org.apache.commons.io.IOUtils;

import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.ExpectedException;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class MessageParserTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void fromStreamToRequestTest() throws Exception {
        Gson gson = new Gson();
        GetChunkRequestArgs args = new GetChunkRequestArgs("testChunk");
        String jsonMessage = gson.toJson(args);

        //Testing with no data stream
        String testMessage = "01" + String.format("%010d", jsonMessage.length()) + String.format("%010d", 10) + jsonMessage;

        MessageParser parser = new MessageParser();
        InputStream stream = IOUtils.toInputStream(testMessage, StandardCharsets.UTF_8);
        Request request = parser.fromStream(stream);

        assertTrue(request.header == MCommand.of("01"));
        assertTrue(((GetChunkRequestArgs) request.args).chunkName.equals(args.chunkName));
        assertTrue(IOUtils.toByteArray(request.data).length == 0);
        assertTrue(request.dataLength == 10); //had set this to 10 for testing

        //testing with some data in the datastream
        testMessage += "0000000000";
        stream = IOUtils.toInputStream(testMessage, StandardCharsets.UTF_8);
        request = parser.fromStream(stream);

        //Should pass earlier tests
        assertTrue(request.header == MCommand.of("01"));
        assertTrue(((GetChunkRequestArgs) request.args).chunkName.equals(args.chunkName));

        //And also the data length now should be 10
        assertTrue(IOUtils.toByteArray(request.data).length == 10);

        //testing with the invalid header
        testMessage = "66" + String.format("%010d", jsonMessage.length()) + String.format("%010d", 10) + jsonMessage;

        stream = IOUtils.toInputStream(testMessage, StandardCharsets.UTF_8);

        exception.expect(MessageParseException.class);
        exception.expectMessage("Invalid Header.");
        parser.fromStream(stream);
    }

    @Test
    public void toStreamFromRequestTest() throws IOException, MessageParseException{
        Gson gson = new Gson();
        GetChunkRequestArgs argsMessage = new GetChunkRequestArgs("testChunk");
        String jsonMessage = gson.toJson(argsMessage);

        //Test with no data
        Request request = new Request(MCommand.GET_CHUNK, argsMessage, null, 0);

        MessageParser parser = new MessageParser();
        InputStream result = parser.toStreamFromRequest(request);

        assertTrue(IOUtils.toString(IOUtils.toByteArray(result, 2), "UTF-8").equals("01"));
        assertTrue(Integer.parseInt(IOUtils.toString(IOUtils.toByteArray(result, 10), "UTF-8")) == jsonMessage.length());
        assertTrue(Integer.parseInt(IOUtils.toString(IOUtils.toByteArray(result, 10), "UTF-8")) == 0);

        assertTrue(IOUtils.toString(IOUtils.toByteArray(result, jsonMessage.length()), "UTF-8").equals(jsonMessage));

        assertTrue(result.available() == 0);

        //Test with some data
        String testMessage = "someDataInTheDataStream";
        InputStream stream = IOUtils.toInputStream(testMessage, StandardCharsets.UTF_8);
        request = new Request(MCommand.GET_CHUNK, argsMessage, stream, testMessage.length());

        result = parser.toStreamFromRequest(request);

        assertTrue(IOUtils.toString(IOUtils.toByteArray(result, 2), "UTF-8").equals("01"));
        assertTrue(Integer.parseInt(IOUtils.toString(IOUtils.toByteArray(result, 10), "UTF-8")) == jsonMessage.length());
        assertTrue(Integer.parseInt(IOUtils.toString(IOUtils.toByteArray(result, 10), "UTF-8")) == testMessage.length());
        assertTrue(IOUtils.toString(IOUtils.toByteArray(result, jsonMessage.length()), "UTF-8").equals(jsonMessage));
    }

    @Test
    public void fromStreamToResponse() throws Exception {
        //Create Response stream first
        Gson gson = new Gson();
        GetChunkResponseArgs args = new GetChunkResponseArgs("");
        String jsonMessage = gson.toJson(args);

        //Testing with no data stream
        String testMessage = "90" + String.format("%010d", jsonMessage.length()) + String.format("%010d", 10) + jsonMessage;

        MessageParser parser = new MessageParser();
        InputStream stream = IOUtils.toInputStream(testMessage, StandardCharsets.UTF_8);
        Response response = parser.fromStreamToResponse(stream, MCommand.GET_CHUNK.responseClass);

        assertTrue(response.code.value.equals(MCode.SUCCESS.value));
        assertTrue(((GetChunkResponseArgs) response.args).status.equals(args.status));
        assertTrue(IOUtils.toByteArray(response.data).length == 0);
        assertTrue(response.dataLength == 10); //had set this to 10 for testing

        //testing with some data in the datastream
        testMessage += "0000000000";
        stream = IOUtils.toInputStream(testMessage, StandardCharsets.UTF_8);
        response = parser.fromStreamToResponse(stream, MCommand.GET_CHUNK.responseClass);

        //Should pass earlier tests
        assertTrue(response.code == MCode.of("90"));
        assertTrue(((GetChunkResponseArgs) response.args).status.equals(args.status));

        //And also the data length now should be 10
        assertTrue(IOUtils.toByteArray(response.data).length == 10);

        //testing with the invalid header
        testMessage = "66" + String.format("%010d", jsonMessage.length()) + String.format("%010d", 10) + jsonMessage;

        stream = IOUtils.toInputStream(testMessage, StandardCharsets.UTF_8);

        exception.expect(MessageParseException.class);
        exception.expectMessage("Invalid Header.");
        parser.fromStreamToResponse(stream, MCommand.GET_CHUNK.responseClass);
    }

    @Test
    public void toStreamFromResponse() throws IOException, MessageParseException{
        Gson gson = new Gson();
        GetChunkResponseArgs argsMessage = new GetChunkResponseArgs("");
        String jsonMessage = gson.toJson(argsMessage);

        //Test with no data
        Response response = new Response(MCode.SUCCESS, argsMessage, null, 0);

        MessageParser parser = new MessageParser();
        InputStream result = parser.toStreamFromResponse(response);

        assertTrue(IOUtils.toString(IOUtils.toByteArray(result, 2), "UTF-8").equals("90"));
        assertTrue(Integer.parseInt(IOUtils.toString(IOUtils.toByteArray(result, 10), "UTF-8")) == jsonMessage.length());
        assertTrue(Integer.parseInt(IOUtils.toString(IOUtils.toByteArray(result, 10), "UTF-8")) == 0);

        assertTrue(IOUtils.toString(IOUtils.toByteArray(result, jsonMessage.length()), "UTF-8").equals(jsonMessage));

        assertTrue(result.available() == 0);

        //Test with some data
        String testMessage = "someDataInTheDataStream";
        InputStream stream = IOUtils.toInputStream(testMessage, StandardCharsets.UTF_8);
        response = new Response(MCode.SUCCESS, argsMessage, stream, testMessage.length());

        result = parser.toStreamFromResponse(response);

        assertTrue(IOUtils.toString(IOUtils.toByteArray(result, 2), "UTF-8").equals("90"));
        assertTrue(Integer.parseInt(IOUtils.toString(IOUtils.toByteArray(result, 10), "UTF-8")) == jsonMessage.length());
        assertTrue(Integer.parseInt(IOUtils.toString(IOUtils.toByteArray(result, 10), "UTF-8")) == testMessage.length());
        assertTrue(IOUtils.toString(IOUtils.toByteArray(result, jsonMessage.length()), "UTF-8").equals(jsonMessage));
    }
}


