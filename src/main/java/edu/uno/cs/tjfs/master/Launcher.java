package edu.uno.cs.tjfs.master;

import edu.uno.cs.tjfs.Config;
import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.messages.MessageClient;
import edu.uno.cs.tjfs.common.messages.MessageServer;
import edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Launcher {
    final static Logger logger = BaseLogger.getLogger(Launcher.class);
    public static void main(String args[]){
        while(true) {
            //TODO: get these from command line
            Machine zookeeper = Machine.fromString("137.30.122.138:2181");
            int port = 6003;
            Path storage = Paths.get("./fs");

            try {
                MasterServer masterServer = MasterServer.getInstance(zookeeper, new Config(), port, storage);
                MessageServer messageServer = new MessageServer(masterServer);
                masterServer.start();
                messageServer.start(port);
            } catch (TjfsException e) {
                logger.error(e.getMessage(), e);
                break;
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
