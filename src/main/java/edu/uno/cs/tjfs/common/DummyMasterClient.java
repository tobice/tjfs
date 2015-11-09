package edu.uno.cs.tjfs.common;

import edu.uno.cs.tjfs.client.AllocatedChunkName;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class DummyMasterClient implements IMasterClient {

    private int chunkCounter = 0;
    private List<Machine> chunkServers = new LinkedList<>();

    public DummyMasterClient() {
        chunkServers.add(new Machine("127.0.0.1", 8000));
        chunkServers.add(new Machine("127.0.0.1", 8001));
        chunkServers.add(new Machine("127.0.0.1", 8002));
        chunkServers.add(new Machine("127.0.0.1", 8003));
    }

    @Override
    public List<AllocatedChunkName> allocateChunks(int number) throws TjfsException {
        List<AllocatedChunkName> allocated = new LinkedList<>();
        for (int i = 0; i < number; i++) {
            allocated.add(new AllocatedChunkName("" + (chunkCounter++), getRandomChunkServers(2)));
        }
        return allocated;
    }

    private List<Machine> getRandomChunkServers(int number) {
        Collections.shuffle(chunkServers);
        return new LinkedList<>(chunkServers.subList(0, number));
    }
}
