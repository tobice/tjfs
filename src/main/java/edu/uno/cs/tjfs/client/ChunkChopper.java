package edu.uno.cs.tjfs.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Takes an incoming input stream and chops the stream into chunks of predefined size
 */
public class ChunkChopper {
    private final int chunkSize;
    private final InputStream stream;

    public ChunkChopper(int chunkSize, InputStream stream) {
        this.chunkSize = chunkSize;
        this.stream = stream;
    }

    /**
     * Chop off another chunk from the data stream. Each chunk will be long the predefined number
     * of bytes, except for the last chunk which might be shorter.
     * @return byte array or null if no more data is available.
     * @throws IOException
     */
    public byte[] chopNext() throws IOException {
        byte[] chunk = new byte[chunkSize];
        int read = stream.read(chunk, 0, chunkSize);

        if (read == -1) {
            return null;
        }

        if (read < chunkSize) {
            stream.close();
            return Arrays.copyOfRange(chunk, 0, read);
        } else {
            return chunk;
        }
    }
}
