package edu.uno.cs.tjfs.common;

import edu.uno.cs.tjfs.common.messages.*;
import edu.uno.cs.tjfs.common.messages.arguments.*;

import java.nio.file.Path;
import java.util.List;

public class MasterClient implements IMasterClient {
    private MessageClient messageClient;
    private Machine machine;

    public MasterClient(MessageClient messageClient, Machine machine) {
        this.messageClient = messageClient;
        this.machine = machine;
    }

    @Override
    public List<ChunkDescriptor> allocateChunks(int number) throws TjfsException {
        Request request = new Request(MCommand.ALLOCATE_CHUNKS, new AllocateChunksRequestArgs(number), null, 0);
        Response response = this.messageClient.send(machine, request);
        if (response == null) {
            throw new TjfsException("No response from server.");
        } else if (response.code == MCode.ERROR) {
            throw new TjfsException(((AllocateChunkResponseArgs) response.args).status);
        } else {
            return ((AllocateChunkResponseArgs) response.args).chunks;
        }
    }

    @Override
    public FileDescriptor getFile(Path path) throws TjfsException {
        Request request = new Request(MCommand.ALLOCATE_CHUNKS, new GetFileRequestArgs(path), null, 0);
        Response response = this.messageClient.send(machine, request);
        if (response == null) {
            throw new TjfsException("No response from server.");
        } else if (response.code == MCode.ERROR) {
            throw new TjfsException(((GetFileResponseArgs) response.args).status);
        } else {
            return ((GetFileResponseArgs) response.args).file;
        }
    }

    @Override
    public void putFile(FileDescriptor file) throws TjfsException {
        Request request = new Request(MCommand.ALLOCATE_CHUNKS, new PutFileRequestArgs(file), null, 0);
        Response response = this.messageClient.send(machine, request);
        if (response == null) {
            throw new TjfsException("No response from server.");
        } else if (response.code == MCode.ERROR) {
            throw new TjfsException(((PutFileResponseArgs) response.args).status);
        }
    }
}