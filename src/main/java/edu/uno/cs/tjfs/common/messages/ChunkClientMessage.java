package edu.uno.cs.tjfs.common.messages;

import java.io.InputStream;

/**
 * Created by srjanak on 11/5/15.
 */
public class ChunkClientMessage implements IJsonMessage {
    public String status;

    public ChunkClientMessage(String status){
        this.status = status;
    }
}
