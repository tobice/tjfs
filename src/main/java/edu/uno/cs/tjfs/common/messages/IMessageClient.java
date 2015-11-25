package edu.uno.cs.tjfs.common.messages;

import edu.uno.cs.tjfs.client.TjfsClientException;
import edu.uno.cs.tjfs.common.Machine;

public interface IMessageClient {
    Response send(Machine machine, Request request) throws BadRequestException, BadResponseException, ConnectionFailureException, TjfsClientException;
    void sendAsync(Machine machine, Request request) throws BadRequestException, ConnectionFailureException;
}
