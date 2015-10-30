# TJFS

Distributed file system by Janak Dahal and Tobias Potocek

## How to run the client

```bash
mvn package
cd target
java -cp tjfs.jar edu.uno.cs.tjfs.client.Launcher
```

Or you can pipe in a file with list of commands to be executed.

```bash
java -cp tjfs.jar edu.uno.cs.tjfs.client.Launcher -piped < commands
```

## Consistency model

Concurrent reads are allowed. Writing to a file will lock to file and nobody will be allowed to 
either read or write from the file until it's finished.

## Architecture draft

TJFS will be basically just a key-value storage, there is no *directory* concept. The *key* is 
the path to a file and the *value* is the file content itself. The path will be allowed to 
contain slashes, and tokens between the slashes will be interpreted as directories. Our fs won't 
implement any security model so having a *directory* as a real object is not necessary in our 
case. Turning the whole fs into flat key-value storage (instead of a hierarchy) will simplify the
code.

Each file will consist of chunks (blocks) of equal size. There will always be 2 copies of each 
chunk in our filesystem to maintain the fault tolerance.

Two types of servers:

* **Master servers**. They will contain the meta-data (key-value map with file paths and chunk 
mappings, location of chunks). We will have 2 master servers running in sync to maintain the fault 
tolerance.
* **Chunk servers**. They will contain the chunks. We will have an arbitrary number of chunk 
servers, but always at least 3. (If one goes down and we start the replication, for each chunk we 
are able to find a chunk server that does not contain that particular chunk.)