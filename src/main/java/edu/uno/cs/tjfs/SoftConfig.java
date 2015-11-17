package edu.uno.cs.tjfs;

/** ...for testing purposes with editable fields */
public class SoftConfig extends Config {

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public void setExecutorPoolSize(int executorPoolSize) {
        this.executorPoolSize = executorPoolSize;
    }

    public void setExecutorQueueSize(int executorQueueSize) {
        this.executorQueueSize = executorQueueSize;
    }

    public void setPipeBufferSize(int pipeBufferSize) {
        this.pipeBufferSize = pipeBufferSize;
    }
}
