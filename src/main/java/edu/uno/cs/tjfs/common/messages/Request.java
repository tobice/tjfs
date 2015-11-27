package edu.uno.cs.tjfs.common.messages;

import edu.uno.cs.tjfs.common.messages.arguments.IMessageArgs;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;
import java.util.Arrays;

public class Request extends Message {
    public MCommand header;

    public Request(MCommand header, IMessageArgs args, byte[] data, int dataLength){
        this.header = header;
        this.args = args;
        this.data = data;
        this.dataLength = dataLength;
    }

    public Request(MCommand header, IMessageArgs args){
        this.header = header;
        this.args = args;
        this.data = null;
        this.dataLength = 0;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Request))
            return false;
        Request otherRequest = (Request) object;
        boolean sameHeaderAndDataLength =
                this.header == otherRequest.header && this.dataLength == otherRequest.dataLength;
        try {
            return Arrays.equals(data, otherRequest.data) && sameHeaderAndDataLength;
        } catch (Exception e) {
            return false;
        }
    }
}