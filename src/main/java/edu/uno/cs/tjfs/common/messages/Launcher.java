package edu.uno.cs.tjfs.common.messages;

import edu.uno.cs.tjfs.common.*;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.LinkedList;

/**
 * Created by janak on 11/16/2015.
 */
public class Launcher {
    public static void main(String[] args) throws IOException, TjfsException{
        try {
            MessageClient messageClient = new MessageClient();

            ChunkClient chunkClient = new ChunkClient(messageClient);

            LocalFsClient localFsClient = new LocalFsClient();

            InputStream dataStream = localFsClient.readFile(Paths.get("testChunk"));

            Machine machine = new Machine("127.0.0.1", 6002);

            LinkedList<Machine> machines = new LinkedList<Machine>();
            machines.add(machine);
            machines.add(machine);


            byte[] fileInMemorey = IOUtils.toByteArray(dataStream);

            ChunkDescriptor chunkDescriptor = new ChunkDescriptor("testChunk", machines, fileInMemorey.length, 0);

            chunkClient.put(chunkDescriptor, fileInMemorey.length, new ByteArrayInputStream(fileInMemorey));
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
    }
}
