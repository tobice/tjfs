package edu.uno.cs.tjfs.chunkserver;

import edu.uno.cs.tjfs.common.TjfsException;
import edu.uno.cs.tjfs.common.messages.MessageServer;
import org.apache.commons.io.IOUtils;

import java.io.*;

/**
 * Created by srjanak on 11/5/15.
 */
public class Launcher {
    public static void main(String[] args) throws TjfsException, IOException {
        System.out.println("Sending message to chunkServer");

        Server server = new Server();

        MessageServer msgServer = new MessageServer("localhost", 6001, server);

        ChunkServerClient chunkServerClient = new ChunkServerClient(msgServer);

        chunkServerClient.put("chunk.txt", 0, null);
    }

    private static String getStringFromInputStream(InputStream in) throws IOException{
        StringWriter writer = new StringWriter();
        IOUtils.copy(in, writer, "UTF-8");
        System.out.println("Finished reading...");
        String theString = writer.toString();
        return theString;
    }
}
