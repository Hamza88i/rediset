package io.rediset.command;

import io.rediset.server.ClientSession;
import java.util.Objects;

/**
 * Execution context passed to a {@link Command}. It bundles the per-connection
 * {@link ClientSession} with the shared services a command may need.
 *
 * <p>The context is deliberately a small, growing seam: as later steps add the
 * storage engine, pub/sub bus, transaction state, and so on, they are exposed
 * here so command implementations stay decoupled from how those services are
 * constructed.
 */
public final class CommandContext {

    private final ClientSession session;

    public CommandContext(ClientSession session) {
        this.session = Objects.requireNonNull(session, "session");
    }

    public ClientSession session() {
        return session;
    }
}
