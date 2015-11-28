package edu.uno.cs.tjfs.client;

import edu.uno.cs.tjfs.SoftConfig;
import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import static edu.uno.cs.tjfs.common.zookeeper.IZookeeperClient.LockType.*;

public class TjfsClientTest {

    IMasterClient masterClient;
    SoftConfig config;
    ITjfsClient tjfsClient;
    Path path;
    FileDescriptor file;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Mock
    IChunkClient chunkClient;

    @Mock
    IZookeeperClient zkClient;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        masterClient = new DummyMasterClient();
        config = new SoftConfig();
        tjfsClient = new TjfsClient(config, masterClient, chunkClient, zkClient);

        path = Paths.get("/some/file");
        file = new FileDescriptor(path, new Date(),
            new ArrayList<>(Arrays.asList(
                new ChunkDescriptor("0", new LinkedList<>(), 3, 0),
                new ChunkDescriptor("1", new LinkedList<>(), 3, 1),
                new ChunkDescriptor("2", new LinkedList<>(), 3, 1),
                new ChunkDescriptor("3", new LinkedList<>(), 3, 1),
                new ChunkDescriptor("4", new LinkedList<>(), 2, 2))));
    }

    @Test
    public void testPut() throws Exception {
        // The put operation is quite complicated and all edge cases are tested separately in the
        // operation sub components (JobProducer, JobExecutor, PutChunkJob). This merely makes
        // sure that all components work together as they should.

        final String lock = "lock_node";
        when(zkClient.acquireFileLock(path, WRITE)).thenReturn(lock);

        config.setChunkSize(3);
        config.setExecutorPoolSize(2);
        config.setExecutorQueueSize(2);
        tjfsClient.put(path, new ByteArrayInputStream("abcdefgh".getBytes()));

        // Test that the file metadata (chunks) have been updated on the master server
        FileDescriptor file = masterClient.getFile(path);
        assertThat(file.getChunk(0).size, is(3));
        assertThat(file.getChunk(1).size, is(3));
        assertThat(file.getChunk(2).size, is(2));
        assertThat(file.getChunk(3), equalTo(null));

        // Test that the file has been locked and unlocked
        verify(zkClient).acquireFileLock(path, WRITE);
        verify(zkClient).releaseFileLock(lock);

        // Test that the chunks have been correctly pushed using the chunk client.
        verify(chunkClient).put(anyObject(), eq("abc".getBytes()));
        verify(chunkClient).put(anyObject(), eq("def".getBytes()));
        verify(chunkClient).put(anyObject(), eq("gh".getBytes()));
    }

    @Test
    public void testGet() throws Exception {
        // Read chunks from the file system and store them into a string. As in the previous
        // case, all the edge situation are covered by the test for individual components. Here
        // we're merely testing that everything works together.

        config.setChunkSize(3);
        config.setExecutorPoolSize(2); // squeeze it a bit to test queueing
        config.setExecutorQueueSize(2);
        config.setPipeBufferSize(2);
        masterClient.putFile(file);

        final String lock = "lock_node";
        when(zkClient.acquireFileLock(path, READ)).thenReturn(lock);

        when(chunkClient.get(file.getChunk(0))).thenReturn("abc".getBytes());
        when(chunkClient.get(file.getChunk(1))).thenReturn("def".getBytes());
        when(chunkClient.get(file.getChunk(2))).thenReturn("ghi".getBytes());
        when(chunkClient.get(file.getChunk(3))).thenReturn("jkl".getBytes());
        when(chunkClient.get(file.getChunk(4))).thenReturn("mn".getBytes());

        InputStream stream = tjfsClient.get(path);
        assertThat(IOUtils.toString(stream), equalTo("abcdefghijklmn"));

        // Test that the file has been locked and unlocked (just wait a bit to let the thread
        // finish)
        Thread.sleep(100);
        verify(zkClient).acquireFileLock(path, READ);
        verify(zkClient).releaseFileLock(lock);
    }

    @Test
    public void testGetWithAnError() throws Exception {
        // Read chunks from the file system and store them into a string, except now the last
        // chunk will fail to load.

        config.setChunkSize(3);
        masterClient.putFile(file);

        when(chunkClient.get(file.getChunk(0))).thenReturn("abc".getBytes());
        when(chunkClient.get(file.getChunk(1))).thenReturn("def".getBytes());
        when(chunkClient.get(file.getChunk(2))).thenReturn("ghi".getBytes());
        when(chunkClient.get(file.getChunk(3))).thenReturn("jkl".getBytes());
        when(chunkClient.get(file.getChunk(4))).thenThrow(new TjfsException("Some reason"));

        exception.expect(IOException.class);
        exception.expectMessage("Downloading file failed. Get chunk job failed. Reason: Some reason");

        InputStream stream = tjfsClient.get(path);
        IOUtils.toString(stream);
        stream.close();
    }

    @Test
    public void testDelete() throws Exception {
        final String lock = "lock_node";
        when(zkClient.acquireFileLock(path, WRITE)).thenReturn(lock);

        // First put a standard file, then delete it and finally make sure that the file has
        // been replaced with an empty descriptor
        masterClient.putFile(file);
        assertThat(masterClient.getFile(file.path), equalTo(file));
        tjfsClient.delete(file.path);
        assertThat(masterClient.getFile(file.path), equalTo(new FileDescriptor(file.path)));

        // Test that the file has been locked and unlocked
        verify(zkClient).acquireFileLock(file.path, WRITE);
        verify(zkClient).releaseFileLock(lock);
    }

    @Test
    public void testGetSize() throws Exception {
        masterClient.putFile(file);
        assertThat(tjfsClient.getSize(file.path), equalTo(file.getSize()));
    }

    @Test
    public void testGetTime() throws Exception {
        masterClient.putFile(file);
        assertThat(tjfsClient.getTime(file.path), equalTo(file.time));
    }

    @Test
    public void testList() throws Exception {
        masterClient.putFile(file);
        assertThat(tjfsClient.list(Paths.get("/some/")), equalTo(new String[]{"file"}));
    }

    @Test
    public void testMove() throws Exception {
        Path sourcePath = Paths.get("/a");
        Path destinationPath = Paths.get("/b");

        final String lock1 = "lock_node1";
        final String lock2 = "lock_node2";
        when(zkClient.acquireFileLock(sourcePath, WRITE)).thenReturn(lock1);
        when(zkClient.acquireFileLock(destinationPath, WRITE)).thenReturn(lock2);

        FileDescriptor sourceFile = new FileDescriptor(sourcePath, file.time, file.chunks);
        masterClient.putFile(sourceFile);
        tjfsClient.move(sourcePath, destinationPath);

        assertThat(masterClient.getFile(sourcePath), equalTo(new FileDescriptor(sourcePath)));
        assertThat(masterClient.getFile(destinationPath), equalTo(new FileDescriptor(destinationPath, file.time, file.chunks)));

        // Test that the file has been locked and unlocked
        verify(zkClient).acquireFileLock(sourcePath, WRITE);
        verify(zkClient).acquireFileLock(destinationPath, WRITE);
        verify(zkClient).releaseFileLock(lock1);
        verify(zkClient).releaseFileLock(lock2);
    }
}
