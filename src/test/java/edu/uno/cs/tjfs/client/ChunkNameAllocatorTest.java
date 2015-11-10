package edu.uno.cs.tjfs.client;

import edu.uno.cs.tjfs.common.ChunkDescriptor;
import edu.uno.cs.tjfs.common.DummyMasterClient;
import edu.uno.cs.tjfs.common.IMasterClient;
import edu.uno.cs.tjfs.common.TjfsException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class ChunkNameAllocatorTest {
    private ChunkNameAllocator allocator;

    @Before
    public void setUp() {
        IMasterClient masterClient = new DummyMasterClient();
        allocator = new ChunkNameAllocator(masterClient, 3);
    }

    @Test
    public void testGetOne() throws TjfsException {
        for (int i = 0; i < 10; i++) {
            ChunkDescriptor name = allocator.getOne();
            assertThat(name.name, equalTo("" + i)); // DummyMasterClient uses numbers as chunk names
            assertThat(name.chunkServers.size(), equalTo(2));
            assertThat(name.chunkServers.get(0), not(equalTo(name.chunkServers.get(1))));
        }

        // Just check that the correct number of names is still left in the buffer
        assertThat(allocator.allocated.size(), equalTo(((10 / 3) + 1) * 3 - 10));
    }

}