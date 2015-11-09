package edu.uno.cs.tjfs.client;

import edu.uno.cs.tjfs.common.Machine;

import java.util.List;

public class AllocatedChunkName {
    public final String name;
    public final List<Machine> targetChunkServers;

    public AllocatedChunkName(String name, List<Machine> targetChunkServers) {
        this.name = name;
        this.targetChunkServers = targetChunkServers;
    }
}
