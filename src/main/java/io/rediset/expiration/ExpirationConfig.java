package io.rediset.expiration;

import io.rediset.config.ServerConfig;

/**
 * Settings for the background expiration sweeper.
 *
 * @param sweepIntervalMillis how often the sweeper runs
 * @param sampleSize          maximum number of keys-with-TTL examined per sweep
 */
public record ExpirationConfig(long sweepIntervalMillis, int sampleSize) {

    private static final long DEFAULT_INTERVAL = 100L;
    private static final int DEFAULT_SAMPLE = 20;

    public ExpirationConfig {
        if (sweepIntervalMillis < 1) {
            throw new IllegalArgumentException("sweepIntervalMillis must be >= 1");
        }
        if (sampleSize < 1) {
            throw new IllegalArgumentException("sampleSize must be >= 1");
        }
    }

    public static ExpirationConfig defaults() {
        return new ExpirationConfig(DEFAULT_INTERVAL, DEFAULT_SAMPLE);
    }

    public static ExpirationConfig from(ServerConfig config) {
        return new ExpirationConfig(
                config.getLong("expiration.sweep-interval-millis", DEFAULT_INTERVAL),
                config.getInt("expiration.sweep-sample-size", DEFAULT_SAMPLE));
    }
}
