package io.rediset.integration;

import io.rediset.command.CommandExecutor;
import io.rediset.command.CommandRegistry;
import io.rediset.command.RedisetConnectionHandler;
import io.rediset.protocol.ProtocolLimits;
import io.rediset.server.ConnectionHandler;
import io.rediset.server.NetworkConfig;
import io.rediset.server.RedisetServer;
import io.rediset.storage.StorageEngine;
import java.io.IOException;
import java.net.ServerSocket;

/**
 * Test fixture that starts a full {@link RedisetServer} wired with the real
 * command pipeline on an ephemeral port. Tests supply the {@link CommandRegistry}
 * so each suite can register exactly the commands it needs.
 */
final class RedisetServerFixture implements AutoCloseable {

    private final RedisetServer server;
    private final StorageEngine storage;

    private RedisetServerFixture(RedisetServer server, StorageEngine storage) {
        this.server = server;
        this.storage = storage;
    }

    static RedisetServerFixture start(CommandRegistry registry) throws IOException {
        return start(registry, new StorageEngine());
    }

    static RedisetServerFixture start(CommandRegistry registry, StorageEngine storage)
            throws IOException {
        CommandExecutor executor = new CommandExecutor(registry);
        ConnectionHandler handler =
                new RedisetConnectionHandler(executor, ProtocolLimits.defaults(), storage);
        NetworkConfig config = new NetworkConfig("127.0.0.1", freePort(), 100, 0);
        RedisetServer server = new RedisetServer(config, handler);
        server.start();
        return new RedisetServerFixture(server, storage);
    }

    StorageEngine storage() {
        return storage;
    }

    RedisetTestClient newClient() throws IOException {
        return new RedisetTestClient("127.0.0.1", server.boundPort());
    }

    int port() {
        return server.boundPort();
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @Override
    public void close() {
        server.shutdown();
    }
}
