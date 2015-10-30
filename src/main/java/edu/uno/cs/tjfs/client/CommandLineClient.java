package edu.uno.cs.tjfs.client;

import edu.uno.cs.tjfs.common.TjfsException;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static edu.uno.cs.tjfs.client.Command.Name.*;

public class CommandLineClient {

    private IClient client;

    /** Current remote path in Tjfs */
    private Path workingDirectory;

    /** Output stream used for printing log messages */
    private PrintStream log;

    /** Apache Commons CLI parses for parsing command arguments */
    private CommandLineParser parser = new DefaultParser();

    public CommandLineClient(IClient client, PrintStream log) {
        this.client = client;
        this.log = log;
        this.workingDirectory = Paths.get("/");
    }

    /**
     * Get current working directory on a remote server
     * @return path to a remote folder
     */
    public Path getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * Parse and execute command.
     * @param line command to execute
     */
    public void command(String line) {
        try {
            Command command = Command.parse(line);

            switch (command.name) {
                case GET: get(command.arguments); break;
                case PUT: put(command.arguments); break;
                case DELETE: delete(command.arguments); break;
                case GET_SIZE: getSize(command.arguments); break;
                case GET_TIME: getTime(command.arguments); break;
                case LIST: list(command.arguments); break;
                case CD: cd(command.arguments); break;
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
        } catch (TjfsException e) {
            log.println(e.getMessage());
            e.printStackTrace(log);
        }
    }

    /**
     * Resolves relative paths against current working directory. Absolute paths remain unchanged.
     * The resulting path gets normalized (.., . are evaluated)
     * @param path either relative or absolute remote path as string
     * @return absolute remote path
     */
    private Path getAbsolutePath(String path) {
        if (path.startsWith("/")) {
            return Paths.get(path);
        } else {
            return workingDirectory.resolve(path).normalize();
        }
    }

    private void get(String[] arguments) throws CommandFormatException, IOException, TjfsClientException, ParseException {
        if (arguments.length < 1) {
            throw new CommandFormatException("Please specify remote file");
        }
        if (arguments.length < 2) {
            throw new CommandFormatException("Please specify local target");
        }

        Path source = getAbsolutePath(arguments[0]);
        Path target = Paths.get(arguments[1]);

        CommandLine options = parser.parse(Command.cmdOptions.get(GET), arguments);

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

    private void put(String[] arguments) throws CommandFormatException, ParseException, IOException, TjfsClientException {
        if (arguments.length < 1) {
            throw new CommandFormatException("Please specify local file");
        }
        if (arguments.length < 2) {
            throw new CommandFormatException("Please specify remote target");
        }

        Path source = Paths.get(arguments[0]);
        Path target = getAbsolutePath(arguments[1]);

        CommandLine options = parser.parse(Command.cmdOptions.get(PUT), arguments);

        if (options.hasOption("byteOffset")) {
            int byteOffset = ((Number) options.getParsedOptionValue("byteOffset")).intValue();
            client.put(source, target, byteOffset);
        } else {
            client.put(source, target);
        }
    }

    private void delete(String[] arguments) throws TjfsClientException, CommandFormatException {
        if (arguments.length < 1) {
            throw new CommandFormatException("Please specify remote file to delete");
        }

        Path path = getAbsolutePath(arguments[0]);
        client.delete(path);
    }

    private void getSize(String[] arguments) throws CommandFormatException, TjfsClientException {
        if (arguments.length < 1) {
            throw new CommandFormatException("Please specify remote file");
        }

        Path path = getAbsolutePath(arguments[0]);
        int bytes = client.getSize(path);
        log.println(bytes + " bytes (" + FileUtils.byteCountToDisplaySize(bytes) + ")");
    }

    private void getTime(String[] arguments) throws CommandFormatException, TjfsClientException {
        if (arguments.length < 1) {
            throw new CommandFormatException("Please specify remote file");
        }

        Path path = getAbsolutePath(arguments[0]);
        log.println(client.getTime(path));
    }

    private void list(String[] arguments) throws CommandFormatException, TjfsClientException {
        if (arguments.length < 1) {
            throw new CommandFormatException("Please specify remote folder");
        }

        Path path = getAbsolutePath(arguments[0]);
        String[] content = client.list(path);

        // Sort items alphabetically and put folders first
        Arrays.sort(content, (String a, String b) -> {
            if (a.endsWith("/") && !b.endsWith("/")) {
                return -1;
            } else if (!a.endsWith("/") && b.endsWith("/")) {
                return 1;
            } else {
                return a.compareTo(b);
            }
        });

        for (String item : content) {
            // Remove prefixes (i. e. display only node names)
            log.println(item.replaceFirst("^" + path.toString(), "").replaceFirst("^/", ""));
        }
    }

    private void cd(String[] arguments) throws TjfsException {
        if (arguments.length < 1) {
            throw new CommandFormatException("Please specify remote folder");
        }

        Path path = getAbsolutePath(arguments[0]);
        String[] content = client.list(path);

        // Allow change only to non empty directories
        if (content.length == 0) {
            throw new TjfsException("Directory does not exist (= is empty)");
        }

        workingDirectory = path;
    }
}
