package edu.uno.cs.tjfs.common;

import org.apache.commons.io.IOUtils;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

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

    public static void writeFileT(Path path, InputStream inputStream, int dataLength) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(path.toString());

        try {
            int read = 0;
            byte[] bytes = new byte[1];
            int counter = 0;
            while (counter < dataLength && (read = inputStream.read(bytes)) != -1) {
                //BaseLogger.info("wrote to file");
                outputStream.write(bytes);
                counter++;
            }
        } catch (IOException e) {
            throw e;
        } finally {
//            if (inputStream != null) {
//                try {
//                    inputStream.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
            if (outputStream != null) {
                try {
                    // outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }


}
