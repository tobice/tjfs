package edu.uno.cs.tjfs.client;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.verify;
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
}