package edu.uno.cs.tjfs.chunkserver;

import com.google.gson.Gson;
import edu.uno.cs.tjfs.common.messages.ChunkClientMessage;
import edu.uno.cs.tjfs.common.messages.ClientChunkMessage;
import edu.uno.cs.tjfs.common.messages.SocketMessage;

/**
 * Client = 1
 * Master = 2
 * Chunk = 3
 * Created by janak on 11/6/2015.
 */
public class Server implements IServer{
    public SocketMessage process(SocketMessage message){
        SocketMessage returnMessage = null;
        switch (message.header){
            case "1300" : //Client to Chunk
                System.out.println("Processing Client to chunk Message");
                System.out.println(message.json);
                Gson gson = new Gson();
                returnMessage = gson.fromJson(message.json, ClientChunkMessage.class).process(message.data);
            case "1200" :
                System.out.println("Processing server to chunk message.");
                break;
            default :
                break;
        }
        return returnMessage;
    }
}
