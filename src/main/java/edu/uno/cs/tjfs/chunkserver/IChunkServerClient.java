package edu.uno.cs.tjfs.chunkserver;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by srjanak on 11/5/15.
 */
public interface IChunkServerClient {
    InputStream get(String chunkName) throws IOException;
    void put(String chunkName, int dataLength, InputStream chunkInputStream) throws Exception;
    void delete(String chunkName);
    String[] list();
}
