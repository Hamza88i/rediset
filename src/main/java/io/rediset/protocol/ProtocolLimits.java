package io.rediset.protocol;

import io.rediset.config.ServerConfig;

/**
 * Configurable protocol-level limits that provide backpressure and protect the
 * server from hostile or buggy clients.
 *
 * @param maxBulkStringSize largest allowed single bulk-string payload, in bytes
 * @param maxArraySize      largest allowed element count in an array frame
 * @param maxRequestSize    largest allowed total size of one request frame, in bytes
 */
public record ProtocolLimits(long maxBulkStringSize, int maxArraySize, long maxRequestSize) {

    private static final long DEFAULT_MAX_BULK = 512L * 1024 * 1024;
    private static final int DEFAULT_MAX_ARRAY = 1024 * 1024;
    private static final long DEFAULT_MAX_REQUEST = 512L * 1024 * 1024;

    public ProtocolLimits {
        if (maxBulkStringSize < 1) {
            throw new IllegalArgumentException("maxBulkStringSize must be >= 1");
        }
        if (maxArraySize < 1) {
            throw new IllegalArgumentException("maxArraySize must be >= 1");
        }
        if (maxRequestSize < 1) {
            throw new IllegalArgumentException("maxRequestSize must be >= 1");
        }
    }

    /** Returns a permissive default set of limits. */
    public static ProtocolLimits defaults() {
        return new ProtocolLimits(DEFAULT_MAX_BULK, DEFAULT_MAX_ARRAY, DEFAULT_MAX_REQUEST);
    }

    /** Builds limits from configuration. */
    public static ProtocolLimits from(ServerConfig config) {
        return new ProtocolLimits(
                config.getLong("protocol.max-bulk-string-size", DEFAULT_MAX_BULK),
                config.getInt("protocol.max-array-size", DEFAULT_MAX_ARRAY),
                config.getLong("protocol.max-request-size", DEFAULT_MAX_REQUEST));
    }
}
