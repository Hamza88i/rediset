package io.rediset.exception;

/**
 * Raised when an argument value is syntactically invalid, for example a
 * non-integer where an integer is required.
 */
public class InvalidArgumentException extends CommandException {

    public InvalidArgumentException(String message) {
        super("ERR", message);
    }
}
