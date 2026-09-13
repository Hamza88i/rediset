package io.rediset.config;

/**
 * Thrown when RediSet configuration cannot be loaded or contains invalid values.
 */
public class ConfigurationException extends RuntimeException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
