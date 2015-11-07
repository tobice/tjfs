package edu.uno.cs.tjfs.common.messages;

import edu.uno.cs.tjfs.common.LocalFsClient;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by srjanak on 11/5/15.
 */
public class ClientChunkMessage implements  IJsonMessage {
    public enum ClientChunkMessageType {
        PUT,
        GET
    }

    public String chunkName;
    public ClientChunkMessageType type;

    public ClientChunkMessage (ClientChunkMessageType type, String chunkName){
        this.type = type;
        this.chunkName = chunkName;
    }

    public SocketMessage process(InputStream data){
        String result = "";
        InputStream dataResult = null;
        int dataLength = 0;
        switch (this.type) {
            case PUT :
                try {
                    LocalFsClient localFsClient = new LocalFsClient();
                    localFsClient.writeFile(Paths.get(this.chunkName), data);
                }catch(Exception e){
                    result = e.getMessage();
                }
                break;
            case GET :
                    try{
                        LocalFsClient localFsClient = new LocalFsClient();
                        dataResult = localFsClient.readFile(Paths.get(this.chunkName));
                        dataLength = 120; //How do I do this???
                    }catch(Exception e){
                        result = e.getMessage();
                    }
                break;
            default:
                break;
        }
        return new SocketMessage("3100", new ChunkClientMessage(result), dataLength, dataResult);
    }
}
