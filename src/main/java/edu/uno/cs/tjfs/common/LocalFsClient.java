package edu.uno.cs.tjfs.common;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.*;

public class LocalFsClient implements ILocalFsClient {

    public InputStream readFile(Path path) throws IOException {
        return Files.newInputStream(path);
    }

    public void writeFile(Path path, InputStream data) throws IOException {
        try {
            Files.copy(data, path, REPLACE_EXISTING);
        } catch (IOException e) {
            throw e;
        } finally {
            data.close();
        }
    }

    @Override
    public byte[] readBytesFromFile(Path path) throws IOException {
        return IOUtils.toByteArray(Files.newInputStream(path));
    }

    @Override
    public void writeBytesToFile(Path path, byte[] data) throws IOException {
        FileOutputStream outStream = new FileOutputStream(path.toString());

        outStream.write(data);
    }

    @Override
    public String[] list(Path path) {
        File folder = new File(path.toString());
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null) {
            return new String[0];
        }

        return Arrays.asList(listOfFiles).stream()
            .map(File::getName)
            .collect(Collectors.toList())
            .toArray(new String[listOfFiles.length]);
    }

    @Override
    public void mkdir(Path path) throws IOException {
        File folder = path.toFile();
        if (!folder.mkdir()) {
            throw new IOException("Unable to create the folder");
        }
    }

    @Override
    public boolean exists(Path path) {
        return path.toFile().exists();
    }

    @Override
    public void deleteFile(Path path) throws IOException {
        Files.delete(path);
    }
}
