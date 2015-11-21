package edu.uno.cs.tjfs.common.zookeeper;

import edu.uno.cs.tjfs.common.TjfsException;

public class ZnodeTakenException extends TjfsException {
    public ZnodeTakenException(String s) {
        super(s);
    }

    public ZnodeTakenException(String s, Throwable reason) {
        super(s, reason);
    }
}
