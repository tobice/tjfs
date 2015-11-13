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
        chunkServers.add(new Machine("127.0.0.1", 8000));
        chunkServers.add(new Machine("127.0.0.1", 8001));
        chunkServers.add(new Machine("127.0.0.1", 8002));
        chunkServers.add(new Machine("127.0.0.1", 8003));
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

    private List<Machine> getRandomChunkServers(int number) {
        Collections.shuffle(chunkServers);
        return new LinkedList<>(chunkServers.subList(0, number));
    }
}
