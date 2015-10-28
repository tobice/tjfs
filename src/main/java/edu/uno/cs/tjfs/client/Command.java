package edu.uno.cs.tjfs.client;

import java.util.Arrays;

public class Command {
    public final CommandName name;
    public final String[] arguments;

    private Command(CommandName name, String[] arguments) {
        this.name = name;
        this.arguments = arguments;
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

        CommandName name = getCommandName(tokens[0]);
        if (name == null) {
            throw new CommandFormatException("Unknown command " + tokens[0]);
        }

        return new Command(name, Arrays.copyOfRange(tokens, 1, tokens.length));
    }

    private static CommandName getCommandName(String token) {
        for (CommandName name : CommandName.values()) {
            if (name.getValue().equals(token)) {
                return name;
            }
        }

        return null;
    }
}
