package edu.uno.cs.tjfs.client;

import edu.uno.cs.tjfs.common.ILocalFsClient;
import edu.uno.cs.tjfs.common.LocalFsClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Launcher {
    protected static ITjfsClient tjfsClient;
    protected static ILocalFsClient localFsClient;
    protected static IClient client;
    protected static CommandLineClient cmdClient;

    public static void main(String[] args) throws IOException {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.out.println("Terminated! Cleaning up...");
            }
        });

        // If the commands are piped in (changes the output slightly)
        boolean piped = args.length > 0 && args[0].equals("-piped");

        // Create instance of tjfs client that will connect to the remote filesystem
        tjfsClient = new DummyTjfsClient();

        // Create instance of local fs client to gain access to local filesystem
        localFsClient = new LocalFsClient();

        // Create instance of the wrapping client that will move stuff between local and remote fs
        client = new Client(tjfsClient, localFsClient);

        // Finally, wrap the client by the cmd client to parse commands for us
        cmdClient = new CommandLineClient(client, System.out);

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
