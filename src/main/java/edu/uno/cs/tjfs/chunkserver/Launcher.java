package edu.uno.cs.tjfs.chunkserver;

import edu.uno.cs.tjfs.common.BaseLogger;
import edu.uno.cs.tjfs.common.ChunkClient;
import edu.uno.cs.tjfs.common.DummyMasterClient;
import edu.uno.cs.tjfs.common.LocalFsClient;
import edu.uno.cs.tjfs.common.messages.MessageClient;

import java.nio.file.Paths;

public class Launcher {
    public static void main(String[] args){
        while(true) {
            try {
                DummyMasterClient masterClient = new DummyMasterClient();
                ChunkClient chunkClient = new ChunkClient(new MessageClient());
                LocalFsClient localFsClient = new LocalFsClient();

                ChunkServer chunkServer = new ChunkServer(localFsClient, masterClient, chunkClient, Paths.get("/home/srjanak/chunks"));

                MessageServer server = new MessageServer(chunkServer);

                server.start(6002);//waits here until any error
            } catch (Exception e) {
                BaseLogger.error(e.getMessage());//if any error logs it and restarts
            }
        }
    }
}
