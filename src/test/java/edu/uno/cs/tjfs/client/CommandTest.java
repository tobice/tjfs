package edu.uno.cs.tjfs.client;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class CommandTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldDetectCommand() throws CommandFormatException {
        assertThat(CommandName.GET, equalTo(Command.parse("get /remote /local").name));
        assertThat(CommandName.PUT, equalTo(Command.parse("put /local /remote").name));
        assertThat(CommandName.DELETE, equalTo(Command.parse("delete /remote").name));
        assertThat(CommandName.GET_SIZE, equalTo(Command.parse("getsize /remote").name));
        assertThat(CommandName.GET_TIME, equalTo(Command.parse("gettime /remote").name));
        assertThat(CommandName.LIST, equalTo(Command.parse("list /remote/folder").name));
        assertThat(CommandName.CD, equalTo(Command.parse("cd /remote/folder").name));
    }

    @Test
    public void shouldFailOnUnknownCommand() throws CommandFormatException {
        exception.expect(CommandFormatException.class);
        exception.expectMessage("Unknown command poo");
        Command.parse("poo arg1 arg2");
    }
}