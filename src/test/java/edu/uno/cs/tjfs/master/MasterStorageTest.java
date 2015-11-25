package edu.uno.cs.tjfs.master;

import com.google.gson.*;
import com.sun.prism.shader.Solid_TextureYV12_AlphaTest_Loader;
import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import javax.sound.midi.SysexMessage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class MasterStorageTest {

    private MasterStorage masterStorage;

    @Mock
    private IZookeeperClient zookeeperClient;

    @Mock
    private IChunkClient chunkClient;

    @Before
    public void setUp() throws IOException {
        LocalFsClient localFsClient = new LocalFsClient();
        ChunkServerService csServerice = new ChunkServerService(zookeeperClient, chunkClient);
        masterStorage = new MasterStorage(Paths.get("fs/test"), localFsClient, csServerice);
        masterStorage.init();
    }

    @Test
    public void  getPutFileTest() throws IOException{
        //should not return null
        Path testPath = Paths.get("/home/testpath");
        FileDescriptor fileDescriptor = masterStorage.getFile(testPath);
        assertTrue(fileDescriptor.path.equals(testPath));


        ChunkDescriptor chunkDescriptor = new ChunkDescriptor("someChunk", new ArrayList<>());
        Path path = Paths.get("/some/file/path");
        ArrayList<ChunkDescriptor> chunks = new ArrayList<>();
        chunks.add(chunkDescriptor);
        fileDescriptor = new FileDescriptor(path);

        masterStorage.putFile(path, fileDescriptor);
        FileDescriptor resultFile = masterStorage.getFile(path);
        Gson gson = CustomGson.create();
        assertTrue(resultFile.equals(fileDescriptor));

        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, 1);

        //same path just different file descriptor...should return the latest one
        fileDescriptor = new FileDescriptor(path, c.getTime(), chunks);
        masterStorage.putFile(path, fileDescriptor);
        resultFile = masterStorage.getFile(path);

        assertTrue(fileDescriptor.time == resultFile.time);

        //so far i have written two files, so if i do getlog and give id =  should return >= 1 == 1
        assertTrue(this.masterStorage.getLog(0).size() == 2);
        assertTrue(this.masterStorage.getLog(1).size() == 1);
        assertTrue(this.masterStorage.getLog(2).size() == 0);

        List<FileDescriptor> logs = this.masterStorage.getLog(0);

        //Now if i update the log with what i get from calling getlog(0), it should have four files
        this.masterStorage.updateLog(logs);

        LocalFsClient fs = new LocalFsClient();
        assertTrue(fs.listFiles(Paths.get("fs/test")).length == 4);

        //this get file test should still pass
        resultFile = masterStorage.getFile(path);

        assertTrue(fileDescriptor.time.toString().equals(resultFile.time.toString()));
    }

    @After
    public void clear() throws IOException {
        LocalFsClient ls = new LocalFsClient();
        ls.deleteFile(Paths.get("fs/test/0"));
        ls.deleteFile(Paths.get("fs/test/1"));
        ls.deleteFile(Paths.get("fs/test/2"));
        ls.deleteFile(Paths.get("fs/test/3"));
    }
}