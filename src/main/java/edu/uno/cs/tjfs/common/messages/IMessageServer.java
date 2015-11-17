package edu.uno.cs.tjfs.common.messages;

import edu.uno.cs.tjfs.common.Machine;

public interface IMessageServer {
    void listen(Machine machine);
}
