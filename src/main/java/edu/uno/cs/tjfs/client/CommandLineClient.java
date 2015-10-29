package edu.uno.cs.tjfs.client;

import org.apache.commons.cli.*;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import static edu.uno.cs.tjfs.client.Command.Name.*;

public class CommandLineClient {

    private IClient client;

    /** Current remote path in Tjfs */
    private Path pwd;

    /** Output stream used for printing log messages */
    private PrintStream log;

    /** Apache Commons CLI parses for parsing command arguments */
    private CommandLineParser parser = new BasicParser();

    public CommandLineClient(IClient client, PrintStream log) {
        this.client = client;
        this.log = log;
        this.pwd = Paths.get("/");
    }

    public void command(String line) {
        try {
            Command command = Command.parse(line);

            switch (command.name) {
                case GET:
                    get(command);
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

        } catch (CommandFormatException | ParseException e) {
            log.println("Invalid command. " + e.getMessage());
            e.printStackTrace(log);
        } catch (TjfsClientException e) {
            log.println("Tjfs client failure. " + e.getMessage());
            e.printStackTrace(log);
        } catch (IOException e) {
            log.println("Local IO failure. " + e.getMessage());
            e.printStackTrace(log);
        }
    }

    private void get(Command command) throws CommandFormatException, IOException, TjfsClientException, ParseException {
        if (command.arguments.length < 1) {
            throw new CommandFormatException("Please specify remote file");
        }
        if (command.arguments.length < 2) {
            throw new CommandFormatException("Please specify local target");
        }

        // TODO: distinguish when the remote path starts with /
        Path source = pwd.resolve(command.arguments[0]);
        Path target = Paths.get(command.arguments[1]);

        CommandLine options = parser.parse(Command.cmdOptions.get(GET), command.arguments);

        if (options.hasOption("byteOffset")) {
            int byteOffset = ((Number) options.getParsedOptionValue("byteOffset")).intValue();

            if (options.hasOption("numberOfBytes")) {
                int numberOfBytes = ((Number) options.getParsedOptionValue("numberOfBytes")).intValue();
                client.get(source, target, byteOffset, numberOfBytes);
            }
            client.get(source, target, byteOffset);
        } else {
            client.get(source, target);
        }
    }
}
