package edu.uno.cs.tjfs.client;

import edu.uno.cs.tjfs.common.TjfsException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ClosedChannelException;
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
     * Chop off another chunk from the data stream. Each chunk will be long given number
     * of bytes, except for the last chunk which might be shorter.
     * @param length of the chunk to be chopped of
     * @return byte array or null if no more data is available.
     * @throws TjfsException
     */
    public byte[] chopNext(int length) throws TjfsException {
        try {
            byte[] chunk = new byte[length];
            int read = stream.read(chunk, 0, length);

            if (read == -1) {
                return null;
            }

            if (read < length) {
                stream.close();
                return Arrays.copyOfRange(chunk, 0, read);
            } else {
                return chunk;
            }
        }
        catch(ClosedChannelException e) {
            // TODO: test
            return null;
        } catch (IOException e) {
            throw new TjfsException("Chopping chunk from the data stream failed. ", e);
        }
    }

    /**
     * Chop off another chunk from the data stream. This chunk will be exactly chunk size long,
     * except for the last chunk which might be shorter.
     * @return byte array or null if no more data is available.
     * @throws TjfsException
     */
    public byte[] chopNext() throws TjfsException {
        return chopNext(chunkSize);
    }
}
