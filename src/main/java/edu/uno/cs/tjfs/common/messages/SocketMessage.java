package edu.uno.cs.tjfs.common.messages;

import java.io.InputStream;
import java.util.Comparator;

import com.google.gson.Gson;

/**
 * Created by srjanak on 11/5/15.
 */
public class SocketMessage{
    public String header;
    public String json;
    public InputStream data;
    public int dataLength;

    public SocketMessage(String header, IJsonMessage json, int dataLength, InputStream data){
        this.header = header;
        Gson gson = new Gson();
        this.json = gson.toJson(json);
        this.data = data;
        this.dataLength = dataLength;
    }

    public SocketMessage(String header, String json, int dataLength, InputStream data){
        this.header = header;
        this.json = json;
        this.data = data;
        this.dataLength = dataLength;
    }

    public SocketMessage(String header, String json){
        this.header = header;
        this.json = json;
        this.data = null;
        this.dataLength = 0;
    }

    public SocketMessage(String header, IJsonMessage json){
        this.header = header;
        Gson gson = new Gson();
        this.json = gson.toJson(json);
        this.data = null;
        this.dataLength = 0;
    }

    public String toString(){
        String result = this.header;
        result += String.format("%010d", this.json.length());
        result += String.format("%010d", this.dataLength);
        result += this.json;
        return result;
    }

    @Override
    public boolean equals(Object obj){
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SocketMessage other = (SocketMessage) obj;
        return other.toString().equals(this.toString());
    }
}
