package edu.uno.cs.tjfs.common.threads;

import edu.uno.cs.tjfs.common.TjfsException;

public class UnableToProduceJobException extends TjfsException {
    public UnableToProduceJobException(String s) {
        super(s);
    }

    public UnableToProduceJobException(String s, Throwable reason) {
        super(s, reason);
    }
}
