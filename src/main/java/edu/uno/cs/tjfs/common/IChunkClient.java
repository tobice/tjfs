package edu.uno.cs.tjfs.common;

import edu.uno.cs.tjfs.common.Machine;
import edu.uno.cs.tjfs.common.TjfsException;

import java.io.IOException;
import java.io.InputStream;

public interface IChunkClient {
    InputStream get(Machine machine, String name) throws TjfsException;
    void put(Machine machine, String name, int length, InputStream data) throws TjfsException;
    void delete(Machine machine, String name) throws TjfsException;
    String[] list(Machine machine) throws TjfsException;
}