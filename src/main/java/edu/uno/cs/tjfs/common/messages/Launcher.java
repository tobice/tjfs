package edu.uno.cs.tjfs.common.messages;

import edu.uno.cs.tjfs.common.*;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.LinkedList;

public class Launcher {
    public static void main(String[] args) throws IOException, TjfsException{
        try {
            MessageClient messageClient = new MessageClient();

            ChunkClient chunkClient = new ChunkClient(messageClient);

            LocalFsClient localFsClient = new LocalFsClient();

            byte[] data = localFsClient.readBytesFromFile(Paths.get("/home/srjanak/test/testChunk"));

            Machine machine = new Machine("127.0.0.1", 6002);

            LinkedList<Machine> machines = new LinkedList<>();
            machines.add(machine);
            machines.add(machine);
            ChunkDescriptor chunkDescriptor = new ChunkDescriptor("/home/srjanak/test/testChunk2", machines, data.length, 0);

            chunkClient.put(chunkDescriptor, data.length, new ByteArrayInputStream(data));
        }catch(Exception e){
            System.out.println(e.getMessage());
        }
    }
}
