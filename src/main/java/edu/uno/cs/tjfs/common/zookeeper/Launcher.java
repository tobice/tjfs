package edu.uno.cs.tjfs.common.zookeeper;

import edu.uno.cs.tjfs.common.Machine;

import java.nio.file.Paths;
import static edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient.LockType.*;

/** Testing executable for ZookeeperClient */
public class Launcher {
    public static void main(String[] args) throws ZookeeperException, InterruptedException {
        ZookeeperClient zkClient = ZookeeperClient.connect(new Machine("127.0.0.1", 2181), 2000);
        /*
        zkClient.registerMasterServer(new Machine("127.0.0.1", 8000));
        System.out.println(zkClient.getMasterServer());
        zkClient.registerChunkServer(new Machine("127.0.0.1", 8000));
        zkClient.registerChunkServer(new Machine("127.0.0.2", 8000));
        zkClient.getChunkServers().forEach(System.out::println);
        */

        zkClient.acquireFileLock(Paths.get("/ubuntu.iso"), READ);


        while (true) {
            Thread.sleep(1000);
        }
    }
}

