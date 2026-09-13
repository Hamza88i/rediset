package io.rediset.server;

import io.rediset.config.ServerConfig;

/**
 * Network-layer settings, resolved from {@link ServerConfig}.
 *
 * @param host                the bind address
 * @param port                the listening port
 * @param maxClients          maximum number of concurrent client connections
 * @param idleTimeoutSeconds  close a connection after this many seconds of
 *                            inactivity; {@code 0} disables the idle timeout
 */
public record NetworkConfig(String host, int port, int maxClients, int idleTimeoutSeconds) {

    private static final int DEFAULT_PORT = 6412;
    private static final int DEFAULT_MAX_CLIENTS = 10_000;

    public NetworkConfig {
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("port must be in [1, 65535], got " + port);
        }
        if (maxClients < 1) {
            throw new IllegalArgumentException("maxClients must be >= 1, got " + maxClients);
        }
        if (idleTimeoutSeconds < 0) {
            throw new IllegalArgumentException(
                    "idleTimeoutSeconds must be >= 0, got " + idleTimeoutSeconds);
        }
    }

    /** Builds a {@link NetworkConfig} from the given {@link ServerConfig}. */
    public static NetworkConfig from(ServerConfig config) {
        return new NetworkConfig(
                config.getString("server.host", "0.0.0.0"),
                config.getInt("server.port", DEFAULT_PORT),
                config.getInt("server.max-clients", DEFAULT_MAX_CLIENTS),
                config.getInt("server.idle-timeout-seconds", 0));
    }
}
