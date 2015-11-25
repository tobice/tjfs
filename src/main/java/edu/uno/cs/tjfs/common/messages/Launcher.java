package edu.uno.cs.tjfs.common.messages;

import edu.uno.cs.tjfs.common.*;
import org.apache.commons.io.IOUtils;
import sun.nio.ch.IOUtil;

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

            byte[] data = localFsClient.readBytesFromFile(Paths.get("/home/srjanak/testFSFiles/1"));

            Machine machine = new Machine("127.0.0.1", 6002);
            Machine machine2 = new Machine("127.0.0.1", 6002);

            LinkedList<Machine> machines = new LinkedList<>();
            machines.add(machine);
            machines.add(machine2);

            ChunkDescriptor chunkDescriptor = new ChunkDescriptor("2", machines, data.length, 1);


            chunkClient.put(chunkDescriptor, data.length, new ByteArrayInputStream(data));
            String test = IOUtils.toString(IOUtils.toByteArray(chunkClient.get(chunkDescriptor)), "UTF-8");
            System.out.println("file is " + test);
        }catch(Exception e){
            System.out.println("Error is " + e.getMessage());
        }
    }
}
