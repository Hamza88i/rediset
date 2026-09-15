package io.rediset.exception;

/**
 * Base class for exceptions that occur while validating or executing a command
 * and should be reported to the client as a protocol error reply.
 *
 * <p>Each command exception carries an {@link #errorCode()} that becomes the
 * first word of the error reply (for example {@code ERR}, {@code WRONGTYPE}), by
 * convention upper-cased.
 */
public class CommandException extends RedisetException {

    private final String errorCode;

    public CommandException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }

    /** Renders this exception as an error-reply message: {@code CODE message}. */
    public String toReplyMessage() {
        return errorCode + " " + getMessage();
    }
}
