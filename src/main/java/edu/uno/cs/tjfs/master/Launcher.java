package edu.uno.cs.tjfs.master;

import edu.uno.cs.tjfs.Config;
import edu.uno.cs.tjfs.common.ChunkClient;
import edu.uno.cs.tjfs.common.LocalFsClient;
import edu.uno.cs.tjfs.common.MasterClient;
import edu.uno.cs.tjfs.common.messages.MessageClient;
import edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient;

public class Launcher {
    public static void main(String args[]) throws Exception{
        Config config = new Config();
        LocalFsClient localFsClient = new LocalFsClient();
        MessageClient messageClient = new MessageClient();
        ChunkClient chunkClient = new ChunkClient(messageClient);
        IZookeeperClient zClient = null;
        ChunkServerService chunkServerService = new ChunkServerService(zClient, chunkClient);
        MasterClient masterClient = new MasterClient(messageClient, zClient);

        MasterStorage masterStorage = new MasterStorage(
                config.getMasterStoragePath(), localFsClient, masterClient, config.getMasterReplicationIntervalTime());

        MasterServer masterServer = new MasterServer(masterStorage, chunkServerService, zClient);
        masterServer.start();
    }
}
