package edu.uno.cs.tjfs.common;

import edu.uno.cs.tjfs.common.messages.IMessageClient;
import edu.uno.cs.tjfs.common.messages.MCommand;
import edu.uno.cs.tjfs.common.messages.Request;
import edu.uno.cs.tjfs.common.messages.arguments.GetChunkRequestArgs;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class ChunkClientTest {
    private ChunkClient chunkClient;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Mock
    private IMessageClient messageClient;

    @Captor
    private ArgumentCaptor<Machine> machineCaptor;

    @Captor
    private ArgumentCaptor<Request> requestCaptor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        this.chunkClient = new ChunkClient(messageClient);
    }

    @Test
    public void shouldFailWithNoChunkServers() throws TjfsException{
        exception.expect(TjfsException.class);
        exception.expectMessage("Invalid number of chunk-severs.");
        this.chunkClient.get(new ChunkDescriptor("0", new LinkedList<Machine>(), 3, 0));
    }

    @Test
    public void shouldCallMessageClientGet() throws TjfsException, IOException{
        Machine testMachine1 =new Machine("some test ip 1", 2125);
        Machine testMachine2 =new Machine("some test ip 1", 2126);
        try {
            this.chunkClient.get(new ChunkDescriptor("0", new LinkedList<>(Arrays.asList(
                    testMachine1, testMachine2)), 3, 0));
        }catch(TjfsException | NullPointerException e) {
            verify(messageClient, atLeast(2)).send(machineCaptor.capture(), requestCaptor.capture());
            assertTrue(checkMachineEquality(machineCaptor.getAllValues().get(0), testMachine1));
            assertTrue(checkMachineEquality(machineCaptor.getAllValues().get(1), testMachine2));

            Request expectedRequest = new Request(MCommand.GET_CHUNK, new GetChunkRequestArgs("0"));

            assertTrue(checkRequestEquality(requestCaptor.getAllValues().get(0), expectedRequest));
            assertTrue(checkRequestEquality(requestCaptor.getAllValues().get(1), expectedRequest));
        }
    }

    @Test
    public void shouldCallMessageClientPut() throws TjfsException, IOException{
        Machine testMachine1 =new Machine("some test ip 1", 2125);
        Machine testMachine2 =new Machine("some test ip 1", 2126);
        ChunkDescriptor testChunkDescriptor =
                new ChunkDescriptor("0", new LinkedList<>(Arrays.asList(testMachine1, testMachine2)));
        try {
            this.chunkClient.get(testChunkDescriptor);
        }catch(TjfsException | NullPointerException e) {
            verify(messageClient, atLeast(2)).send(machineCaptor.capture(), requestCaptor.capture());
            assertTrue(checkMachineEquality(machineCaptor.getAllValues().get(0), testMachine1));
            assertTrue(checkMachineEquality(machineCaptor.getAllValues().get(1), testMachine2));

            Request expectedRequest = new Request(MCommand.GET_CHUNK, new GetChunkRequestArgs("0"));

            assertTrue(checkRequestEquality(requestCaptor.getAllValues().get(0), expectedRequest));
            assertTrue(checkRequestEquality(requestCaptor.getAllValues().get(1), expectedRequest));
        }
    }

    private Boolean checkRequestEquality(Request request1, Request request2) throws IOException{
        if (request1.header != request2.header) return false;
        else if (request1.args.getClass() != request2.header.requestClass) return false;
        else if (request1.data != null && request2.data != null &&
                !Arrays.equals(request1.data, request2.data)) return false;
        else if (request1.dataLength != request2.dataLength) return false;
        return true;
    }

    private boolean checkMachineEquality(Machine machine1, Machine machine2) {
        return machine1.ip.equals(machine2.ip) && machine1.port == machine2.port;
    }
}