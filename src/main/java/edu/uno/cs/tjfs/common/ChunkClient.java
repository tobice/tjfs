package edu.uno.cs.tjfs.common;

import edu.uno.cs.tjfs.common.messages.*;

import java.io.InputStream;

import edu.uno.cs.tjfs.common.messages.arguments.*;

public class ChunkClient implements IChunkClient {
    private IMessageClient messageClient;
    public ChunkClient(IMessageClient messageClient){
        this.messageClient = messageClient;
    }

    public InputStream get(Machine machine, String chunkName) throws TjfsException {
        Request request = new Request(MCommand.GET_CHUNK, new GetChunkRequestArgs(chunkName), null, 0);
        Response response = this.messageClient.send(machine, request);
        if (response == null){
            throw new TjfsException("No response from server.");
        }
        else if (response.code == MCode.ERROR){
            throw new TjfsException(((GetChunkResponseArgs)response.args).status);
        }
        else{
            return response.data;
        }
    }

    @Override
    public InputStream get(ChunkDescriptor chunkDescriptor) throws TjfsException {
        if (chunkDescriptor.chunkServers.size() != 2){
            throw new TjfsException("Invalid number of chunk-severs.");
        }
        InputStream result;
        try{
            result = get(chunkDescriptor.chunkServers.get(0), chunkDescriptor.name);
        }catch(Exception e){
            result = get(chunkDescriptor.chunkServers.get(1), chunkDescriptor.name);
        }
        return result;
    }

    public void put(Machine machine, String chunkName, int dataLength, InputStream data) throws TjfsException{
        Request request = new Request(MCommand.PUT_CHUNK, new PutChunkRequestArgs(chunkName), data, dataLength);
        Response response = this.messageClient.send(machine, request);
        if (response.code == MCode.ERROR){
            throw new TjfsException(((PutChunkResponseArgs)response.args).status);
        }
    }

    private void replicateAsync(Machine machineFrom, Machine machineTo, String chunkName) throws TjfsException{
        Request request = new Request(MCommand.PUT_CHUNK, new ReplicateChunkRequestArgs(chunkName, machineTo), null, 0);
        this.messageClient.sendAsync(machineFrom, request);
    }

    @Override
    public void put(ChunkDescriptor chunkDescriptor, int length, InputStream data) throws TjfsException {
        if (chunkDescriptor.chunkServers.size() != 2)
            throw new TjfsException("Invalid number of chunk-servers.");

        try{
            put(chunkDescriptor.chunkServers.get(0), chunkDescriptor.name, length, data);
            replicateAsync(chunkDescriptor.chunkServers.get(0), chunkDescriptor.chunkServers.get(1), chunkDescriptor.name);
        }catch(Exception e){
            put(chunkDescriptor.chunkServers.get(1), chunkDescriptor.name, length, data);
        }
    }

    public void delete(Machine machine, String chunkName)  throws TjfsException{
        Request request = new Request(MCommand.DELETE_CHUNK, new DeleteChunkRequestArgs(chunkName), null, 0);
        Response response = this.messageClient.send(machine, request);
        if(response.code == MCode.ERROR){
            throw new TjfsException(((DeleteChunkResponseArgs)response.args).status);
        }
    }

    public String[] list(Machine machine) throws TjfsException{
        Request request = new Request(MCommand.LIST_CHUNK, new ListChunkRequestArgs(), null, 0);
        Response response = this.messageClient.send(machine, request);
        if(response.code == MCode.ERROR){
            throw new TjfsException(((ListChunkResponseArgs)response.args).status);
        }
        else {
            return ((ListChunkResponseArgs)response.args).chunks;
        }
    }
}