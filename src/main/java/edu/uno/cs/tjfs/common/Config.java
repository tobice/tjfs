package edu.uno.cs.tjfs.common;

public class Config {
    public final int chunkSize;

    public Config(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Config() {
        this(8 * 1024 * 1024);
    }
}
