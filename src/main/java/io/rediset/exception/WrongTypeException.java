package io.rediset.exception;

/**
 * Raised when a command is applied to a key whose stored value is of a different
 * data type (for example a list operation on a string key).
 */
public class WrongTypeException extends CommandException {

    public WrongTypeException() {
        super("WRONGTYPE", "Operation against a key holding the wrong kind of value");
    }
}
