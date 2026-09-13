package io.rediset.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Strategy for servicing a single client connection.
 *
 * <p>The {@link RedisetServer} owns socket lifecycle and concurrency; it delegates
 * the actual read/decode/execute/write loop to a {@code ConnectionHandler}. This
 * keeps the {@code server} package free of any dependency on the protocol and
 * command layers, preserving the one-way dependency direction
 * {@code server → protocol → command → storage}.
 */
@FunctionalInterface
public interface ConnectionHandler {

    /**
     * Services one connection until the client disconnects or the connection is
     * closed. Implementations must return promptly once {@code session} reports
     * that it is closing, and must not swallow {@link InterruptedException}
     * without restoring the interrupt flag.
     *
     * @param session the per-connection session state
     * @param in      the client input stream
     * @param out     the client output stream
     * @throws IOException if the underlying streams fail
     */
    void handle(ClientSession session, InputStream in, OutputStream out) throws IOException;
}
