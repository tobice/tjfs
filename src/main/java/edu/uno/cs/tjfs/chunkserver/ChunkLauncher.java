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
    final static Logger logger = Logger.getLogger(ChunkLauncher.class);

    public static void main(String[] args){
        Logger.getLogger(MessageClient.class).setLevel(Level.INFO);
        Logger.getLogger(MessageServer.class).setLevel(Level.INFO);

        // Parse commands
        Machine zookeeper = Machine.fromString(args.length > 0 ? args[0] : "137.30.122.138:2181");
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 6002;
        Path storage = Paths.get(args.length > 2 ? args[2] : "./chunks");

        System.out.println("RUNNING TJFS CHUNK SERVER INSTANCE");
        System.out.println("Configured zookeeper server: " + zookeeper);
        System.out.println("Configured port: " + port);
        System.out.println("Configured local chunk storage: " + storage);

        while (true) {
            try {
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
