package edu.uno.cs.tjfs.common;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class MachineTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

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

    @Test
    public void testToString() {
        Machine machine1 = new Machine("127.0.0.1", 8000);
        assertThat(machine1.toString(), equalTo("127.0.0.1:8000"));
    }

    @Test
    public void testGetBytes() {
        Machine machine1 = new Machine("127.0.0.1", 8000);
        assertThat(machine1.getBytes(), equalTo("127.0.0.1:8000".getBytes()));
    }

    @Test
    public void testFromString() {
        Machine machine1 = Machine.fromString("127.0.0.1:8000");
        assertThat(machine1.ip, equalTo("127.0.0.1"));
        assertThat(machine1.port, is(8000));

        Machine machine2 = Machine.fromString("localhost:8000");
        assertThat(machine2.ip, equalTo("localhost"));
        assertThat(machine2.port, is(8000));

        Machine machine3 = Machine.fromString("[::1]:8000");
        assertThat(machine3.ip, equalTo("[::1]"));
        assertThat(machine3.port, is(8000));

        exception.expect(IllegalArgumentException.class);
        Machine machine4 = Machine.fromString("127.0.0.1");
    }

    @Test
    public void testFromBytes() {
        Machine machine1 = Machine.fromBytes("127.0.0.1:8000".getBytes());
        assertThat(machine1.ip, equalTo("127.0.0.1"));
        assertThat(machine1.port, is(8000));
    }
}
