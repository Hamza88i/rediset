package io.rediset.exception;

/**
 * Base class for all RediSet-specific runtime exceptions.
 *
 * <p>Command-level exceptions that should be reported back to a connected client
 * as protocol errors extend {@link CommandException}. Infrastructure failures
 * (I/O, configuration) may extend this class directly or use dedicated types.
 */
public class RedisetException extends RuntimeException {

    public RedisetException(String message) {
        super(message);
    }

    public RedisetException(String message, Throwable cause) {
        super(message, cause);
    }
}
