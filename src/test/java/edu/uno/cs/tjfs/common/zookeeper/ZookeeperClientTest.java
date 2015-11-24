package edu.uno.cs.tjfs.common.zookeeper;

import edu.uno.cs.tjfs.common.Machine;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ZookeeperClientTest {

    ZookeeperClient zkClient;
    DataMonitor dataMonitor;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Mock
    ZooKeeper zk;

    @Before
    public void setUp() {
        initMocks(this);
        dataMonitor = new DataMonitor();
        zkClient = new ZookeeperClient(zk, dataMonitor);
    }

    @Test
    public void testStart() throws ZookeeperException, KeeperException, InterruptedException {
        zkClient.start();
        verify(zk).create("/master", new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        verify(zk).create("/chunkservers", new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        verify(zk).create("/fs", new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }

    @Test
    public void testRegisterMasterServer() throws ZookeeperException, KeeperException, InterruptedException {
        Machine machine = Machine.fromString("127.0.0.1:8000");

        when(zk.create("/master/machine", machine.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL))
            .thenReturn("machine0000000001");
        when(zk.getChildren("/master", false))
            .thenReturn(Arrays.asList("machine0000000001"));
        zkClient.registerMasterServer(machine);

        verify(zk).create("/master/machine", machine.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        verify(zk).getChildren("/master", false);
    }

    @Test
    public void testRegisterMasterServerFailure() throws ZookeeperException, KeeperException, InterruptedException {
        Machine machine = Machine.fromString("127.0.0.1:8000");

        when(zk.create("/master/machine", machine.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL))
            .thenReturn("machine0000000002");
        when(zk.getChildren("/master", false))
            .thenReturn(Arrays.asList("machine0000000001", "machine0000000002"));

        exception.expect(ZookeeperException.MasterAlreadyExistsException.class);
        zkClient.registerMasterServer(machine);

        verify(zk).create("/master/machine", machine.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        verify(zk).getChildren("/master", false);
    }

    @Test
    public void testGetMasterServer() throws KeeperException, InterruptedException, ZookeeperException {
        Machine machine = Machine.fromString("127.0.0.1:8000");

        when(zk.getChildren("/master", false))
            .thenReturn(Collections.singletonList("machine0000000001"));
        when(zk.getData("/master/machine0000000001", false, null))
            .thenReturn(machine.getBytes());

        assertThat(zkClient.getMasterServer(), equalTo(machine));
    }

    @Test
    public void testGetMasterServerFailure() throws KeeperException, InterruptedException, ZookeeperException {
        when(zk.getChildren("/master", false))
            .thenReturn(new LinkedList<>());

        exception.expect(ZookeeperException.NoMasterRegisteredException.class);
        zkClient.getMasterServer();
    }

    @Test
    public void testRegisterChunkServer() throws KeeperException, InterruptedException, ZookeeperException {
        Machine machine = Machine.fromString("127.0.0.1:8000");

        when(zk.create("/chunkservers/" + machine, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL))
            .thenReturn(machine.toString());

        zkClient.registerChunkServer(machine);
        verify(zk).create("/chunkservers/" + machine, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
    }

    @Test
    public void testRegisterChunkServerFailure() throws KeeperException, InterruptedException, ZookeeperException {
        Machine machine = Machine.fromString("127.0.0.1:8000");

        when(zk.create("/chunkservers/" + machine, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL))
            .thenThrow(KeeperException.create(KeeperException.Code.NODEEXISTS));

        exception.expect(ZookeeperException.ChunkServerExistsException.class);
        zkClient.registerChunkServer(machine);
    }

    @Test
    public void testGetChunkServers() throws KeeperException, InterruptedException, ZookeeperException {
        Machine machine1 = Machine.fromString("127.0.0.1:8000");
        Machine machine2 = Machine.fromString("127.0.0.2:8000");

        when(zk.getChildren("/chunkservers", false))
            .thenReturn(Arrays.asList(machine1.toString(), machine2.toString()));

        assertThat(zkClient.getChunkServers(), hasItems(machine1, machine2));
    }

    private void testAcquireFileLock(IZookeeperClient.LockType lockType, String actualPath, List<String> children) throws KeeperException, InterruptedException, ZookeeperException {
        Path path = Paths.get("/ubuntu/iso.iso");

        when(zk.create("/fs/" + path.toString().hashCode() + "/" + lockType.toString(), new byte[0],
            ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL))
            .thenReturn(actualPath);
        when(zk.getChildren("/fs/" + path.toString().hashCode(), false))
            .thenReturn(children);
        zkClient.acquireFileLock(path, lockType);

        verify(zk).exists("/fs/" + path.toString().hashCode(), false);
        verify(zk).create("/fs/" + path.toString().hashCode() + "/" + lockType.toString(), new byte[0],
            ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        verify(zk).getChildren("/fs/" + path.toString().hashCode(), false);
    }

    @Test
    public void testAcquireReadLock() throws ZookeeperException, KeeperException, InterruptedException {
        testAcquireFileLock(IZookeeperClient.LockType.READ, "read0000000001",
            Arrays.asList("read0000000001", "write0000000002"));
    }

    @Test
    public void testAcquireConcurrentReadLock() throws ZookeeperException, KeeperException, InterruptedException {
        testAcquireFileLock(IZookeeperClient.LockType.READ, "read0000000002",
            Arrays.asList("read0000000002", "read0000000001", "write0000000003"));
    }

    @Test
    public void testAcquireWriteLock() throws ZookeeperException, KeeperException, InterruptedException {
        testAcquireFileLock(IZookeeperClient.LockType.WRITE, "write0000000001",
            Arrays.asList("write0000000001", "write0000000002"));
    }

    @Test
    public void testAcquireReadLockFailure() throws ZookeeperException, KeeperException, InterruptedException {
        exception.expect(ZookeeperException.FileLockedException.class);
        testAcquireFileLock(IZookeeperClient.LockType.READ, "read0000000003",
            Arrays.asList("read0000000001", "write0000000002"));
    }

    @Test
    public void testAcquireWriteLockFailure() throws ZookeeperException, KeeperException, InterruptedException {
        exception.expect(ZookeeperException.FileLockedException.class);
        testAcquireFileLock(IZookeeperClient.LockType.WRITE, "write0000000003",
            Arrays.asList("read0000000001", "read0000000002"));
    }

    @Test
    public void testReleaseFileLock() throws ZookeeperException, KeeperException, InterruptedException {
        Path path = Paths.get("/ubuntu/iso.iso");
        zkClient.currentLocks.put("/fs/" + path.toString().hashCode() + "/read", "/fs/" + path.toString().hashCode() + "/read0000000001");
        zkClient.releaseFileLock(path, IZookeeperClient.LockType.READ);
        verify(zk).delete("/fs/" + path.toString().hashCode() + "/read0000000001", -1);
    }

    @Test
    public void testIsIgnored() throws Exception {
        assertTrue(ZookeeperClient.isIgnored("read0000000002", IZookeeperClient.LockType.READ));
        assertTrue(ZookeeperClient.isIgnored("read0000000004", IZookeeperClient.LockType.READ));
        assertFalse(ZookeeperClient.isIgnored("write0000000004", IZookeeperClient.LockType.READ));
        assertFalse(ZookeeperClient.isIgnored("read0000000004", IZookeeperClient.LockType.WRITE));
        assertFalse(ZookeeperClient.isIgnored("read0000000004", null));
    }

    @Test
    public void testGetCounter() throws Exception {
        assertThat(ZookeeperClient.getCounter("read0000000002"), is(2));
        assertThat(ZookeeperClient.getCounter("read0000000012"), is(12));
        assertThat(ZookeeperClient.getCounter("read"), is(0));
        assertThat(ZookeeperClient.getCounter("readabcdefghijkalmn"), is(0));
    }
}