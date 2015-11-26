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

    public String getCurrentIPAddress() throws TjfsException {
        String result = "";
        try {
            Enumeration e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface n = (NetworkInterface) e.nextElement();
                Enumeration ee = n.getInetAddresses();
                while (ee.hasMoreElements()) {
                    InetAddress i = (InetAddress) ee.nextElement();
                    String hostAddress = i.getHostAddress();
                    if (!hostAddress.contains("192.168.")
                            && !hostAddress.contains("0:0:0"))
                        result = hostAddress;
                }
            }
        }catch(Exception e){
            //do nothing should handle this in upper layer if ip is empty just throw error
        }
        return result;
    }

    public int getMasterPort(){
        return 6002;
    }
}
