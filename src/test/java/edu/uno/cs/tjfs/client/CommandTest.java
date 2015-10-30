package edu.uno.cs.tjfs.client;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static edu.uno.cs.tjfs.client.Command.Name.*;

public class CommandTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldDetectCommand() throws CommandFormatException {
        assertThat(GET, equalTo(Command.parse("get /remote /local").name));
        assertThat(PUT, equalTo(Command.parse("put /local /remote").name));
        assertThat(DELETE, equalTo(Command.parse("delete /remote").name));
        assertThat(GET_SIZE, equalTo(Command.parse("getsize /remote").name));
        assertThat(GET_TIME, equalTo(Command.parse("gettime /remote").name));
        assertThat(LIST, equalTo(Command.parse("list /remote/folder").name));
        assertThat(CD, equalTo(Command.parse("cd /remote/folder").name));
    }

    @Test
    public void shouldFailOnUnknownCommand() throws CommandFormatException {
        exception.expect(CommandFormatException.class);
        exception.expectMessage("Unknown command poo");
        Command.parse("poo arg1 arg2");
    }
}