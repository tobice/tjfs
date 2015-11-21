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

            byte[] data = localFsClient.readBytesFromFile(Paths.get("1"));

            Machine machine = new Machine("192.168.43.27", 6002);
            Machine machine2 = new Machine("192.168.43.218", 6002);

            LinkedList<Machine> machines = new LinkedList<>();
            machines.add(machine);
            machines.add(machine2);

            ChunkDescriptor chunkDescriptor = new ChunkDescriptor("0", machines, data.length, 1);

            String test = IOUtils.toString(IOUtils.toByteArray(chunkClient.get(chunkDescriptor)), "UTF-8");
            System.out.println("file is " + test);

            //chunkClient.put(chunkDescriptor, data.length, new ByteArrayInputStream(data));
        }catch(Exception e){
            System.out.println("Error is " + e.getMessage());
        }
    }
}
