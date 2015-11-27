package edu.uno.cs.tjfs.client;

import edu.uno.cs.tjfs.Config;
import edu.uno.cs.tjfs.SoftConfig;
import edu.uno.cs.tjfs.common.*;
import edu.uno.cs.tjfs.common.messages.IMessageClient;
import edu.uno.cs.tjfs.common.messages.MessageClient;
import edu.uno.cs.tjfs.common.zookeeper.ZookeeperException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Launcher {
    protected static CommandLineClient cmdClient;

    public static void main(String[] args) throws IOException, ZookeeperException {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.out.println("Terminated! Cleaning up...");
            }
        });

        // If the commands are piped in (changes the output slightly)
        boolean piped = args.length > 0 && args[0].equals("-piped");

        // TODO: get the Zookeeper address from the arguments
        Machine zookeeper = Machine.fromString("137.30.122.138:2181");

        // Instantiate the command line client
        cmdClient = CommandLineClient.getInstance(new Config(), zookeeper, System.out);

        InputStreamReader converter = new InputStreamReader(System.in);
        BufferedReader in = new BufferedReader(converter);

        String line;
        printWorkingDirectory();
        while ((line = in.readLine()) != null) {
            if (piped) {
                // If the stdin is piped in, let's print the command that's being processed
                System.out.println(line);
            }
            cmdClient.command(line);
            printWorkingDirectory();
        }
    }

    public static void printWorkingDirectory() {
        System.out.print("tjfs " + cmdClient.getWorkingDirectory() + " > ");
    }
}
