package io.rediset.exception;

/** Raised when a client sends a command name that is not registered. */
public class UnknownCommandException extends CommandException {

    public UnknownCommandException(String commandName) {
        super("ERR", "unknown command '" + commandName + "'");
    }
}
