package edu.uno.cs.tjfs.client;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Static tools for parsing commands. Defines available commands and their options.
 *
 * An instance of this object represents a parsed command with its name and arguments.
 */
public class Command {

    /** Enum of all available commands */
    public enum Name {
        GET("get"),
        PUT("put"),
        DELETE("delete"),
        GET_SIZE("getsize"),
        GET_TIME("gettime"),
        LIST("list"),
        CD("cd");

        private String value;

        private Name(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }
    }

    /** Definition of named options for each command */
    public static Map<Name, Options> cmdOptions;

    static {
        // Initialize option definitions for Apache Commons CLI
        cmdOptions = new HashMap<>();
        /*
        cmdOptions.put(Name.GET, new Options()
                .addOption("byteOffset", true, "")
                .addOption("numberOfBytes", true, ""));
        */
        cmdOptions.put(Name.GET, new Options()
            .addOption(Option.builder().longOpt("byteOffset").hasArg().type(Number.class).build())
            .addOption(Option.builder().longOpt("numberOfBytes").hasArg().type(Number.class).build()));
    }

    /**
     * Parse line to get command with arguments
     * @param line input string to be parsed
     * @return parsed command
     * @throws CommandFormatException
     */
    public static Command parse(String line) throws CommandFormatException {
        // TODO: come up with better parsing (support quotes, backslashes etc...)
        String[] tokens = line.replaceAll("^\\s+", "").split("\\s+");

        if (tokens.length == 0) {
            throw new CommandFormatException("Entered empty command");
        }

        Name name = getCommandName(tokens[0]);
        if (name == null) {
            throw new CommandFormatException("Unknown command " + tokens[0]);
        }

        return new Command(name, Arrays.copyOfRange(tokens, 1, tokens.length));
    }

    /**
     * Return name of the command passed as text
     * @param token command name as text
     * @return commmand name or null if not found
     */
    private static Name getCommandName(String token) {
        for (Name name : Name.values()) {
            if (name.getValue().equals(token)) {
                return name;
            }
        }

        return null;
    }

    /** Name of the parsed command */
    public final Name name;

    /** Parsed arguments */
    public final String[] arguments;

    private Command(Name name, String[] arguments) {
        this.name = name;
        this.arguments = arguments;
    }

}
