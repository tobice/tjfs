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
        Response response = null;
        for(Machine machine : chunkDescriptor.chunkServers){
            Request request = new Request(MCommand.GET_CHUNK, new GetChunkRequestArgs(chunkDescriptor.name), null, 0);
            MessageClient client = new MessageClient();
            response = client.send(machine, request);
            if (response.code == MCode.SUCCESS) {
                break;
            }
        }
        if (response == null) {
            throw new TjfsException("No response received.");
        }
        else if (response.code == MCode.ERROR){
            throw new TjfsException(((GetChunkResponseArgs)response.args).status);
        }
        return response.data;
    }

    public void put(Machine machine, String chunkName, int dataLength, InputStream data) throws TjfsException{
        Request request = new Request(MCommand.PUT_CHUNK, new PutChunkRequestArgs(chunkName), data, dataLength);
        MessageClient client = new MessageClient();
        Response response = client.send(machine, request);
        if (response.code == MCode.ERROR){
            throw new TjfsException(((PutChunkResponseArgs)response.args).status);
        }
    }

    private void replicateAsync(Machine machineFrom, Machine machineTo, String chunkName) throws TjfsException{
        Request request = new Request(MCommand.PUT_CHUNK, new ReplicateChunkRequestArgs(chunkName, machineTo), null, 0);
        MessageClient client = new MessageClient();
        client.sendAsync(machineFrom, request);
    }

    @Override
    public void put(ChunkDescriptor chunkDescriptor, int length, InputStream data) throws TjfsException {
        if (chunkDescriptor.size != 2)
            throw new TjfsException("Invalid number of chunks.");

        try{
            put(chunkDescriptor.chunkServers.get(0), chunkDescriptor.name, length, data);
            replicateAsync(chunkDescriptor.chunkServers.get(0), chunkDescriptor.chunkServers.get(1), chunkDescriptor.name);
        }catch(Exception e){
            put(chunkDescriptor.chunkServers.get(1), chunkDescriptor.name, length, data);
        }
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
