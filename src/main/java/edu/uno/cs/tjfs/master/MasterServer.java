package edu.uno.cs.tjfs.master;

import edu.uno.cs.tjfs.Config;
import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.messages.Request;
import edu.uno.cs.tjfs.common.messages.Response;
import edu.uno.cs.tjfs.common.messages.arguments.*;
import edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient;
import edu.uno.cs.tjfs.common.zookeeper.ZookeeperException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MasterServer implements IServer, IZookeeperClient.IMasterServerDownListener {

    private MasterStorage storage;
    private ChunkServerService chunkServerService;
    private IZookeeperClient zkClient;
    private Config config;
    private boolean amIShadow = true;

    public MasterServer(MasterStorage storage, ChunkServerService chunkServerService,
                        IZookeeperClient zkClient, Config config) {
        this.storage = storage;
        this.chunkServerService = chunkServerService;
        this.zkClient = zkClient;
        this.config = config;
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
            Machine me = new Machine(config.getCurrentIPAddress(), config.getMasterPort());
            this.zkClient.registerMasterServer(me);
            becomeMaster();
        } catch (ZookeeperException.MasterAlreadyExistsException e) {
            becomeShadow();
        } catch (ZookeeperException e) {
            //throw e; -> This requires api change
            // TODO: totally fail
        } catch(TjfsException e){
            //TODO: fail too
        }
    }

    private void becomeMaster() {
        amIShadow = false;
        storage.stopReplication();
    }

    protected void becomeShadow() {
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
                case GET_LOG:
                    return getLog((GetLogRequestArgs) request.args);
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
        FileDescriptor fileDescriptor = this.storage.getFile(args.path);
        fileDescriptor = fileDescriptor == null
                            ? new FileDescriptor(args.path)
                            : chunkServerService.updateChunkServers(fileDescriptor);
        return Response.Success(new GetFileResponseArgs(fileDescriptor));
    }

    private Response putFile(PutFileRequestArgs args) throws IOException {
        this.storage.putFile(args.file.path, args.file);
        return Response.Success();
    }

    private Response getLog(GetLogRequestArgs args) throws IOException {
        List<FileDescriptor> result = storage.getLog(args.logID);
        return Response.Success(new GetLogResponseArgs(result));
    }

    @Override
    public void onMasterServerDown() {
        if (amIShadow()) {
            attemptToBecomeMaster();
        } else {
            BaseLogger.error("MasterServer.onMasterServerDown - listening to its own down event.");
            //throw new TjfsException("Master server listens to its own going down event.");
        }
    }
}
