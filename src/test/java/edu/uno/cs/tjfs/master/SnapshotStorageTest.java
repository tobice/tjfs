package edu.uno.cs.tjfs.master;

import com.google.gson.Gson;
import edu.uno.cs.tjfs.common.CustomGson;
import edu.uno.cs.tjfs.common.FileDescriptor;
import edu.uno.cs.tjfs.common.ILocalFsClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class SnapshotStorageTest {
    Path folder = Paths.get("/");
    IMasterStorage.Snapshot snapshot;
    SnapshotStorage snapshotStorage;
    Gson gson = CustomGson.create();

    @Mock
    ILocalFsClient localFsClient;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        snapshotStorage = new SnapshotStorage(localFsClient, folder);

        FileDescriptor file1 = new FileDescriptor(Paths.get("/blah"));
        FileDescriptor file2 = new FileDescriptor(Paths.get("/blah2"));
        snapshot = new IMasterStorage.Snapshot(5, Arrays.asList(file1, file2));
    }

    @Test
    public void testStore() throws Exception {
        snapshotStorage.store(snapshot);
        verify(localFsClient).writeBytesToFile(
            eq(folder.resolve(snapshot.version + "")),
            eq(gson.toJson(snapshot).getBytes()));
    }

    @Test
    public void testRestore() throws Exception {
        when(localFsClient.exists(folder.resolve(snapshot.version + "")))
            .thenReturn(true);
        when(localFsClient.readBytesFromFile(folder.resolve(snapshot.version + "")))
            .thenReturn(gson.toJson(snapshot).getBytes());

        IMasterStorage.Snapshot restored = snapshotStorage.restore(snapshot.version);
        assertThat(restored.version, equalTo(snapshot.version));
        assertThat(restored.files, equalTo(snapshot.files));
    }

    @Test
    public void testGetLatest() throws Exception {
        when(localFsClient.list(folder)).thenReturn(new String[] {"1", "2", snapshot.version + ""});
        when(localFsClient.exists(folder.resolve(snapshot.version + "")))
            .thenReturn(true);
        when(localFsClient.readBytesFromFile(folder.resolve(snapshot.version + "")))
            .thenReturn(gson.toJson(snapshot).getBytes());

        IMasterStorage.Snapshot latest = snapshotStorage.getLatest();
        assertThat(latest.version, equalTo(snapshot.version));
        assertThat(latest.files, equalTo(snapshot.files));
    }

    @Test
    public void testGetLatestWhenThereIsNone() throws Exception {
        when(localFsClient.list(folder)).thenReturn(new String[0]);
        assertThat(snapshotStorage.getLatest(), equalTo(null));
    }
}
