package edu.uno.cs.tjfs.chunkserver;

import edu.uno.cs.tjfs.Config;
import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.messages.MessageServer;
import edu.uno.cs.tjfs.common.zookeeper.ZookeeperException;
import org.apache.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Launcher {
    final static Logger logger = BaseLogger.getLogger(Launcher.class);
    public static void main(String[] args){
        while(true) {
            try {
                // TODO: get these arguments from the command line
                Machine zookeeper = Machine.fromString("127.0.0.1:2181");
                int port = 6002;
                Path storage = Paths.get("./chunks");

                ChunkServer chunkServer = ChunkServer.getInstance(zookeeper, new Config(), port, storage);
                MessageServer server = new MessageServer(chunkServer);

                chunkServer.start();
                server.start(6002);
            } catch (ZookeeperException e) {
                logger.error(e.getMessage());//if any error logs it and restarts
                break;
            } catch (Exception e) {
                logger.error(e.getMessage());//if any error logs it and restarts
            }
        }
    }
}
