package edu.uno.cs.tjfs.common.messages;

import edu.uno.cs.tjfs.client.TjfsClientException;
import edu.uno.cs.tjfs.common.Machine;
import edu.uno.cs.tjfs.common.messages.arguments.GetChunkRequestArgs;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MessageClientTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void sendTest() throws BadRequestException, BadResponseException, ConnectionFailureException, TjfsClientException{
        //There is no such machine
        Machine machine = new Machine("", 0);
        Request request = new Request(MCommand.GET_CHUNK, new GetChunkRequestArgs("someChunk"));

        MessageClient client = new MessageClient();

        exception.expect(ConnectionFailureException.class);
        client.send(machine, request);
    }
}
