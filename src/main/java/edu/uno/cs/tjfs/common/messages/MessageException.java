package edu.uno.cs.tjfs.common.messages;

import edu.uno.cs.tjfs.common.TjfsException;

public class MessageException extends TjfsException {
    public MessageException(String s) {
        super(s);
    }
    public MessageException(String s, Throwable e){ super (s, e); }
}
