package edu.uno.cs.tjfs.chunkserver;

import edu.uno.cs.tjfs.common.TjfsException;
import edu.uno.cs.tjfs.common.messages.ChunkClientMessage;
import edu.uno.cs.tjfs.common.messages.ClientChunkMessage;
import edu.uno.cs.tjfs.common.messages.IMessageServer;
import edu.uno.cs.tjfs.common.messages.SocketMessage;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.mockito.Captor;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Created by srjanak on 11/6/15.
 */
public class ChunkServerClientTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    /* Instance of the chunk server client to be tested */
    private ChunkServerClient chunkServerClient;

    @Captor
    private ArgumentCaptor<SocketMessage> captor;

    @Mock
    private IMessageServer messageServer;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        this.captor = ArgumentCaptor.forClass(SocketMessage.class);
        this.chunkServerClient = new ChunkServerClient(messageServer);
    }

    @Test
    public void shouldCallSendWithPutSocketMessage() throws Exception {
        ClientChunkMessage clientChunkMessage = new ClientChunkMessage(
                ClientChunkMessage.ClientChunkMessageType.PUT, "testChunk");
        SocketMessage sendMessage = new SocketMessage("1300", clientChunkMessage);

        exception.expect(TjfsException.class);
        chunkServerClient.put("testChunk", 0, null);

        verify(messageServer).send(captor.capture());
        assertEquals(sendMessage, captor.getValue());
    }

    @Test
    public void shouldReturnSocketMessageForPut() throws Exception {
        ClientChunkMessage clientChunkMessage = new ClientChunkMessage(
                ClientChunkMessage.ClientChunkMessageType.PUT, "testChunk");
        SocketMessage sendMessage = new SocketMessage("1300", clientChunkMessage);

        ChunkClientMessage chunkClientMessage = new ChunkClientMessage("");
        SocketMessage returnMessage = new SocketMessage("3100", chunkClientMessage);

        when(messageServer.send(sendMessage)).thenReturn(returnMessage);
        chunkServerClient.put("testChunk", 0, null);//This should not throw any exception

        chunkClientMessage = new ChunkClientMessage("some error some server");
        returnMessage = new SocketMessage("3100", chunkClientMessage);

        when(messageServer.send(sendMessage)).thenReturn(returnMessage);
        exception.expect(TjfsException.class);
        chunkServerClient.put("testChunk", 0, null);//This should throw an exception
    }


}
