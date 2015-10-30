package edu.uno.cs.tjfs.client;

import edu.uno.cs.tjfs.common.DummyLocalFsClient;
import edu.uno.cs.tjfs.common.ILocalFsClient;
import edu.uno.cs.tjfs.common.LocalFsClient;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;
import static org.junit.Assert.*;

public class ClientTest {

    private IClient client;
    private ITjfsClient tjfsClient;
    private ILocalFsClient localFsClient;

    private String file1 = "abcd";
    private String file2 = "efghij";
    private String file3 = "klm";

    private String asString(InputStream stream) throws IOException {
        return IOUtils.toString(stream);
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws IOException {
        tjfsClient = new DummyTjfsClient();
        localFsClient = new DummyLocalFsClient();
        client = new Client(tjfsClient, localFsClient);

        localFsClient.writeFile(Paths.get("/folder/file1"), new ByteArrayInputStream(file1.getBytes()));
        localFsClient.writeFile(Paths.get("/folder/file2"), new ByteArrayInputStream(file2.getBytes()));
        localFsClient.writeFile(Paths.get("/folder/file3"), new ByteArrayInputStream(file3.getBytes()));
    }

    @Test
    public void testPutAndGet() throws IOException, TjfsClientException {
        client.put(Paths.get("/folder/file1"), Paths.get("/remote/file1"));
        client.get(Paths.get("/remote/file1"), Paths.get("/received/file"));

        assertThat(file1, equalTo(asString(localFsClient.readFile(Paths.get("/received/file")))));
    }

    @Test
    public void testPutWithOffset() throws IOException, TjfsClientException {
        // Basically simulate append
        client.put(Paths.get("/folder/file1"), Paths.get("/remote/file1"));
        int byteOffset = client.getSize(Paths.get("/remote/file1"));
        client.put(Paths.get("/folder/file2"), Paths.get("/remote/file1"), byteOffset);
        client.get(Paths.get("/remote/file1"), Paths.get("/received/file"));

        assertThat(file1 + file2, equalTo(asString(localFsClient.readFile(Paths.get("/received/file")))));
    }

    @Test
    public void testGetWithOffsetAndRange() throws IOException, TjfsClientException {
        client.put(Paths.get("/folder/file1"), Paths.get("/remote/file1"));
        client.get(Paths.get("/remote/file1"), Paths.get("/received/file"), 1, 2);
        assertThat("bc", equalTo(asString(localFsClient.readFile(Paths.get("/received/file")))));
    }

    @Test
    public void testGetSize() throws IOException, TjfsClientException {
        client.put(Paths.get("/folder/file2"), Paths.get("/remote/file"));
        assertThat(6, equalTo(client.getSize(Paths.get("/remote/file"))));
    }

    @Test
    public void testGetTime() throws IOException, TjfsClientException {
        client.put(Paths.get("/folder/file2"), Paths.get("/remote/file"));
        assertThat(new Date(), equalTo(client.getTime(Paths.get("/remote/file"))));
        // Okay, this is really hacky. The dummy tjfs client always returns current time (which is
        // probably not right but hey, it's a dummy!). I just rely on the fact that this test is
        // performed fast enough so that the current timestamp doesn't change.
    }

    @Test
    public void testList() throws IOException, TjfsClientException {
        client.put(Paths.get("/folder/file1"), Paths.get("/remote/file1"));
        client.put(Paths.get("/folder/file2"), Paths.get("/remote/file2"));
        client.put(Paths.get("/folder/file3"), Paths.get("/remote/file3"));

        assertThat(client.list(Paths.get("/")), equalTo(new String[]{"remote/"}));
        assertThat(client.list(Paths.get("/remote")), arrayContainingInAnyOrder("file1", "file2", "file3"));
    }

    @Test
    public void testGetNonExistingFile() throws IOException, TjfsClientException {
        exception.expect(TjfsClientException.class);
        exception.expectMessage("File not found");
        client.get(Paths.get("/random/file"), Paths.get("/received/file"));
    }

    @Test
    public void testDelete() throws IOException, TjfsClientException {
        exception.expect(TjfsClientException.class);
        exception.expectMessage("File not found");
        client.put(Paths.get("/folder/file1"), Paths.get("/remote/file"));
        client.delete(Paths.get("/remote/file"));
        client.get(Paths.get("/remote/file"), Paths.get("/received/file"));
    }

    @Test
    public void testMove() throws IOException, TjfsClientException {
        client.put(Paths.get("/folder/file1"), Paths.get("/remote/file"));
        client.move(Paths.get("/remote/file"), Paths.get("/remote/file_moved"));
        client.get(Paths.get("/remote/file_moved"), Paths.get("/received/file"));
        assertThat(file1, equalTo(asString(localFsClient.readFile(Paths.get("/received/file")))));

        exception.expect(TjfsClientException.class);
        exception.expectMessage("File not found");
        client.get(Paths.get("/remote/file"), Paths.get("/received/file"));
    }
}