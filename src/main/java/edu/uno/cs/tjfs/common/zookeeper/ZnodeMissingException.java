package edu.uno.cs.tjfs.common.zookeeper;

import edu.uno.cs.tjfs.common.TjfsException;

public class ZnodeMissingException extends TjfsException {
    public ZnodeMissingException(String s) {
        super(s);
    }

    public ZnodeMissingException(String s, Throwable reason) {
        super(s, reason);
    }
}
