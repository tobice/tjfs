package edu.uno.cs.tjfs.master;

import com.google.gson.*;
import edu.uno.cs.tjfs.common.ChunkDescriptor;
import edu.uno.cs.tjfs.common.FileDescriptor;
import edu.uno.cs.tjfs.common.LocalFsClient;
import edu.uno.cs.tjfs.common.Machine;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;

import static org.junit.Assert.assertTrue;

public class MasterStorageTest {

    private MasterStorage masterStorage;

    @Before
    public void setUp() throws IOException {

        ChunkDescriptor chunkDescriptor = new ChunkDescriptor("someChunk", new ArrayList<Machine>());
        Gson gson = new Gson();
        GsonBuilder gsonBuilder = new GsonBuilder();
        System.out.println("serializing chunkS");
        System.out.println(gson.toJson(chunkDescriptor));
        System.out.print("Done");

        ArrayList<ChunkDescriptor> chunks = new ArrayList<>();
        chunks.add(chunkDescriptor);
        chunks.add(chunkDescriptor);

        FileDescriptor fileDescriptor = new FileDescriptor(Paths.get(""), new Date(), chunks);

        System.out.println("serializing fileDescriptor");


//        gsonBuilder.registerTypeAdapter()

        System.out.println(gson.toJson(fileDescriptor));
        System.out.print("Done");


        masterStorage = new MasterStorage(Paths.get("fs"), new LocalFsClient());
        masterStorage.init();
    }

    @Test
    public void  getFileTest() throws IOException{
        FileDescriptor fileDescriptor = masterStorage.getFile(Paths.get(""));
        assertTrue(fileDescriptor == null);

        ChunkDescriptor chunkDescriptor = new ChunkDescriptor("someChunk", new ArrayList<Machine>());
        Path path = Paths.get("/some/file/path");
        ArrayList<ChunkDescriptor> chunks = new ArrayList<>();
        chunks.add(chunkDescriptor);
        fileDescriptor = new FileDescriptor(path, new Date(), chunks);

        masterStorage.putFile(path, fileDescriptor);
        FileDescriptor resultFile = masterStorage.getFile(path);
        assertTrue(resultFile.chunks.size() == 1);
    }
}
