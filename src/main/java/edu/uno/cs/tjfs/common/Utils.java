package edu.uno.cs.tjfs.common;

import java.util.Arrays;

public class Utils {
    /**
     * Return byte offset within the file corresponding to the given chunk number.
     * @param chunkIndex number of the chunk within the file (like first, second, third)
     * @param chunkSize size of one size
     * @return byte offset of given chunk within a file
     */
    public static int getChunkOffset(int chunkIndex, int chunkSize) {
        return chunkIndex * chunkSize;
    }

    /**
     * Return chunk number for given byte offset, i. e. to which chunk given byte belongs
     * @param byteOffset byte offset we want to find the chunk number for
     * @param chunkSize size of one chunk
     * @return number of the chunk which contains given by
     */
    public static int getChunkIndex(int byteOffset, int chunkSize) {
        return byteOffset / chunkSize;
    }

    /**
     * Copies new data onto the old data at given byte offset. If necessary, extends the array.
     * @param oldData old chunk data
     * @param newData new chunk data
     * @param byteOffset the offset at which the new data should be written
     * @return combined results
     */
    public static byte[] mergeChunks(byte[] oldData, byte[] newData, int byteOffset) {
        int length = Math.max(oldData.length, byteOffset + newData.length);
        byte[] result = Arrays.copyOf(oldData, length);
        System.arraycopy(newData, 0, result, byteOffset, newData.length);
        return result;
    }
}
