package edu.uno.cs.tjfs.common.messages;

import java.io.OutputStream;

abstract public class Message<T> {
    int jsonLength;
    int dataLength;

    T json;
    OutputStream data;
}
