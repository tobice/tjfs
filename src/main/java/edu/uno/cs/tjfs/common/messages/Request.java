package edu.uno.cs.tjfs.common.messages;

import edu.uno.cs.tjfs.common.messages.arguments.IMessageArgs;

import java.io.InputStream;

public class Request extends Message {
    public MCommand header;

    public Request(MCommand header, IMessageArgs args, InputStream data, int dataLength){
        this.header = header;
        this.args = args;
        this.data = data;
        this.dataLength = dataLength;
    }
}