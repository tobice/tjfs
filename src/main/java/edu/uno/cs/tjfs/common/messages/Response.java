package edu.uno.cs.tjfs.common.messages;

import edu.uno.cs.tjfs.common.messages.arguments.IMessageArgs;
import edu.uno.cs.tjfs.common.messages.MCode;

import java.io.InputStream;

public class Response extends Message{
    public MCode code;

    public Response (MCode code, IMessageArgs args, InputStream data, int dataLength){
        this.code = code;
        this.args = args;
        this.data = data;
        this.dataLength = dataLength;
    }
}