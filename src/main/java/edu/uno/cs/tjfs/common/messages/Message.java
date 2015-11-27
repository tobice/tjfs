package edu.uno.cs.tjfs.common.messages;

import edu.uno.cs.tjfs.common.messages.arguments.IMessageArgs;

abstract public class Message {
    public final IMessageArgs args;
    public final byte[] data;
    public final int dataLength;

    protected Message(IMessageArgs args, byte[] data) {
        this.args = args;
        this.data = data;
        this.dataLength = data.length;
    }

    protected Message(IMessageArgs args) {
        this.args = args;
        this.data = null;
        this.dataLength = 0;
    }
}