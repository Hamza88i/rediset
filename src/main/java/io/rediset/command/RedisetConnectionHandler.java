package io.rediset.command;

import io.rediset.protocol.CommandParser;
import io.rediset.protocol.ParsedCommand;
import io.rediset.protocol.ProtocolDecoder;
import io.rediset.protocol.ProtocolException;
import io.rediset.protocol.ProtocolLimits;
import io.rediset.protocol.Reply;
import io.rediset.protocol.ResponseWriter;
import io.rediset.server.ClientSession;
import io.rediset.server.ConnectionHandler;
import io.rediset.storage.StorageEngine;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ConnectionHandler} that drives the RediSet request/response loop:
 * decode a request frame, parse it into a command, execute it, and write the
 * reply. It bridges the {@code protocol} and {@code command} layers while the
 * {@code server} layer remains unaware of both.
 *
 * <p>A single handler instance is shared across all connections; per-connection
 * state lives in the {@link ClientSession} and in stack-local decoder/writer
 * objects created per invocation.
 */
public final class RedisetConnectionHandler implements ConnectionHandler {

    private static final Logger log = LoggerFactory.getLogger(RedisetConnectionHandler.class);

    private final CommandExecutor executor;
    private final ProtocolLimits limits;
    private final StorageEngine storage;

    public RedisetConnectionHandler(CommandExecutor executor, ProtocolLimits limits,
            StorageEngine storage) {
        this.executor = executor;
        this.limits = limits;
        this.storage = storage;
    }

    @Override
    public void handle(ClientSession session, InputStream in, OutputStream out) throws IOException {
        ProtocolDecoder decoder = new ProtocolDecoder(in, limits);
        ResponseWriter writer = new ResponseWriter(out);
        CommandContext context = new CommandContext(session, storage);

        while (!session.isClosing()) {
            ParsedCommand parsed;
            try {
                parsed = CommandParser.parse(decoder.readRequest());
            } catch (EOFException e) {
                // Clean client disconnect between frames.
                return;
            } catch (ProtocolException e) {
                // Report the violation, then close this connection only.
                log.debug("Protocol error on client {}: {}", session.id(), e.getMessage());
                writer.write(Reply.error("ERR protocol error: " + e.getMessage()));
                return;
            }

            session.touch();
            Reply reply = executor.execute(context, parsed);
            writer.write(reply);
        }
    }
}
