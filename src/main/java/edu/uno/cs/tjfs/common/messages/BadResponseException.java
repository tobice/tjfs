package edu.uno.cs.tjfs.common.messages;

public class BadResponseException extends MessageException {
    public final Response response;
    public BadResponseException(String s, Response response) {
        super(s);
        this.response = response;
    }
}
