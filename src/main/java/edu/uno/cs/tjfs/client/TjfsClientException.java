package edu.uno.cs.tjfs.client;

import edu.uno.cs.tjfs.common.TjfsException;

public class TjfsClientException extends TjfsException {
    public TjfsClientException(String s) {
        super(s);
    }

    public TjfsClientException(String s, Throwable reason) {
        super(s, reason);
    }
}
