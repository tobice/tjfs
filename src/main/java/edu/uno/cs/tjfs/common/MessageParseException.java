package edu.uno.cs.tjfs.common;

import edu.uno.cs.tjfs.common.messages.MessageException;

public class MessageParseException extends MessageException {
    public MessageParseException(String s) {
        super(s);
    }

    public MessageParseException(String s, Throwable e){
        super(s, e);
    }
}
