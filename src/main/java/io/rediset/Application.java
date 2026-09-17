package io.rediset;

import io.rediset.command.CommandExecutor;
import io.rediset.command.CommandRegistry;
import io.rediset.command.CommandRegistryFactory;
import io.rediset.command.RedisetConnectionHandler;
import io.rediset.config.ServerConfig;
import io.rediset.expiration.ExpirationConfig;
import io.rediset.expiration.ExpirationSweeper;
import io.rediset.protocol.ProtocolLimits;
import io.rediset.server.ConnectionHandler;
import io.rediset.server.NetworkConfig;
import io.rediset.server.RedisetServer;
import io.rediset.storage.StorageEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RediSet entry point. Wires the configuration, protocol, command, and server
 * layers together and runs the TCP server until it is shut down.
 */
public final class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private Application() {
    }

    public static void main(String[] args) throws Exception {
        ServerConfig config = ServerConfig.load();
        NetworkConfig networkConfig = NetworkConfig.from(config);
        ProtocolLimits limits = ProtocolLimits.from(config);

        StorageEngine storage = new StorageEngine();
        CommandRegistry registry = CommandRegistryFactory.createDefault();
        CommandExecutor executor = new CommandExecutor(registry);
        ConnectionHandler handler = new RedisetConnectionHandler(executor, limits, storage);

        ExpirationSweeper sweeper =
                new ExpirationSweeper(storage, ExpirationConfig.from(config));
        RedisetServer server = new RedisetServer(networkConfig, handler);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
            sweeper.stop();
        }, "rediset-shutdown"));

        log.info("Starting RediSet with {} registered command(s)", registry.size());
        sweeper.start();
        server.start();
        server.awaitTermination();
    }
}
