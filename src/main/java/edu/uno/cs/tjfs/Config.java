package edu.uno.cs.tjfs;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {
    protected int chunkSize = 16 * 1024 * 1024;
    protected int executorPoolSize = 3;
    protected int executorQueueSize = 3;
    protected int pipeBufferSize = 3 * chunkSize;
    protected int zookeeperSessionTimeout = 1000;
    protected int masterReplicationIntervalTime = 10000;

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

    public Path getMasterStoragePath(){
        return Paths.get("fs/MasterFS");
    }
}
