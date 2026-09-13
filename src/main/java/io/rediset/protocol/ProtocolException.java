package io.rediset.protocol;

import io.rediset.exception.RedisetException;

/**
 * Thrown when an inbound byte stream violates the RediSet wire protocol (RSP):
 * an unknown type marker, malformed length, missing terminator, or a frame that
 * exceeds a configured limit. A protocol exception is fatal to the connection on
 * which it occurred but never affects other connections.
 */
public class ProtocolException extends RedisetException {

    public ProtocolException(String message) {
        super(message);
    }

    public ProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
