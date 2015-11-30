package edu.uno.cs.tjfs.chunkserver;

import edu.uno.cs.tjfs.Config;
import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.messages.MessageClient;
import edu.uno.cs.tjfs.common.messages.MessageServer;
import edu.uno.cs.tjfs.common.zookeeper.ZookeeperException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ChunkLauncher {
    final static Logger logger = BaseLogger.getLogger(ChunkLauncher.class);
    public static void main(String[] args){
        Logger.getLogger(MessageClient.class).setLevel(Level.INFO);
        Logger.getLogger(MessageServer.class).setLevel(Level.INFO);

        while(true) {
            try {
                // TODO: get these arguments from the command line
                Machine zookeeper = Machine.fromString("137.30.122.138:2181");
                int port = 6002;
                Path storage = Paths.get("./chunks");

                ChunkServer chunkServer = ChunkServer.getInstance(zookeeper, new Config(), port, storage);
                MessageServer server = new MessageServer(chunkServer);

                chunkServer.start();
                server.start(6002);
            } catch (ZookeeperException e) {
                logger.error(e.getMessage());
                break;
            } catch (Exception e) {
                logger.error(e.getMessage());//if any error logs it and restarts
            }
        }
    }
}
