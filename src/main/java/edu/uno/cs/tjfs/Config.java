package edu.uno.cs.tjfs;

import edu.uno.cs.tjfs.common.BaseLogger;
import edu.uno.cs.tjfs.common.TjfsException;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;

public class Config {
    protected int chunkSize = 16 * 1024 * 1024;
    protected int executorPoolSize = 6;
    protected int executorQueueSize = 10;
    protected int pipeBufferSize = 5 * chunkSize;
    protected int zookeeperSessionTimeout = 1000;
    protected int masterReplicationIntervalTime = 10 * 1000;
    protected int masterSnapshottingIntervalTime = 60 * 1000;

    public int getExecutorQueueSize() {
        return executorQueueSize;
    }

    public int getExecutorPoolSize() {
        return executorPoolSize;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public int getPipeBufferSize() {
        return pipeBufferSize;
    }

    public int getZookeeperSessionTimeout() {
        return zookeeperSessionTimeout;
    }

    public int getMasterReplicationIntervalTime() {
        return masterReplicationIntervalTime;
    }

    public int getMasterSnapshottingIntervalTime() {
        return masterSnapshottingIntervalTime;
    }
}
