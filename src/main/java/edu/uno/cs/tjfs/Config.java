package edu.uno.cs.tjfs;

public class Config {
    protected int chunkSize = 16 * 1024 * 1024;
    protected int executorPoolSize = 3;
    protected int executorQueueSize = 3;
    protected int pipeBufferSize = 3 * chunkSize;

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
}
