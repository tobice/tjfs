package edu.uno.cs.tjfs.common.messages;

import edu.uno.cs.tjfs.common.messages.arguments.IMessageArgs;

import java.io.InputStream;

abstract public class Message {
    public IMessageArgs args;
    public InputStream data;

    public int dataLength;
}