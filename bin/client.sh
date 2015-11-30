#!/bin/bash
if [ ! -f ../target/tjfs.jar ]; then
    echo "Missing ../target/tjfs.jar file. Run mvn package first";
    exit 1;
fi;

# Default values
ZOOKEEPER="127.0.0.1:2181"
PIPED=""

if [ "$1" ]; then ZOOKEEPER=$1; fi
if [ "$2" ]; then PIPED=$2; fi

java -cp ../target/tjfs.jar edu.uno.cs.tjfs.client.ClientLauncher "$ZOOKEEPER" "$PIPED"
