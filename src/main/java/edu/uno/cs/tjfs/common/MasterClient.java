package edu.uno.cs.tjfs.common;

import edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient;

public class MasterClient implements IZookeeperClient.IMasterServerDownListener, IZookeeperClient.IMasterServerUpListener {
    /** Zookeeper client instance */
    private IZookeeperClient zkClient;

    /** Currently elected master */
    private Machine masterServer;

    public MasterClient(IZookeeperClient zkClient) {
        this.zkClient = zkClient;
    }

    public void start() {
        zkClient.setOnMasterServerUpListener(this);
        zkClient.setOnMasterServerDownListener(this);
    }

    @Override
    public void onMasterServerUp(Machine machine) {
        masterServer = machine;
    }

    @Override
    public void onMasterServerDown() {
        masterServer = null;
    }
}
