package edu.uno.cs.tjfs.client;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.Date;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class CommandLineClientTest {

    /** Instance of the client that we are testing */
    private CommandLineClient commandLineClient;

    /** Output stream to which the command line client will print error messages */
    private ByteArrayOutputStream log;

    @Mock
    private IClient client;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        log = new ByteArrayOutputStream();
        commandLineClient = new CommandLineClient(client, new PrintStream(log));
    }

    @After
    public void tearDown() {
        // Let's print the log
        // System.out.println(log.toString());
    }

    @Test
    public void shouldFailOnUnknownCommand() {
        commandLineClient.command("blah /remote/source /local/target");
        assertThat(log.toString(), startsWith("Invalid command. Unknown command blah"));
    }

    @Test
    public void shouldFailOnMissingOptionArgument() {
        commandLineClient.command("get /remote/source /local/target --byteOffset --numberOfBytes");
        assertThat(log.toString(), startsWith("Invalid command. Missing argument for option"));
    }

    @Test
    public void shouldFailOnInvalidOptionArgumentType() {
        commandLineClient.command("get /remote/source /local/target --byteOffset a --numberOfBytes b");
        assertThat(log.toString(), startsWith("Invalid command. For input string"));
    }

    @Test
    public void shouldCallGet() throws IOException, TjfsClientException {
        commandLineClient.command("get /remote/source /local/target");
        verify(client).get(Paths.get("/remote/source"), Paths.get("/local/target"));
    }

    @Test
    public void shouldCallGetWithOptions() throws IOException, TjfsClientException {
        commandLineClient.command("get /remote/source /local/target --byteOffset 1000 --numberOfBytes 2000");
        verify(client).get(Paths.get("/remote/source"), Paths.get("/local/target"), 1000, 2000);
    }

    @Test
    public void shouldCallPut() throws IOException, TjfsClientException {
        commandLineClient.command("put /local/file /remote/target");
        verify(client).put(Paths.get("/local/file"), Paths.get("/remote/target"));
    }

    @Test
    public void shouldCallPutWithOptions() throws IOException, TjfsClientException {
        commandLineClient.command("put /local/file /remote/target --byteOffset 1200");
        verify(client).put(Paths.get("/local/file"), Paths.get("/remote/target"), 1200);
    }

    @Test
    public void shouldCallDelete() throws TjfsClientException {
        commandLineClient.command("delete /remote/file");
        verify(client).delete(Paths.get("/remote/file"));
    }

    @Test
    public void shouldCallGetSize() throws TjfsClientException {
        when(client.getSize(Paths.get("/remote/file"))).thenReturn(1234);
        commandLineClient.command("getsize /remote/file");
        verify(client).getSize(Paths.get("/remote/file"));
        assertThat(log.toString(), startsWith("1234 bytes (1 KB)"));
    }

    @Test
    public void shouldCallGetTime() throws TjfsClientException {
        Date date = new Date();
        when(client.getTime(Paths.get("/remote/file"))).thenReturn(date);
        commandLineClient.command("gettime /remote/file");
        verify(client).getTime(Paths.get("/remote/file"));
        assertThat(log.toString(), startsWith(date.toString()));
    }

    @Test
    public void shouldCallList() throws TjfsClientException {
        String[] content = {"/remote/folder/b", "/remote/folder/a", "/remote/folder/folder/"};
        when(client.list(Paths.get("/remote/folder"))).thenReturn(content);
        commandLineClient.command("list /remote/folder");
        verify(client).list(Paths.get("/remote/folder"));
        assertThat(log.toString(), startsWith("folder/\na\nb"));
    }

    @Test
    public void shouldChangeWorkingDirectory() throws TjfsClientException {
        // Define simple folder structure
        when(client.list(Paths.get("/"))).thenReturn(
            new String[] {"/remote/"});
        when(client.list(Paths.get("/remote"))).thenReturn(
            new String[] {"/remote/folder/"});
        when(client.list(Paths.get("/remote/folder"))).thenReturn(
            new String[] {"/remote/folder/b", "/remote/folder/a", "/remote/folder/folder/"});
        when(client.list(Paths.get("/remote/folder/b"))).thenReturn(
            new String[] {});

        commandLineClient.command("cd remote");
        assertThat(commandLineClient.getWorkingDirectory().toString(), equalTo("/remote"));
        verify(client).list(Paths.get("/remote"));

        commandLineClient.command("cd folder");
        assertThat(commandLineClient.getWorkingDirectory().toString(), equalTo("/remote/folder"));

        commandLineClient.command("cd /");
        assertThat(commandLineClient.getWorkingDirectory().toString(), equalTo("/"));

        commandLineClient.command("cd /remote/folder/b");
        assertThat(commandLineClient.getWorkingDirectory().toString(), equalTo("/"));
        assertThat(log.toString(), startsWith("Directory does not exist"));

        commandLineClient.command("cd /remote/folder");
        commandLineClient.command("cd ../");
        assertThat(commandLineClient.getWorkingDirectory().toString(), equalTo("/remote"));

        commandLineClient.command("cd .");
        commandLineClient.command("cd ./");
        assertThat(commandLineClient.getWorkingDirectory().toString(), equalTo("/remote"));

        commandLineClient.command("cd ..");
        assertThat(commandLineClient.getWorkingDirectory().toString(), equalTo("/"));

        commandLineClient.command("cd ..");
        assertThat(commandLineClient.getWorkingDirectory().toString(), equalTo("/"));
    }

    @Test
    public void shouldCdAndGet() throws IOException, TjfsClientException {
        when(client.list(Paths.get("/remote"))).thenReturn( new String[] {"/remote/source"});

        commandLineClient.command("cd remote");
        commandLineClient.command("get source /local/target");
        verify(client).get(Paths.get("/remote/source"), Paths.get("/local/target"));
    }
}