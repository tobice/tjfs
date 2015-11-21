package edu.uno.cs.tjfs.master;

import edu.uno.cs.tjfs.common.ChunkDescriptor;
import edu.uno.cs.tjfs.common.FileDescriptor;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class MasterStorageTest {

    private MasterStorage masterStorage;

    @Before
    public void setUp() throws IOException {
        masterStorage = new MasterStorage(Paths.get("fs"));
        masterStorage.init();
    }

    @Test
    public void  getFileTest() throws IOException{
        FileDescriptor fileDescriptor = masterStorage.getFile(Paths.get(""));
        assertTrue(fileDescriptor == null);

        ChunkDescriptor chunkDescriptor = new ChunkDescriptor("someChunk", null);
        Path path = Paths.get("/some/file/path");
        ArrayList<ChunkDescriptor> chunks = new ArrayList<>();
        chunks.add(chunkDescriptor);
        fileDescriptor = new FileDescriptor(path, new Date(), chunks);

        masterStorage.putFile(path, fileDescriptor);
        FileDescriptor resultFile = masterStorage.getFile(path);
        assertTrue(resultFile.chunks.size() == 1);
    }
}
