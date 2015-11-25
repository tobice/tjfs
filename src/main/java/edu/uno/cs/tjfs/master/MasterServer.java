package edu.uno.cs.tjfs.master;

import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.messages.Request;
import edu.uno.cs.tjfs.common.messages.Response;
import edu.uno.cs.tjfs.common.messages.arguments.*;
import edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient;
import edu.uno.cs.tjfs.common.zookeeper.ZnodeTakenException;
import edu.uno.cs.tjfs.common.zookeeper.ZookeeperDownException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
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
            Machine me = new Machine(getCurrentIPAddress(), 6002); // TODO: Should port be from the config
            this.zkClient.registerMasterServer(me);
            becomeMaster();
        } catch (ZnodeTakenException e) {
            becomeShadow();
        } catch (ZookeeperDownException e) {
            //throw e; -> This requires api change
            // TODO: totally fail
        } catch(TjfsException e){
            //TODO: fail too
        }
    }

    protected String getCurrentIPAddress() throws TjfsException {
        String result = "";
        try {
            Enumeration e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface n = (NetworkInterface) e.nextElement();
                Enumeration ee = n.getInetAddresses();
                while (ee.hasMoreElements()) {
                    InetAddress i = (InetAddress) ee.nextElement();
                    String hostAddress = i.getHostAddress();
                    if (!hostAddress.contains("127.0.0") && !hostAddress.contains("192.168.")
                            && !hostAddress.contains("0:0:0"))
                        result = hostAddress;
                }
            }
        }catch(Exception e){
            BaseLogger.error("MasterServer.getCurrentIPAddress - Cannot get the ip address.");
            BaseLogger.error("MasterServer.getCurrentIPAddress - ", e);
            throw new TjfsException("Cannot get master IP", e);
        }
        if (result.isEmpty()) {
            BaseLogger.error("MasterServer.getCurrentIPAddress - Cannot get the ip address.");
            throw new TjfsException("Cannot get master IP");
        }
        return result;
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
        FileDescriptor file = this.storage.getFile(args.path);
        return Response.Success(new GetFileResponseArgs(file));
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
