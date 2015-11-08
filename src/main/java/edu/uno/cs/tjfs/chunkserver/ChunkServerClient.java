package edu.uno.cs.tjfs.chunkserver;

import com.google.gson.Gson;
import edu.uno.cs.tjfs.common.Machine;
import edu.uno.cs.tjfs.common.TjfsException;
import edu.uno.cs.tjfs.common.messages.ChunkClientMessage;
import edu.uno.cs.tjfs.common.messages.ClientChunkMessage;
import edu.uno.cs.tjfs.common.messages.IMessageServer;
import edu.uno.cs.tjfs.common.messages.SocketMessage;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by srjanak on 11/5/15.
 */
public class ChunkServerClient implements IChunkClient {
    private IMessageServer messageLayer;

    public ChunkServerClient(IMessageServer messageLayer){
        this.messageLayer = messageLayer;
    }


    public InputStream get(String chunkName) throws IOException{
        ClientChunkMessage clientChunkMessage = new ClientChunkMessage(
                ClientChunkMessage.ClientChunkMessageType.GET, chunkName);

        SocketMessage outGoingMessage = new SocketMessage(
                "1300", clientChunkMessage);

        SocketMessage msgReceived = this.messageLayer.send(outGoingMessage);

        if (msgReceived == null)
            throw new IOException("No message from chunk server");

        Gson gson = new Gson();
        ChunkClientMessage messageFromServer = gson.fromJson(msgReceived.json, ChunkClientMessage.class);
        if (!messageFromServer.status.equals("")){
            throw new IOException("Cannot put chunk to the server");
        }
        return msgReceived.data;
    }

    public void put(String chunkName, int dataLength, InputStream chunkInputStream) throws TjfsException, IOException{
        ClientChunkMessage clientChunkMessage = new ClientChunkMessage(
                ClientChunkMessage.ClientChunkMessageType.PUT, chunkName);

        SocketMessage outGoingMessage = new SocketMessage(
                "1300", clientChunkMessage, dataLength, chunkInputStream);

        SocketMessage msgReceived = this.messageLayer.send(outGoingMessage);

        if (msgReceived == null)
            throw new TjfsException("No message from chunk server");

        Gson gson = new Gson();
        ChunkClientMessage messageFromServer = gson.fromJson(msgReceived.json, ChunkClientMessage.class);
        if (!messageFromServer.status.equals("")){
            throw new TjfsException("Cannot put chunk to the server");
        }
    }

    public void delete(String chunkName){
        // TODO: should delete the chunk ? or may be not because the Master would just remove that chunk mapping
    }

    public String[] list(){
        // TODO: this would be implemented
        return null;
    }

    @Override
    public InputStream getChunk(Machine machine, String name) throws TjfsException {
        return null;
    }

    @Override
    public void putChunk(Machine machine, String name, int length, InputStream data) throws TjfsException {

    }

    @Override
    public void deleteChunk(Machine machine, String name) throws TjfsException {

    }

    @Override
    public String[] list(Machine machine) throws TjfsException {
        return new String[0];
    }
}
