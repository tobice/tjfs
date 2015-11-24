package edu.uno.cs.tjfs.common.zookeeper;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

public class DataMonitor implements Watcher {
    /**
     * Something happened and Zookeeper is letting us know.
     * @param watchedEvent event that happened on Zookeeper's side
     */
    @Override
    public void process(WatchedEvent watchedEvent) {

    }
}
