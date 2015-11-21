package edu.uno.cs.tjfs.master;

import edu.uno.cs.tjfs.chunkserver.IServer;
import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.messages.MCode;
import edu.uno.cs.tjfs.common.messages.Request;
import edu.uno.cs.tjfs.common.messages.Response;
import edu.uno.cs.tjfs.common.messages.arguments.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MasterServer implements IServer{
    private MasterStorage storage;
    public MasterServer(MasterStorage storage){
        this.storage = storage;
    }
    @Override
    public Response process(Request request) throws TjfsException {
        return null;
    }

    public Response process(Request request, int threadID) throws TjfsException {
        BaseLogger.info("Processing the request as master.");
        if (request == null)
            throw new TjfsException("Empty Request error.");
        Response response;
        switch (request.header){
            case ALLOCATE_CHUNKS:
                response = allocateChunks(request, threadID);
                break;
            case GET_FILE:
                response = getFile(request);
                break;
            case PUT_FILE:
                response = putFile(request);
                break;
            default:
                throw new TjfsException("Invalid Header");
        }
        BaseLogger.info("Ending the process and replying with a request");
        return response;
    }

    private String getChunkName(int threadID){
        String newChunkName = "" + threadID;
        newChunkName += System.currentTimeMillis();
        Random r = new Random();
        newChunkName += r.nextLong();
        return newChunkName;
    }

    private Machine getRandomChunkServer(List<Machine> chunkServers){
        Random r = new Random();
        return chunkServers.get(r.nextInt(chunkServers.size()));
    }

    private List<Machine> getTwoRandomChunkServers(){
        //TODO: get list of chunkservers
        Machine machine = new Machine("192.168.43.27", 6002);
        Machine machine2 = new Machine("192.168.43.218", 6002);
        ArrayList<Machine> machines = new ArrayList<>();
        machines.add(machine);
        machines.add(machine2);

        Machine chunkServer1 = getRandomChunkServer(machines);
        Machine chunkServer2 = getRandomChunkServer(machines);

        ArrayList<Machine> chunkServers = new ArrayList<>();
        chunkServers.add(chunkServer1);
        chunkServers.add(chunkServer2);

        return chunkServers;
    }

    private Response allocateChunks(Request request, int threadID){
        BaseLogger.trace("MasterServer.allocateChunks - started to allocate the chunks");
        Response response;
        try {
            int args = ((AllocateChunksRequestArgs) request.args).number;
            ArrayList<ChunkDescriptor> result = new ArrayList<>();
            for(int counter = 0; counter < args; counter++){
                String chunkName = getChunkName(threadID);
                List<Machine> chunkServers = getTwoRandomChunkServers();
                ChunkDescriptor chunkDescriptor = new ChunkDescriptor(chunkName, chunkServers);
                result.add(chunkDescriptor);
            }
            this.storage.allocateChunks(result);
            response = new Response(MCode.SUCCESS, (new AllocateChunkResponseArgs(result, "")), null, 0);
        }catch(Exception e){
            response = new Response(MCode.ERROR, (new GetChunkResponseArgs(e.getMessage())), null, 0);
        }
        return response;
    }

    private Response getFile(Request request){
        BaseLogger.trace("MasterServer.getFile - started to get the file");
        Response response;
        try {
            Path args = ((GetFileRequestArgs) request.args).path;
            FileDescriptor file = this.storage.getFile(args);
            response = new Response(MCode.SUCCESS, (new GetFileResponseArgs(file, "")), null, 0);
        }catch(Exception e){
            response = new Response(MCode.ERROR, (new GetChunkResponseArgs(e.getMessage())), null, 0);
        }
        return response;
    }

    private Response putFile(Request request){
        BaseLogger.trace("MasterServer.putFile - started to put the file");
        Response response;
        try {
            FileDescriptor args = ((PutFileRequestArgs) request.args).file;
            this.storage.putFile(args.path, args);
            response = new Response(MCode.SUCCESS, (new PutFileResponseArgs("")), null, 0);
        }catch(Exception e){
            response = new Response(MCode.ERROR, (new GetChunkResponseArgs(e.getMessage())), null, 0);
        }
        return response;
    }
}
