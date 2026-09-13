package io.rediset.protocol;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Writes {@link Reply} values to a client's output stream, buffering and flushing
 * appropriately. One writer is used per connection.
 *
 * <p>This class is a thin, testable seam over {@link ProtocolEncoder} plus the
 * output stream; it is not thread-safe and must be used from a single connection
 * thread.
 */
public final class ResponseWriter {

    private final BufferedOutputStream out;

    public ResponseWriter(OutputStream out) {
        this.out = out instanceof BufferedOutputStream buffered
                ? buffered
                : new BufferedOutputStream(out);
    }

    /** Encodes and writes a reply, then flushes it to the client. */
    public void write(Reply reply) throws IOException {
        ProtocolEncoder.write(out, reply);
        out.flush();
    }

    /** Writes a reply without flushing, for batching multiple replies. */
    public void writeBuffered(Reply reply) throws IOException {
        ProtocolEncoder.write(out, reply);
    }

    /** Flushes any buffered replies to the client. */
    public void flush() throws IOException {
        out.flush();
    }
}
