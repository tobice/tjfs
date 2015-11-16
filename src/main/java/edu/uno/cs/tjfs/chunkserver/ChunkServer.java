package edu.uno.cs.tjfs.chunkserver;

import edu.uno.cs.tjfs.client.TjfsClientException;
import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.messages.*;
import edu.uno.cs.tjfs.common.messages.arguments.*;
import org.apache.commons.io.IOUtils;

import java.awt.*;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;

public class ChunkServer implements IServer {

    private ILocalFsClient localFsClient;
    private IMasterClient masterClient;
    private IChunkClient chunkClient;

    public ChunkServer(ILocalFsClient localFsClient, IMasterClient masterClient, IChunkClient chunkClient){
        this.localFsClient = localFsClient;
        this.masterClient = masterClient;
        this.chunkClient = chunkClient;
    }

    public Response process(Request request) throws TjfsException{
        BaseLogger.info("Processing the request");
        if (request == null)
            throw new TjfsException("Empty Request error.");
        Response response = null;
        switch (request.header){
            case GET_CHUNK:
                response = processGetChunk(request);
                break;
            case PUT_CHUNK:
                response = processPutChunk(request);
            case LIST_CHUNK:
                break;
            case DELETE_CHUNK:
                break;
            case REPLICATE_CHUNK:
                response = processReplicateChunk(request);
                break;
        }
        BaseLogger.info("Ending the process and replying with a request");
        return response;
    }

    private Response processGetChunk(Request request) throws TjfsException{
        Response response;
        try {
            String args = ((GetChunkRequestArgs) request.args).chunkName;
            InputStream stream = this.localFsClient.readFile(Paths.get(args));
            int dataLength = IOUtils.toByteArray(stream).length;
            response = new Response(MCode.SUCCESS, null, stream, dataLength);
        }catch(Exception e){
            response = new Response(MCode.ERROR, (new GetChunkResponseArgs(e.getMessage())), null, 0);
        }
        return response;
    }

    private Response processPutChunk(Request request) throws TjfsException {
        Response response;
        try{
            String args = ((PutChunkRequestArgs) request.args).chunkName;
            int dataLength = request.dataLength;
            BaseLogger.info("Data length is " + dataLength);
            System.out.println(request.data);
            BaseLogger.info("Started writing to the chunk - Chunk name " + args + " of size - " + request.dataLength);
            LocalFsClient.writeFileT(Paths.get(args), request.data, request.dataLength);
            BaseLogger.info("Finished writing to the chunk - Chunk name " + args + " of size - " + request.dataLength);
            response = new Response(MCode.SUCCESS, (new PutChunkResponseArgs("")), null, 0);
        }catch(Exception e){
            response = new Response(MCode.ERROR, (new PutChunkResponseArgs(e.getMessage())), null, 0);
        }
        return response;
    }

    private Response processListChunk(Request request) {
        Response response;
        try{
            //TODO: localfilesystem has to implement this too
            response = null;
        }catch (Exception e){
            response = new Response(MCode.ERROR, (new ListChunkResponseArgs(null, e.getMessage())), null, 0);
        }
        return response;
    }

    private Response processDeleteChunk(Request request){
        Response response;
        try{
            String args = ((DeleteChunkRequestArgs) request.args).chunkName;
            //TODO: localfilesystem hasn't implemented this yet
            response = null;
        }catch(Exception e){
            response = new Response(MCode.ERROR, (new DeleteChunkResponseArgs(e.getMessage())), null, 0);
        }
        return response;
    }

    private Response processReplicateChunk(Request request){
        BaseLogger.info("Replicating the chunk.");
        Response response;
        try{
            String args = ((ReplicateChunkRequestArgs) request.args).chunkName;
            Machine machine = ((ReplicateChunkRequestArgs) request.args).machine;

            InputStream stream = this.localFsClient.readFile(Paths.get(args));
            int dataLength = IOUtils.toByteArray(stream).length;

            InputStream stream1 = this.localFsClient.readFile(Paths.get(args));

            ChunkDescriptor chunkDescriptor = new ChunkDescriptor(args,
                    new LinkedList<>(Arrays.asList(machine)), dataLength, 0);

            this.chunkClient.putAsync(machine, args , dataLength, stream1);
            response = null; //TODO: it does not matter if i return anything this socket connection would be terminated
        }catch(Exception e){
            BaseLogger.info("Error while replicating chunk : " + e.getMessage());
            response = new Response(MCode.ERROR, (new DeleteChunkResponseArgs(e.getMessage())), null, 0);
        }
        BaseLogger.info("Done Replicating the chunk.");
        return response;
    }
}
