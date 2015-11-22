package edu.uno.cs.tjfs.master;

import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.messages.Request;
import edu.uno.cs.tjfs.common.messages.Response;
import edu.uno.cs.tjfs.common.messages.arguments.*;
import edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient;
import edu.uno.cs.tjfs.common.zookeeper.ZnodeTakenException;
import edu.uno.cs.tjfs.common.zookeeper.ZookeeperDownException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

public class MasterServer implements IServer, IZookeeperClient.IMasterServerDownListener {
    private MasterStorage storage;
    private ChunkServerService chunkServerService;
    private IZookeeperClient zkClient;

    private boolean amIShadow = true;

    public MasterServer(MasterStorage storage, ChunkServerService chunkServerService,
                        IZookeeperClient zkClient) {
        this.storage = storage;
        this.chunkServerService = chunkServerService;
        this.zkClient = zkClient;
    }

    public void start() {
        zkClient.addOnMasterServerDownListener(this);
        storage.init();
        attemptToBecomeMaster();
    }

    private boolean amIShadow() {
        return amIShadow;
    }

    private void attemptToBecomeMaster() {
        try {
            Machine me = null; // TODO
            this.zkClient.registerMasterServer(me);
            becomeMaster();
        } catch (ZnodeTakenException e) {
            becomeShadow();
        } catch (ZookeeperDownException e) {
            // TODO: totally fail
        }
    }

    private void becomeMaster() {
        amIShadow = false;
        storage.stopReplication();
    }

    private void becomeShadow() {
        amIShadow = true;
        storage.startReplication();
    }

    @Override
    public Response process(Request request) throws TjfsException {
        if (amIShadow()) {
            throw new TjfsException("I'm not the real master");
        }
        if (request == null)
            throw new TjfsException("Empty Request error.");
        try {
            switch (request.header) {
                case ALLOCATE_CHUNKS:
                    return allocateChunks((AllocateChunksRequestArgs) request.args);
                case GET_FILE:
                    return getFile((GetFileRequestArgs) request.args);
                case PUT_FILE:
                    return putFile((PutFileRequestArgs) request.args);
                default:
                    throw new TjfsException("Invalid Header");
            }
        }catch (Exception e){
            throw new TjfsException(e.getMessage(), e);
        }
    }

    private Response allocateChunks(AllocateChunksRequestArgs args){
        Set<String> chunkNames = ChunkNameGenerator.generate(args.number);
        ArrayList<ChunkDescriptor> result = new ArrayList<>();
        for (String chunkName : chunkNames){
            result.add(new ChunkDescriptor(chunkName, this.chunkServerService.getRandomChunkServers(2)));
        }
        return Response.Success(new AllocateChunkResponseArgs(result));
    }

    private Response getFile(GetFileRequestArgs args){
        FileDescriptor file = this.storage.getFile(args.path);
        return Response.Success(new GetFileResponseArgs(file));
    }

    private Response putFile(PutFileRequestArgs args) throws IOException {
        this.storage.putFile(args.file.path, args.file);
        return Response.Success();
    }

    @Override
    public void onMasterServerDown() {
        if (amIShadow()) {
            attemptToBecomeMaster();
        } else {
            // This means that you went down which is extremely weird
            // Throw Runtime exception...
        }
    }
}
