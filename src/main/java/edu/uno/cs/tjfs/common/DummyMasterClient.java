package edu.uno.cs.tjfs.common;

import java.nio.file.Path;
import java.util.*;

public class DummyMasterClient implements IMasterClient {

    private int chunkCounter = 0;

    /** List of available chunk servers */
    private List<Machine> chunkServers = new LinkedList<>();

    /** Map of existing chunks */
    private Map<String, ChunkDescriptor> chunks = new HashMap<>();

    /** Map of existing files */
    private Map<Path, FileDescriptor> files = new HashMap<>();

    public DummyMasterClient() {
        chunkServers.add(new Machine("192.168.43.27", 6002));
        chunkServers.add(new Machine("192.168.43.218", 6002));
    }

    @Override
    public List<ChunkDescriptor> allocateChunks(int number) throws TjfsException {
        List<ChunkDescriptor> allocated = new LinkedList<>();
        ChunkDescriptor chunkDescriptor;
        for (int i = 0; i < number; i++) {
            chunkDescriptor = new ChunkDescriptor("" + (chunkCounter++), getRandomChunkServers(2));
            allocated.add(chunkDescriptor);
            chunks.put(chunkDescriptor.name, chunkDescriptor);
        }
        return allocated;
    }

    @Override
    public FileDescriptor getFile(Path path) throws TjfsException {
        FileDescriptor descriptor = files.get(path);
        return descriptor == null ? new FileDescriptor(path) : descriptor;
    }

    @Override
    public void putFile(FileDescriptor file) throws TjfsException {
        files.put(file.path, file);
    }

    @Override
    public List<FileDescriptor> getLog(int logID) throws TjfsException {
        return null;
    }

    private List<Machine> getRandomChunkServers(int number) {
        Collections.shuffle(chunkServers);
        return new LinkedList<>(chunkServers.subList(0, number));
    }
}
