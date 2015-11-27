package edu.uno.cs.tjfs.common;

import edu.uno.cs.tjfs.common.messages.*;

import edu.uno.cs.tjfs.common.messages.arguments.*;
import org.apache.log4j.Logger;

public class ChunkClient implements IChunkClient {
    private IMessageClient messageClient;
    final static Logger logger = BaseLogger.getLogger(ChunkClient.class);
    public ChunkClient(IMessageClient messageClient){
        this.messageClient = messageClient;
    }

    public byte[] get(Machine machine, String chunkName) throws TjfsException {
        Request request = new Request(MCommand.GET_CHUNK, new GetChunkRequestArgs(chunkName), null, 0);
        Response response = this.messageClient.send(machine, request);
        return response.data;
    }

    @Override
    public byte[] get(ChunkDescriptor chunkDescriptor) throws TjfsException {
        if (chunkDescriptor.chunkServers.size() != 2){
            throw new TjfsException("Invalid number of chunk-severs.");
        }
        byte[] result;
        try{
            result = get(chunkDescriptor.chunkServers.get(0), chunkDescriptor.name);
        } catch(Exception e){
            result = get(chunkDescriptor.chunkServers.get(1), chunkDescriptor.name);
        }
        return result;
    }

    @Override
    public void replicateAsync(Machine machineFrom, Machine machineTo, String chunkName) throws TjfsException{
        Request request = new Request(MCommand.REPLICATE_CHUNK, new ReplicateChunkRequestArgs(chunkName, machineTo), null, 0);
        this.messageClient.sendAsync(machineFrom, request);
    }

    @Override
    public void replicateSync(Machine machineFrom, Machine machineTo, String chunkName) throws TjfsException{
        Request request = new Request(MCommand.REPLICATE_CHUNK_SYNC, new ReplicateChunkRequestArgs(chunkName, machineTo), null, 0);
        this.messageClient.send(machineFrom, request);
    }

    public void put(Machine machine, String chunkName, int dataLength, byte[] data) throws TjfsException{
        Request request = new Request(MCommand.PUT_CHUNK, new PutChunkRequestArgs(chunkName), data, dataLength);
        this.messageClient.send(machine, request);
    }

    public void putAsync(Machine machine, String chunkName, int dataLength, byte[] data) throws TjfsException{
        Request request = new Request(MCommand.PUT_CHUNK, new PutChunkRequestArgs(chunkName), data, dataLength);
        this.messageClient.sendAsync(machine, request);
    }

    @Override
    public void put(ChunkDescriptor chunkDescriptor, int length, byte[] data) throws TjfsException {
        if (chunkDescriptor.chunkServers.size() != 2)
            throw new TjfsException("Invalid number of chunk-servers.");

        try{
            put(chunkDescriptor.chunkServers.get(0), chunkDescriptor.name, length, data);
            try {
                replicateAsync(chunkDescriptor.chunkServers.get(0), chunkDescriptor.chunkServers.get(1), chunkDescriptor.name);
            }catch(Exception e){
                logger.info("Error: " + e.getMessage());
            }

        }catch(Exception e){
            put(chunkDescriptor.chunkServers.get(1), chunkDescriptor.name, length, data);
        }
    }

    public void delete(Machine machine, String chunkName)  throws TjfsException{
        Request request = new Request(MCommand.DELETE_CHUNK, new DeleteChunkRequestArgs(chunkName), null, 0);
        this.messageClient.send(machine, request);
    }

    public String[] list(Machine machine) throws TjfsException{
        Request request = new Request(MCommand.LIST_CHUNK, new ListChunkRequestArgs(), null, 0);
        Response response = this.messageClient.send(machine, request);
        return ((ListChunkResponseArgs)response.args).chunks;
    }
}
