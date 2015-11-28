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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MasterServer implements IServer, IZookeeperClient.IMasterServerDownListener {

    private MasterStorage storage;
    private ChunkServerService chunkServerService;
    private IZookeeperClient zkClient;
    private boolean amIShadow = true;
    final static Logger logger = Logger.getLogger(MasterServer.class);
    private Machine me;

    public MasterServer(MasterStorage storage, ChunkServerService chunkServerService,
                        IZookeeperClient zkClient, Machine me) {
        this.storage = storage;
        this.chunkServerService = chunkServerService;
        this.zkClient = zkClient;
        this.me = me;
    }

    public void start() {
        zkClient.addOnMasterServerDownListener(this);
        chunkServerService.start();
        storage.init();
        attemptToBecomeMaster();
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

    private void becomeMaster() {
        amIShadow = false;
        storage.stopReplication();
        chunkServerService.startSynchronization();
    }

    protected void becomeShadow() {
        amIShadow = true;
        storage.startReplication();
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
                case DELETE_FILE:
                    return deleteFile((DeleteFileRequestArgs) request.args);
                default:
                    throw new TjfsException("Invalid Header");
            }
        }catch (Exception e){
            throw new TjfsException(e.getMessage(), e);
        }
    }

    private Response deleteFile(DeleteFileRequestArgs args) throws TjfsException {
        if (listFile(args.path).length > 1)
            throw new TjfsException("Cannot delete directory");
        storage.deleteFile(args.path);
        return Response.Success();
    }

    private String[] listFile(Path path){
        String prefix = path.toString();
        if (!prefix.endsWith("/")) {
            prefix += "/"; // normalize
        }
        Set<String> result = new HashSet<>();

        for (Path key : storage.getFileSystem().keySet()) {
            String s = key.toString();
            if (s.startsWith(prefix)) {
                s = s.replaceFirst("^" + prefix, "");
                if (!s.contains("/")) {
                    result.add(s);
                } else {
                    result.add(s.replaceAll("/.*$", "/"));
                }
            }
        }
        return result.toArray(new String[result.size()]);
    }

    private Response listFile(ListFileRequestArgs args) {
        return Response.Success(new ListFileResponseArgs(listFile(args.path)));
    }

    private Response allocateChunks(AllocateChunksRequestArgs args){
        Set<String> chunkNames = ChunkNameGenerator.generate(args.number);
        ArrayList<ChunkDescriptor> result = new ArrayList<>();
        for (String chunkName : chunkNames){
            result.add(new ChunkDescriptor(chunkName, this.chunkServerService.getRandomChunkServers(2)));
        }
        return Response.Success(new AllocateChunkResponseArgs(result));
    }

    private Response getFile(GetFileRequestArgs args) throws TjfsException {
        if (listFile(args.path).length > 0)
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
        if(listFile(args.file.path).length > 0){
            //This means empty directory would be turned into a file-->But there should not be an empty directory
            throw new TjfsException("A directory with the same name already exists");
        }
        storage.putFile(args.file.path, args.file);
        chunkServerService.addChunks(args.file.chunks);
        return Response.Success();
    }

    private Response getLog(GetLogRequestArgs args){
        List<FileDescriptor> result = storage.getLog(args.logID);
        return Response.Success(new GetLogResponseArgs(result));
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
                storage, localFsClient, masterClient, config.getMasterReplicationIntervalTime());
        Machine me = new Machine(IpDetect.getLocalIp(zookeeper.ip), port);
        return new MasterServer(masterStorage, chunkServerService, zClient, me);
    }
}
