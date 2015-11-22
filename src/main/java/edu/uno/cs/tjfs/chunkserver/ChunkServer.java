package edu.uno.cs.tjfs.chunkserver;

import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.messages.*;
import edu.uno.cs.tjfs.common.messages.arguments.*;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ChunkServer implements IServer {
    private ILocalFsClient localFsClient;
    private IMasterClient masterClient;//TODO: Not sure if this need masterclient
    private IChunkClient chunkClient;
    private Path fileSystem;

    public ChunkServer(ILocalFsClient localFsClient, IMasterClient masterClient, IChunkClient chunkClient,
                       Path fileSystem){
        this.localFsClient = localFsClient;
        this.masterClient = masterClient;
        this.chunkClient = chunkClient;
        this.fileSystem = fileSystem;
    }

    public Response process(Request request) throws TjfsException{
        if (request == null)
            throw new TjfsException("Empty Request error.");
        Response response = null;
        try {
            switch (request.header) {
                case GET_CHUNK:
                    return processGetChunk((GetChunkRequestArgs)request.args);
                case PUT_CHUNK:
                    return processPutChunk((PutChunkRequestArgs)request.args, request.data, request.dataLength);
                case LIST_CHUNK:
                    return processListChunk();
                case DELETE_CHUNK:
                    return processDeleteChunk((DeleteChunkRequestArgs) request.args);
                case REPLICATE_CHUNK:
                    return processReplicateChunk((ReplicateChunkRequestArgs) request.args);
            }
        }catch (Exception e){
            throw new TjfsException(e.getMessage(), e);
        }
        return response;
    }

    private Response processGetChunk(GetChunkRequestArgs args) throws IOException {
        byte[] data = this.localFsClient.readBytesFromFile(getChunkPath(args.chunkName));
        return Response.Success(data);
    }

    private Response processPutChunk(PutChunkRequestArgs args, InputStream data, int dataLength) throws IOException {
        this.localFsClient.writeBytesToFile(getChunkPath(args.chunkName), IOUtils.toByteArray(data, dataLength));
        return Response.Success();
    }

    private Response processListChunk() {
        String[] listOfFiles = this.localFsClient.listFiles(this.fileSystem);
        return new Response(MCode.SUCCESS, new ListChunkResponseArgs(listOfFiles), null, 0);
    }

    private Response processDeleteChunk(DeleteChunkRequestArgs args) throws IOException {
        this.localFsClient.deleteFile(getChunkPath(args.chunkName));
        return Response.Success();
    }

    private Response processReplicateChunk(ReplicateChunkRequestArgs args) throws IOException, TjfsException {
        byte[] data = this.localFsClient.readBytesFromFile(getChunkPath(args.chunkName));
        this.chunkClient.putAsync(args.machine, args.chunkName, data.length, new ByteArrayInputStream(data));
        return Response.Success();
    }

    private Path getChunkPath(String chunkName){
        return Paths.get(fileSystem.toString() + "/" + chunkName);
    }
}
