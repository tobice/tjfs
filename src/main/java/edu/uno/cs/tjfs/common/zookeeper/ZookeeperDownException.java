package edu.uno.cs.tjfs.common.zookeeper;

import edu.uno.cs.tjfs.common.TjfsException;

public class ZookeeperDownException extends TjfsException {
    public ZookeeperDownException(String s) {
        super(s);
    }

    public ZookeeperDownException(String s, Throwable reason) {
        super(s, reason);
    }
}
