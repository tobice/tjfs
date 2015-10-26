package edu.uno.cs.tjfs.common;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.rmi.server.ExportException;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class LocalFsClientTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private static final byte[] data1 = "test string".getBytes();

    private LocalFsClient client = new LocalFsClient();

    @Test
    public void shouldWriteAndReadFile() throws IOException {
        InputStream stream = new ByteArrayInputStream(data1);
        Path path = folder.getRoot().toPath().resolve("test");
        client.writeFile(path, stream);
        assertThat(data1, equalTo(IOUtils.toByteArray(client.readFile(path))));
    }

    @Test
    public void shouldFailOnNonExistingFile() throws IOException {
        Path path = folder.getRoot().toPath().resolve("test");
        exception.expect(NoSuchFileException.class);
        client.readFile(path);
    }
}