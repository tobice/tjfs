package edu.uno.cs.tjfs.client;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CommandLineClient {

    private IClient client;

    /** Current remote path in Tjfs */
    private Path pwd;

    /** Output stream used for printing log messages */
    private PrintStream log;

    public CommandLineClient(IClient client, PrintStream log) {
        this.client = client;
        this.log = log;
    }

    public void command(String line) {
        try {
            Command command = Command.parse(line);

            switch (command.name) {
                case GET:
                    if (command.arguments.length < 1) {
                        throw new CommandFormatException("Please specify remote file");
                    }
                    if (command.arguments.length < 2) {
                        throw new CommandFormatException("Please specify local target");
                    }

                    Path source = pwd.resolve(command.arguments[0]);
                    Path target = Paths.get(command.arguments[1]);
                    client.get(source, target);
                    break;

                case PUT:
                    log.println("To be implemented");
                    break;

                case DELETE:
                    log.println("To be implemented");
                    break;

                case GET_SIZE:
                    log.println("To be implemented");
                    break;

                case GET_TIME:
                    log.println("To be implemented");
                    break;

                case LIST:
                    log.println("To be implemented");
                    break;

                case CD:
                    log.println("To be implemented");
                    break;
            }

        } catch (CommandFormatException e) {
            log.println("Invalid command. " + e.getMessage());
        } catch (TjfsClientException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
