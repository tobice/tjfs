package edu.uno.cs.tjfs.chunkserver;

import edu.uno.cs.tjfs.Config;
import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.messages.*;
import edu.uno.cs.tjfs.common.messages.arguments.*;
import edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient;
import edu.uno.cs.tjfs.common.zookeeper.ZookeeperClient;
import edu.uno.cs.tjfs.common.zookeeper.ZookeeperException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ChunkServer implements IServer {
    private ILocalFsClient localFsClient;
    private IChunkClient chunkClient;
    private IZookeeperClient zkClient;

    /** Location of where all the chunks are physically stored */
    private Path fileSystem;

    /** Identification of this chunk server (IP + port) */
    private Machine me;

    public ChunkServer(ILocalFsClient localFsClient, IChunkClient chunkClient,
           ZookeeperClient zkClient, Path fileSystem, Machine me) {
        this.localFsClient = localFsClient;
        this.chunkClient = chunkClient;
        this.fileSystem = fileSystem;
        this.zkClient = zkClient;
        this.me = me;
    }

    public void start() throws ZookeeperException {
        zkClient.registerChunkServer(me);
    }

    public Response process(Request request) throws TjfsException{
        if (request == null) {
            throw new TjfsException("Empty Request error.");
        }

        try {
            switch (request.header) {
                case GET_CHUNK:
                    return processGetChunk((GetChunkRequestArgs)request.args);
                case PUT_CHUNK:
                    return processPutChunk((PutChunkRequestArgs)request.args, request.data);
                case LIST_CHUNK:
                    return processListChunk();
                case DELETE_CHUNK:
                    return processDeleteChunk((DeleteChunkRequestArgs) request.args);
                case REPLICATE_CHUNK:
                    return processReplicateChunk((ReplicateChunkRequestArgs) request.args);
                default:
                    throw new TjfsException("Unsupported method");
            }
        } catch (TjfsException|IOException e){
            throw new TjfsException(e.getMessage(), e);
        }
    }

    private Response processGetChunk(GetChunkRequestArgs args) throws IOException {
        byte[] data = this.localFsClient.readBytesFromFile(getChunkPath(args.chunkName));
        return Response.Success(data);
    }

    private Response processPutChunk(PutChunkRequestArgs args, byte[] data) throws IOException {
        this.localFsClient.writeBytesToFile(getChunkPath(args.chunkName), data);
        return Response.Success();
    }

    private Response processListChunk() {
        String[] listOfFiles = this.localFsClient.list(this.fileSystem);
        return new Response(MCode.SUCCESS, new ListChunkResponseArgs(listOfFiles));
    }

    private Response processDeleteChunk(DeleteChunkRequestArgs args) throws IOException {
        this.localFsClient.deleteFile(getChunkPath(args.chunkName));
        return Response.Success();
    }

    private Response processReplicateChunk(ReplicateChunkRequestArgs args) throws IOException, TjfsException {
        byte[] data = this.localFsClient.readBytesFromFile(getChunkPath(args.chunkName));
        this.chunkClient.put(args.machine, args.chunkName, data);
        return Response.Success();
    }

    private Path getChunkPath(String chunkName){
        return Paths.get(fileSystem.toString() + "/" + chunkName);
    }

    public static ChunkServer getInstance(Machine zookeeper, Config config, int port, Path fileSystem) throws TjfsException {
        ZookeeperClient zkClient = ZookeeperClient.connect(zookeeper, config.getZookeeperSessionTimeout());
        MessageClient messageClient = new MessageClient();
        ChunkClient chunkClient = new ChunkClient(messageClient);
        LocalFsClient localFsClient = new LocalFsClient();
        Machine me = new Machine(IpDetect.getLocalIp(zookeeper.ip), port);
        return new ChunkServer(localFsClient, chunkClient, zkClient, fileSystem, me);
    }
}
