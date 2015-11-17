package edu.uno.cs.tjfs.common;

public class TjfsException extends Exception {
    public TjfsException(String s) {
        super(s);
    }

    public TjfsException(String s, Throwable reason) {
        super(s, reason);
    }
}
