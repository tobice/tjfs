package edu.uno.cs.tjfs.master;

import edu.uno.cs.tjfs.Config;
import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.messages.MessageClient;
import edu.uno.cs.tjfs.common.messages.MessageServer;
import edu.uno.cs.tjfs.common.zookeeper.ZookeeperException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MasterLauncher {
    final static Logger logger = Logger.getLogger(MasterLauncher.class);

    public static void main(String args[]){
        Logger.getLogger(MessageClient.class).setLevel(Level.INFO);
        Logger.getLogger(MessageServer.class).setLevel(Level.INFO);

        // Parse commands
        Machine zookeeper = Machine.fromString(args.length > 0 ? args[0] : "137.30.122.138:2181");
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 6003;
        Path storage = Paths.get(args.length > 2 ? args[2] : "./fs");

        System.out.println("RUNNING TJFS MASTER SERVER INSTANCE");
        System.out.println("Configured zookeeper server: " + zookeeper);
        System.out.println("Configured port: " + port);
        System.out.println("Configured local fs storage: " + storage);

        while(true) {
            try {
                MasterServer masterServer = MasterServer.getInstance(zookeeper, new Config(), port, storage);
                MessageServer messageServer = new MessageServer(masterServer);
                masterServer.start();
                messageServer.start(port);
            } catch (ZookeeperException e) {
                logger.error(e.getMessage());
                break;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);

                // Ugly way to detect that some paths are probably broken
                if (e.getCause() instanceof IOException || e.getCause().getCause() instanceof IOException) {
                    break;
                }
            }
        }
    }
}
