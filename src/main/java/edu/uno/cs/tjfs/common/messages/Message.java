package edu.uno.cs.tjfs.common.messages;

import edu.uno.cs.tjfs.common.messages.arguments.IMessageArgs;

abstract public class Message {
    public IMessageArgs args;
    public byte[] data;

    public int dataLength;
}