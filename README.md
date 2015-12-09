# TJFS

Google FS based distributed file system written by [Tobiáš Potoček](mailto:tobiaspotocek@gmail.com) 
and [Janak Dahal](mailto:jdahaldev@gmail.com) as a term project for CSCI 6450 at the [University
of New Orleans](http://www.uno.edu). 

## Overview

* Vaguely based on [Google File System](https://en.wikipedia.org/wiki/Google_File_System) architecture
* Written completely in Java
* **Fully JUnit tested**
* Fault-tolerance: **able to survive crash of any single computer within the cluster without data loss**.
* One *master* server that holds the metadata.
* One *shadow server* that mirrors the master server.
* Couple of *chunk servers* that hold the actual data stored in chunks.
* Synchronized by [Apache Zookeeper](https://zookeeper.apache.org/)
* Dedicated clients
* Each file is divided into **chunks** of a fixed size (currently **16MB**).
* Each chunks stored on at least two chunk servers (to maintain the fault tolerance guarantee)
* Performance-wise, the file system is capable of transferring data on a local **1 Gb network** at rates up to **900 Mb/s**.

**Do you want to know more?** Check out our **[final presentation](doc/tjfs-presentation.pdf)** 
that describes in detail individual components of the filesystem.

## How to run tfjs (Linux/Mac only)

The file system is bundled with a simple command line client that exposes basic functionality to 
the user (writing and reading files from the file system).

1. [Download Zookeeper](http://www.apache.org/dyn/closer.cgi/zookeeper/) (tjfs is tested with the
 3.4.6 version)
2. Unpack the archive, go to the `conf` folder and rename `zoo_sample.cfg` to `zoo.cfg`
3. Go to the `bin` folder and under root run `zkServer.sh`. You can start `zkCli.sh` as well to 
verify that the Zookeeper server is up and running. More details about this procedure can be found
in the project's documentation.
4. Download tjfs codebase to all machines where you want to run the file system.
5. Run `mvn package` to build the code. Java JDK and Maven are required for that.
6. Go to the `bin` folder and run `chunkServer.sh` on every machine where you want to run a 
chunk server. As the first argument, you can pass Zookeeper connection string (ip:port), as the 
second the port, on which this chunk server should run, and the last argument defines a folder where
the chunk server should store the data. By default, the Zookeeper is expected to run on 
localhost, chunk server will be running on the port `6003` and the data will be stored to the 
`chunks` folder in the root of the code base. If you're running multiple chunk servers on a 
single machine, don't forget to define different ports and different folders. Don't forget to create
the folder before running the filesystem.
7. Got to the `bin` folder again and run `masterServer.sh` in at least two separate instances. 
The arguments are exactly the same as for `chunkServer.sh`. The important thing is to provide the
correct Zookeeper address. After that the servers will find each other.
8. Go to the `bin` folder once again and run `client.sh` with the Zookeeper address as the first
argument. You should be successfully connected and you can start using the filesystem.

Basic commands:
* `put /path/to/a/local/file /path/to/a/remote/file` write a file to a remote destination
* `get /path/to/a/remote/file /path/to/a/local/file` get a file from the file system
* `list /remote/directory` list all files in a remote directory
* `delete /remote/file` delete file from the file system

Folders are supported only virtually through the structured paths, it is neither possible nor
required to create or delete folders. 

**Disclaimer: This is a proof-of-concept school project. It is not meant by any means to be used in 
production.** Also check out the presentation for any limitation that the current version has.

## How to use tjfs in your code

Tjfs provides a nice and clean API that can be used in your own application. The API is 
completely stream-based which means that you are not limited to file operations (you can for 
example generate data on-the-fly and immediately write them to tjfs). Sample code:

```java
Machine zookeeper = Machine.fromString("127.0.0.1:2181");
TjfsClient tjfsClient = TjfsClient.getInstance(new Config(), zookeeper);
InputStream is = tjfsClient.get(Paths.get("/path/to/remote/file"));
```

The API is defined and described in [ITjfsClient]
(src/main/java/edu/uno/cs/tjfs/client/ITjfsClient.java) interface. The actual implementation is 
in [TjfsClient](src/main/java/edu/uno/cs/tjfs/client/TjfsClient.java) and for testing purposes
you can use a mocked version [DummyTjfsClient]
(src/main/java/edu/uno/cs/tjfs/client/DummyTjfsClient.java) that mimics the whole file system
using a simple in-memory storage.
