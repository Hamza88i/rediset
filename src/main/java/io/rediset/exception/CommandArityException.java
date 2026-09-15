package io.rediset.exception;

/** Raised when a command is invoked with the wrong number of arguments. */
public class CommandArityException extends CommandException {

    public CommandArityException(String commandName) {
        super("ERR", "wrong number of arguments for '" + commandName + "' command");
    }
}
