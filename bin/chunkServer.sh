#!/bin/bash
if [ ! -f ../target/tjfs.jar ]; then
    echo "Missing ../target/tjfs.jar file. Run mvn package first";
    exit 1;
fi;

# Default values
ZOOKEEPER="127.0.0.1:2181"
PORT=6002
STORAGE="../chunks"

if [ "$1" ]; then ZOOKEEPER=$1; fi
if [ "$2" ]; then PORT=$2; fi
if [ "$3" ]; then STORAGE=$3; fi

java -cp ../target/tjfs.jar edu.uno.cs.tjfs.chunkserver.ChunkLauncher "$ZOOKEEPER" "$PORT" "$STORAGE"
