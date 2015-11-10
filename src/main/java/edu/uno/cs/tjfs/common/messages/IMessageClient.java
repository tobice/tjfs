package edu.uno.cs.tjfs.common.messages;

import edu.uno.cs.tjfs.common.Machine;

public interface IMessageClient {
    Response send(Machine machine, Request request) throws BadRequestException, BadResponseException, ConnectionFailureException;
    void sendAsync(Machine machine, Request request) throws BadRequestException, ConnectionFailureException;
}
