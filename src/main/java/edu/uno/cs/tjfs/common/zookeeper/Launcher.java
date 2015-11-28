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
        zkClient.acquireFileLock(Paths.get("/ubuntu.iso"), READ);
        */

        Listener listener = new Listener();
        zkClient.addOnChunkServerDownListener(listener);
        zkClient.addOnChunkServerUpListener(listener);
        zkClient.addOnMasterServerDownListener(listener);
        zkClient.addOnMasterServerUpListener(listener);
        zkClient.addOnConnectionLostListener(listener);

        int i = 1;
        while (true) {
            Thread.sleep(1000);
            try {
                zkClient.registerMasterServer(Machine.fromString("127.0.0.1:" + i++));
            } catch (ZookeeperException.MasterAlreadyExistsException e) {

            }
        }
    }

    static class Listener implements IZookeeperClient.IChunkServerUpListener, IZookeeperClient
            .IChunkServerDownListener, IZookeeperClient.IMasterServerUpListener, IZookeeperClient
            .IMasterServerDownListener, IZookeeperClient.IConnectionLostListener {

        @Override
        public void onChunkServerDown(Machine machine) {
            System.out.println("chunk server down " + machine);
        }

        @Override
        public void onChunkServerUp(Machine machine) {
            System.out.println("chunk server up " + machine);
        }

        @Override
        public void onMasterServerDown() {
            System.out.println("master down");
        }

        @Override
        public void onMasterServerUp(Machine machine) {
            System.out.println("master up " + machine);

        }

        @Override
        public void onConnectionLost() {
            System.out.println("connection lost!");
        }
    }
}

