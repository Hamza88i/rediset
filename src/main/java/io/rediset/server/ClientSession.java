package io.rediset.server;

import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-connection state, created by the server when a client connects and passed
 * to the {@link ConnectionHandler}.
 *
 * <p>A session carries a stable numeric id, the remote address, liveness flags,
 * and a place for higher layers to attach arbitrary per-connection state (for
 * example a transaction context or a set of pub/sub subscriptions) without the
 * {@code server} package needing to know those types.
 */
public final class ClientSession {

    private final long id;
    private final SocketAddress remoteAddress;
    private final long createdAtMillis;
    private final AtomicBoolean closing = new AtomicBoolean(false);
    private final AtomicLong lastActivityMillis;

    private volatile Object attachment;

    public ClientSession(long id, SocketAddress remoteAddress) {
        this.id = id;
        this.remoteAddress = remoteAddress;
        this.createdAtMillis = System.currentTimeMillis();
        this.lastActivityMillis = new AtomicLong(this.createdAtMillis);
    }

    public long id() {
        return id;
    }

    public SocketAddress remoteAddress() {
        return remoteAddress;
    }

    public long createdAtMillis() {
        return createdAtMillis;
    }

    /** Records that the client sent or received data, resetting the idle clock. */
    public void touch() {
        lastActivityMillis.set(System.currentTimeMillis());
    }

    public long lastActivityMillis() {
        return lastActivityMillis.get();
    }

    /** Returns {@code true} once the connection has been marked for shutdown. */
    public boolean isClosing() {
        return closing.get();
    }

    /** Marks the connection as closing. Idempotent. */
    public void markClosing() {
        closing.set(true);
    }

    /** Returns the attachment set by a higher layer, or {@code null}. */
    public Object attachment() {
        return attachment;
    }

    /** Attaches arbitrary per-connection state owned by a higher layer. */
    public void attach(Object attachment) {
        this.attachment = attachment;
    }

    @Override
    public String toString() {
        return "ClientSession[id=" + id + ", remote=" + remoteAddress + "]";
    }
}
