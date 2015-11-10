package edu.uno.cs.tjfs.common;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class MachineTest {

    @Test
    public void testEqual() {
        Machine machine1 = new Machine("127.0.0.1", 8000);
        Machine machine2 = new Machine("127.0.0.1", 8000);
        Machine machine3 = new Machine("127.0.0.2", 8000);
        Machine machine4 = new Machine("127.0.0.1", 8001);

        assertThat(machine1, equalTo(machine2));
        assertThat(machine1, not(equalTo(machine3)));
        assertThat(machine1, not(equalTo(machine4)));
    }
}
