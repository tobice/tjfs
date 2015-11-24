package edu.uno.cs.tjfs.common.zookeeper;

import edu.uno.cs.tjfs.common.TjfsException;
import org.apache.zookeeper.KeeperException;

/** General Zookeeper exception */
public class ZookeeperException extends TjfsException {
    /** Path of the ZNode that might be relevant to the exceptoin */
    protected String path;
    public ZookeeperException(String s) {
        super(s);
    }

    public ZookeeperException(String s, Throwable reason) {
        super(s, reason);
    }

    public String getPath() {
        return this.path;
    }

    public static ZookeeperException create(Exception e) {
        if (e instanceof KeeperException) {
            KeeperException ke = (KeeperException) e;
            switch (ke.code()) {
                case CONNECTIONLOSS:
                    return new ConnectionLostException(e);
                case NONODE:
                    return new NodeNotFoundException(ke.getPath(), e);
                case NODEEXISTS:
                    return new NodeExistsException(ke.getPath(), e);
                default:
                    return new ZookeeperException(e.getMessage(), e);
            }
        } else if (e instanceof java.lang.InterruptedException) {
            return new InterruptedException(e);
        } else {
            return new ZookeeperException(e.getMessage(), e);
        }
    }

    public static class ConnectionLostException extends ZookeeperException {
        public ConnectionLostException(Throwable reason) {
            super("Unable to connect to Zookeeper", reason);
        }
    }

    public static class NoMasterRegisteredException extends ZookeeperException {
        public NoMasterRegisteredException() {
            super("There is no server registered with Zookeeper as master");
        }
    }

    public static class MasterAlreadyExistsException extends ZookeeperException {
        public MasterAlreadyExistsException(Throwable reason) {
            super("There is a server already registered as master", reason);
        }
    }

    public static class ChunkServerExistsException extends ZookeeperException {
        public ChunkServerExistsException(Throwable reason) {
            super("A chunk server with this connection string is already registered", reason);
        }
    }

    public static class FileLockedException extends ZookeeperException {
        public FileLockedException(Throwable reason) {
            super("This file is already locked by another client", reason);
        }
    }

    public static class InterruptedException extends ZookeeperException {
        public InterruptedException(Throwable reason) {
            super("The request thread was interrupted", reason);
        }
    }

    public static class NodeNotFoundException extends ZookeeperException {
        public NodeNotFoundException(String path, Throwable reason) {
            super("ZNode " + path + " was not found", reason);
            this.path = path;
        }
    }

    public static class NodeExistsException extends ZookeeperException {
        public NodeExistsException(String path, Throwable reason) {
            super("ZNode " + path + " already exists", reason);
            this.path = path;
        }
    }

    public static class NodeLockedException extends ZookeeperException {
        public NodeLockedException(String path) {
            super("Znode " + path + " is already locked");
            this.path = path;
        }
    }
}
