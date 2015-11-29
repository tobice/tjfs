package edu.uno.cs.tjfs.master;

import com.google.gson.*;
import edu.uno.cs.tjfs.Config;
import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class MasterStorageTest {

    Path storageFolder = Paths.get("/");
    Path snapshotsFolder = storageFolder.resolve("snapshots");
    Path logFolder = storageFolder.resolve("log");
    MasterStorage masterStorage;

    Machine machine1 = Machine.fromString("127.0.0.1:80");
    Machine machine2 = Machine.fromString("127.0.0.1:80");
    ChunkDescriptor chunk1;
    ChunkDescriptor chunk2;
    ChunkDescriptor chunk3;
    FileDescriptor file1;
    FileDescriptor file2;
    FileDescriptor file3;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Mock
    IZookeeperClient zookeeperClient;

    @Mock
    ILocalFsClient localFsClient;

    @Mock
    IMasterClient masterClient;

    @Mock
    SnapshotStorage snapshotStorage;

    @Before
    public void setUp() throws IOException {
        initMocks(this);
        masterStorage = new MasterStorage(storageFolder, localFsClient, masterClient, 0, 0);
        masterStorage.snapshotStorage = snapshotStorage; // Ugly way to inject mocked object but  what the hell

        chunk1 = new ChunkDescriptor("chunk1", Arrays.asList(machine1, machine2), 10, 0);
        chunk2 = new ChunkDescriptor("chunk2", Arrays.asList(machine1, machine2), 10, 0);
        chunk3 = new ChunkDescriptor("chunk3", Arrays.asList(machine1, machine2), 10, 0);

        file1 = new FileDescriptor(Paths.get("/abc"), new Date(), new ArrayList<>(Collections.singletonList(chunk1)));
        file2 = new FileDescriptor(Paths.get("/def"), new Date(), new ArrayList<>(Collections.singletonList(chunk2)));
        file3 = new FileDescriptor(Paths.get("/ghi"), new Date(), new ArrayList<>(Collections.singletonList(chunk3)));

        when(localFsClient.list(logFolder)).thenReturn(new String[0]);
        when(localFsClient.list(snapshotsFolder)).thenReturn(new String[0]);
    }

    @Test
    public void testInitWithEmptyLog() throws IOException, TjfsException {
        when(localFsClient.exists(logFolder)).thenReturn(false);
        when(localFsClient.exists(snapshotsFolder)).thenReturn(false);
        when(localFsClient.list(logFolder)).thenReturn(new String[0]);

        masterStorage.init();

        verify(localFsClient).mkdir(logFolder);
        verify(localFsClient).mkdir(snapshotsFolder);
        assertThat(masterStorage.fileSystem.size(), equalTo(0));
        assertThat(masterStorage.version, equalTo(0));
    }

    @Test
    public void testInitWithExistingLog() throws IOException, TjfsException {
        Gson gson = CustomGson.create();

        // Second descriptor is empty
        file2 = new FileDescriptor(file2.path);

        // Third descriptor is updating the first one
        file3 = new FileDescriptor(file1.path, file1.time, file3.chunks);

        when(localFsClient.exists(logFolder)).thenReturn(true);
        when(localFsClient.exists(snapshotsFolder)).thenReturn(true);
        when(localFsClient.list(logFolder)).thenReturn(new String[] {"1", "2", "3"});

        // Log contains 3 entries, the first one and the third one are updating the same file
        when(localFsClient.readBytesFromFile(logFolder.resolve("1"))).thenReturn(gson.toJson(file1).getBytes());
        when(localFsClient.readBytesFromFile(logFolder.resolve("2"))).thenReturn(gson.toJson(file2).getBytes());
        when(localFsClient.readBytesFromFile(logFolder.resolve("3"))).thenReturn(gson.toJson(file3).getBytes());

        masterStorage.init();

        assertThat(masterStorage.fileSystem.get(file1.path), equalTo(file3));
        assertThat(masterStorage.fileSystem.get(file2.path), equalTo(null));
        assertThat(masterStorage.fileSystem.size(), equalTo(1));
        assertThat(masterStorage.version, equalTo(3));
    }

    @Test
    public void testGetFile() throws TjfsException {
        masterStorage.fileSystem.put(file1.path, file1);
        assertThat(masterStorage.getFile(file1.path), equalTo(file1));
        assertThat(masterStorage.getFile(file2.path), equalTo(null));
    }

    @Test
    public void testGetFileThatDoesNotExist() throws TjfsException {
        file1 = new FileDescriptor(file1.path);
        assertThat(masterStorage.getFile(file1.path), equalTo(null));
    }

    @Test
    public void testGetADirectory() throws TjfsException {
        file1 = new FileDescriptor(Paths.get("/folder/a"), file1.time, file1.chunks);
        masterStorage.fileSystem.put(file1.path, file1);

        exception.expect(TjfsException.class);
        exception.expectMessage("Cannot get a directory");

        masterStorage.getFile(Paths.get("/folder"));
    }

    @Test
    public void testPutAndGet() throws TjfsException, IOException {
        Gson gson = CustomGson.create();

        // Third descriptor is updating the first one
        file3 = new FileDescriptor(file1.path, file1.time, file3.chunks);

        masterStorage.putFile(file1);
        masterStorage.putFile(file2);
        masterStorage.putFile(file3);

        assertThat(masterStorage.fileSystem.get(file1.path), equalTo(file3));
        assertThat(masterStorage.fileSystem.get(file2.path), equalTo(file2));
        assertThat(masterStorage.fileSystem.size(), equalTo(2));
        assertThat(masterStorage.version, equalTo(3));

        // Make sure that the files have been actually written and without the chunk servers
        verify(localFsClient).writeBytesToFile(logFolder.resolve("1"), gson.toJson(file1.withoutChunkServers()).getBytes());
        verify(localFsClient).writeBytesToFile(logFolder.resolve("2"), gson.toJson(file2.withoutChunkServers()).getBytes());
        verify(localFsClient).writeBytesToFile(logFolder.resolve("3"), gson.toJson(file3.withoutChunkServers()).getBytes());
    }

    @Test
    public void testPutEmptyFile() throws TjfsException, IOException {
        Gson gson = CustomGson.create();
        file2 = new FileDescriptor(file1.path);

        masterStorage.putFile(file1);
        assertThat(masterStorage.fileSystem.get(file1.path), equalTo(file1));
        masterStorage.putFile(file2);
        assertThat(masterStorage.fileSystem.get(file1.path), equalTo(null));

        verify(localFsClient).writeBytesToFile(logFolder.resolve(masterStorage.version + ""), gson.toJson(file2).getBytes());
    }

    @Test
    public void testPutFolder() throws TjfsException {
        ArrayList<ChunkDescriptor> chunks = file1.chunks;
        masterStorage.putFile(new FileDescriptor(Paths.get("/folder/b"), null, chunks));
        masterStorage.putFile(new FileDescriptor(Paths.get("/fold/b"), null, chunks));

        exception.expect(TjfsException.class);
        exception.expectMessage("This file is a directory.");
        masterStorage.putFile(new FileDescriptor(Paths.get("/folder"), null, chunks));
    }

    @Test
    public void testGetLog() throws IOException {
        Gson gson = CustomGson.create();

        // Third descriptor is updating the first one
        file3 = new FileDescriptor(file1.path, file1.time, file3.chunks);

        // Log contains 3 entries, the first one and the fourth one are updating the same file.
        // The third entry is for some reason missing.
        when(localFsClient.list(logFolder)).thenReturn(new String[] {"1", "2", "4"});
        when(localFsClient.readBytesFromFile(logFolder.resolve("1"))).thenReturn(gson.toJson(file1).getBytes());
        when(localFsClient.readBytesFromFile(logFolder.resolve("2"))).thenReturn(gson.toJson(file2).getBytes());
        when(localFsClient.readBytesFromFile(logFolder.resolve("4"))).thenReturn(gson.toJson(file3).getBytes());

        List<IMasterStorage.LogItem> log;

        // Get the whole log
        log = masterStorage.getLog(0);
        assertThat(log.get(0).version, equalTo(1));
        assertThat(log.get(1).version, equalTo(2));
        assertThat(log.get(2).version, equalTo(4));
        assertThat(log.get(0).file, equalTo(file1));
        assertThat(log.get(1).file, equalTo(file2));
        assertThat(log.get(2).file, equalTo(file3));

        // Get only from 3 and up
        log = masterStorage.getLog(2);
        assertThat(log.size(), is(1));
        assertThat(log.get(0).version, equalTo(4));
        assertThat(log.get(0).file, equalTo(file3));
    }

    @Test
    public void testList() throws TjfsException {
        ArrayList<ChunkDescriptor> chunks = file1.chunks;
        masterStorage.putFile(new FileDescriptor(Paths.get("/a"), null, chunks));
        masterStorage.putFile(new FileDescriptor(Paths.get("/b"), null, chunks));
        masterStorage.putFile(new FileDescriptor(Paths.get("/c/a"), null, chunks));
        masterStorage.putFile(new FileDescriptor(Paths.get("/c/b/a"), null, chunks));
        masterStorage.putFile(new FileDescriptor(Paths.get("/c/b/ba/a"), null, chunks));

        assertThat(masterStorage.list(Paths.get("/")), equalTo(new String[] {"a", "b", "c/"}));
        assertThat(masterStorage.list(Paths.get("/c")), equalTo(new String[] {"a", "b/" }));
        assertThat(masterStorage.list(Paths.get("/c/")), equalTo(new String[] {"a", "b/" }));
        assertThat(masterStorage.list(Paths.get("/c/b")), equalTo(new String[] {"ba/", "a" }));
        assertThat(masterStorage.list(Paths.get("/c/b/a")), equalTo(new String[0]));
        assertThat(masterStorage.list(Paths.get("/c/b/b")), equalTo(new String[0]));
    }

    @Test
    public void testUpdateLog() throws TjfsException {
        // Second descriptor is empty
        file2 = new FileDescriptor(file2.path);

        // Third descriptor is updating the first one
        file3 = new FileDescriptor(file1.path, file1.time, file3.chunks);

        masterStorage.putFile(file1);

        List<IMasterStorage.LogItem> log = new LinkedList<>();
        log.add(new IMasterStorage.LogItem(2, file2));
        log.add(new IMasterStorage.LogItem(3, file3));

        masterStorage.updateLog(log);

        assertThat(masterStorage.fileSystem.get(file1.path), equalTo(file3));
        assertThat(masterStorage.fileSystem.get(file2.path), equalTo(null));
        assertThat(masterStorage.fileSystem.size(), equalTo(1));
        assertThat(masterStorage.version, equalTo(3));
    }

    @Test
    public void testUpdateInvalidLog() throws TjfsException {
        // Third descriptor is updating the first one
        file3 = new FileDescriptor(file1.path, file1.time, file3.chunks);

        masterStorage.putFile(file1);

        List<IMasterStorage.LogItem> log = new LinkedList<>();
        log.add(new IMasterStorage.LogItem(1, file2));
        log.add(new IMasterStorage.LogItem(2, file3));

        exception.expect(TjfsException.class);
        exception.expectMessage("Incoming log is older than local data!");

        masterStorage.updateLog(log);
    }

    @Test
    public void testLoadSnapshot() throws TjfsException {
        IMasterStorage.Snapshot snapshot = new IMasterStorage.Snapshot(3, Arrays.asList(file1, file2, file3));
        masterStorage.loadSnapshot(snapshot);

        assertThat(masterStorage.version, is(snapshot.version));
        assertThat(masterStorage.fileSystem.size(), is(3));
        assertThat(masterStorage.fileSystem.values(), hasItems(file1, file2, file3));

        verify(snapshotStorage).store(snapshot);
    }

    @Test
    public void testClearFilesystem() throws IOException, TjfsException {
        when(localFsClient.list(logFolder)).thenReturn(new String[] {"1", "2", "3"});
        when(localFsClient.list(snapshotsFolder)).thenReturn(new String[] {"2"});

        // Create some garbage
        masterStorage.fileSystem.put(file1.path, file1);
        masterStorage.fileSystem.put(file2.path, file2);
        masterStorage.version = 5;

        masterStorage.clearFilesystem();

        verify(localFsClient).list(logFolder);
        verify(localFsClient).list(snapshotsFolder);

        verify(localFsClient).deleteFile(logFolder.resolve("1"));
        verify(localFsClient).deleteFile(logFolder.resolve("2"));
        verify(localFsClient).deleteFile(logFolder.resolve("3"));

        verify(localFsClient).deleteFile(snapshotsFolder.resolve("2"));

        assertThat(masterStorage.fileSystem.size(), is(0));
        assertThat(masterStorage.version, is(0));
    }

    @Test
    public void testMakeFirstSnapshot() throws TjfsException, IOException {
        Gson gson = CustomGson.create();

        // There is no last snapshot
        when(masterStorage.getLastSnapshot()).thenReturn(null);

        // Current log
        when(localFsClient.list(logFolder)).thenReturn(new String[] {"1", "2", "3"});
        when(localFsClient.readBytesFromFile(logFolder.resolve("1"))).thenReturn(gson.toJson(file1).getBytes());
        when(localFsClient.readBytesFromFile(logFolder.resolve("2"))).thenReturn(gson.toJson(file2).getBytes());
        when(localFsClient.readBytesFromFile(logFolder.resolve("3"))).thenReturn(gson.toJson(file3).getBytes());

        // Current version
        masterStorage.version = 3;

        masterStorage.makeSnapshot();

        // Make sure that the log has been loaded
        verify(localFsClient).readBytesFromFile(logFolder.resolve("1"));
        verify(localFsClient).readBytesFromFile(logFolder.resolve("2"));
        verify(localFsClient).readBytesFromFile(logFolder.resolve("3"));

        // Make sure that the snapshot has been stored
        ArgumentCaptor<IMasterStorage.Snapshot> argument = ArgumentCaptor.forClass(IMasterStorage.Snapshot.class);
        verify(snapshotStorage).store(argument.capture());

        // Make sure that correct snapshot has been created
        IMasterStorage.Snapshot snapshot = argument.getValue();
        assertThat(snapshot.version, is(3));
        assertThat(snapshot.files.size(), is(3));
        assertThat(snapshot.files, hasItems(file1, file2, file3));
    }

    @Test
    public void testMakeSnapshotBasedOnOlder() throws TjfsException, IOException {
        Gson gson = CustomGson.create();

        // Third descriptor is updating the first one
        file3 = new FileDescriptor(file1.path, file1.time, file3.chunks);

        // The last snapshot
        IMasterStorage.Snapshot latestSnapshot = new IMasterStorage.Snapshot(2, Arrays.asList(file1, file2));
        when(masterStorage.getLastSnapshot()).thenReturn(latestSnapshot);

        // Updates in the log
        when(localFsClient.list(logFolder)).thenReturn(new String[] {"1", "2", "3"});
        when(localFsClient.readBytesFromFile(logFolder.resolve("3"))).thenReturn(gson.toJson(file3).getBytes());

        // Current version
        masterStorage.version = 3;

        masterStorage.makeSnapshot();

        // Make sure that only the last item from the log has been read
        verify(localFsClient, never()).readBytesFromFile(logFolder.resolve("1"));
        verify(localFsClient, never()).readBytesFromFile(logFolder.resolve("2"));
        verify(localFsClient).readBytesFromFile(logFolder.resolve("3"));

        // Make sure that the snapshot has been stored
        ArgumentCaptor<IMasterStorage.Snapshot> argument = ArgumentCaptor.forClass(IMasterStorage.Snapshot.class);
        verify(snapshotStorage).store(argument.capture());

        // Make sure that correct snapshot has been created
        IMasterStorage.Snapshot snapshot = argument.getValue();
        assertThat(snapshot.version, is(3));
        assertThat(snapshot.files.size(), is(2));
        assertThat(snapshot.files.get(0), equalTo(file2));
        assertThat(snapshot.files.get(1), equalTo(file3));
    }

    @Test
    public void testMakeNoSnapshot() throws TjfsException {
        // The last snapshot
        IMasterStorage.Snapshot latestSnapshot = new IMasterStorage.Snapshot(3, Arrays.asList(file1, file2));
        when(masterStorage.getLastSnapshot()).thenReturn(latestSnapshot);

        // Current version is the same one as the snapshot's one
        masterStorage.version = 3;

        masterStorage.makeSnapshot();

        // Since the version is still the same make sure that we didn't create a new snapshot
        verify(snapshotStorage, never()).store(anyObject());
    }
}
