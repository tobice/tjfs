package edu.uno.cs.tjfs.common.messages;

public class BadRequestException extends MessageException {
    public final Request request;
    public BadRequestException(String s, Request request) {
        super(s);
        this.request = request;
    }
}
