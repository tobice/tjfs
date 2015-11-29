package edu.uno.cs.tjfs.master;

import edu.uno.cs.tjfs.Config;
import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.messages.MessageClient;
import edu.uno.cs.tjfs.common.messages.Request;
import edu.uno.cs.tjfs.common.messages.Response;
import edu.uno.cs.tjfs.common.messages.arguments.*;
import edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient;
import edu.uno.cs.tjfs.common.zookeeper.ZookeeperClient;
import edu.uno.cs.tjfs.common.zookeeper.ZookeeperException;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MasterServer implements IServer, IZookeeperClient.IMasterServerDownListener {
    final static Logger logger = Logger.getLogger(MasterServer.class);

    private MasterStorage storage;
    private ChunkServerService chunkServerService;
    private IZookeeperClient zkClient;
    protected boolean amIShadow = true;
    private Machine me;

    public MasterServer(MasterStorage storage, ChunkServerService chunkServerService,
                        IZookeeperClient zkClient, Machine me) {
        this.storage = storage;
        this.chunkServerService = chunkServerService;
        this.zkClient = zkClient;
        this.me = me;
    }

    public void start() throws TjfsException {
        try {
            zkClient.addOnMasterServerDownListener(this);
            chunkServerService.start();
            storage.init();
            attemptToBecomeMaster();
        } catch (TjfsException e) {
            throw new TjfsException("Failed to init the storage. " + e.getMessage(), e);
        }
    }

    private boolean amIShadow() {
        return amIShadow;
    }

    private void attemptToBecomeMaster() {
        try {
            this.zkClient.registerMasterServer(me);
            becomeMaster();
        } catch (ZookeeperException.MasterAlreadyExistsException e) {
            becomeShadow();
        } catch (ZookeeperException e){
            logger.error(e.getMessage(), e);
        }
    }

    protected void becomeMaster() {
        amIShadow = false;
        storage.stopReplication();
        storage.startSnapshotting();
        chunkServerService.startSynchronization();
    }

    protected void becomeShadow() {
        amIShadow = true;
        storage.stopSnapshotting();
        storage.startReplication(); // the replication will take care of snapshotting as well
        chunkServerService.stopSynchronization();
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
                case LIST_FILE:
                    return listFile((ListFileRequestArgs) request.args);
                case GET_LATEST_SNAPSHOT:
                    return getLatestSnapshot();
                default:
                    throw new TjfsException("Invalid Header");
            }
        } catch (Exception e){
            throw new TjfsException(e.getMessage(), e);
        }
    }

    protected Response listFile(ListFileRequestArgs args) {
        return Response.Success(new ListFileResponseArgs(storage.list(args.path)));
    }

    protected Response allocateChunks(AllocateChunksRequestArgs args){
        Set<String> chunkNames = ChunkNameGenerator.generate(args.number);
        ArrayList<ChunkDescriptor> result = new ArrayList<>();
        for (String chunkName : chunkNames){
            result.add(new ChunkDescriptor(chunkName, this.chunkServerService.getRandomChunkServers(2)));
        }
        return Response.Success(new AllocateChunkResponseArgs(result));
    }

    protected Response getFile(GetFileRequestArgs args) throws TjfsException {
        if (storage.list(args.path).length > 0)
            throw new TjfsException("Cannot get a directory");
        FileDescriptor fileDescriptor = this.storage.getFile(args.path);
        fileDescriptor = fileDescriptor == null ? new FileDescriptor(args.path) : fileDescriptor;
        try{
            fileDescriptor = chunkServerService.updateChunkServers(fileDescriptor);
        }catch(TjfsException e){
            logger.error("Updating the chunk server failed - " , e);
            //do nothing
        }
        return Response.Success(new GetFileResponseArgs(fileDescriptor));
    }

    protected Response putFile(PutFileRequestArgs args) throws TjfsException {
        if (args.file.path.toString().isEmpty())
            throw new TjfsException("Empty file name");
        if(storage.list(args.file.path).length > 0){
            //This means empty directory would be turned into a file-->But there should not be an empty directory
            throw new TjfsException("A directory with the same name already exists");
        }
        storage.putFile(args.file);
        chunkServerService.addChunks(args.file.chunks);
        return Response.Success();
    }

    protected Response getLog(GetLogRequestArgs args) {
        List<IMasterStorage.LogItem> log = storage.getLog(args.lastVersion);
        return Response.Success(new GetLogResponseArgs(log));
    }

    protected Response getLatestSnapshot() throws TjfsException {
        IMasterStorage.Snapshot snapshot = storage.getLastSnapshot();
        return Response.Success(new GetLatestSnapshotsResponseArgs(snapshot));
    }

    @Override
    public void onMasterServerDown() {
        if (amIShadow()) {
            attemptToBecomeMaster();
        } else {
            logger.error("Listening to its own down event.");
        }
    }

    public static MasterServer getInstance(Machine zookeeper, Config config, int port, Path storage) throws TjfsException {
        LocalFsClient localFsClient = new LocalFsClient();
        MessageClient messageClient = new MessageClient();
        ChunkClient chunkClient = new ChunkClient(messageClient);
        IZookeeperClient zClient = ZookeeperClient.connect(zookeeper, config.getZookeeperSessionTimeout());
        ChunkServerService chunkServerService = new ChunkServerService(zClient, chunkClient);
        MasterClient masterClient = new MasterClient(messageClient, zClient);
        MasterStorage masterStorage = new MasterStorage(
                storage, localFsClient, masterClient,
                config.getMasterReplicationIntervalTime(), config.getMasterSnapshottingIntervalTime());
        Machine me = new Machine(IpDetect.getLocalIp(zookeeper.ip), port);
        return new MasterServer(masterStorage, chunkServerService, zClient, me);
    }
}
