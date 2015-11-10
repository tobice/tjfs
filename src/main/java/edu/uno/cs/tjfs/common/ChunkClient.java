package edu.uno.cs.tjfs.common;

import edu.uno.cs.tjfs.common.messages.*;

import java.io.InputStream;

import edu.uno.cs.tjfs.common.messages.arguments.*;

public class ChunkClient implements IChunkClient {
    public InputStream get(Machine machine, String chunkName) throws TjfsException {
        Request request = new Request(MCommand.GET_CHUNK, new GetChunkRequestArgs(chunkName), null, 0);
        MessageClient client = new MessageClient();
        Response response = client.send(machine, request);
        if (response.code == MCode.ERROR){
            throw new TjfsException(((GetChunkResponseArgs)response.args).status);
        }
        else{
            return response.data;
        }
    }

    @Override
    public InputStream get(ChunkDescriptor chunkDescriptor) throws TjfsException {
        return null;
    }

    public void put(Machine machine, String chunkName, int dataLength, InputStream data) throws TjfsException{
        Request request = new Request(MCommand.PUT_CHUNK, new PutChunkRequestArgs(chunkName), data, dataLength);
        MessageClient client = new MessageClient();
        Response response = client.send(machine, request);
        if (response.code == MCode.ERROR){
            throw new TjfsException(((PutChunkResponseArgs)response.args).status);
        }
    }

    @Override
    public void put(ChunkDescriptor chunkDescriptor, int length, InputStream data) throws TjfsException {

    }

    public void delete(Machine machine, String chunkName)  throws TjfsException{
        Request request = new Request(MCommand.DELETE_CHUNK, new DeleteChunkRequestArgs(chunkName), null, 0);
        MessageClient client = new MessageClient();
        Response response = client.send(machine, request);
        if(response.code == MCode.ERROR){
            throw new TjfsException(((DeleteChunkResponseArgs)response.args).status);
        }
    }

    public String[] list(Machine machine) throws TjfsException{
        Request request = new Request(MCommand.LIST_CHUNK, new ListChunkRequestArgs(), null, 0);
        MessageClient client = new MessageClient();
        Response response = client.send(machine, request);
        if(response.code == MCode.ERROR){
            throw new TjfsException(((ListChunkResponseArgs)response.args).status);
        }
        else {
            return ((ListChunkResponseArgs)response.args).chunks;
        }
    }
}
