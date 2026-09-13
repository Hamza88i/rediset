package io.rediset.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

/**
 * Immutable, typed view over RediSet's configuration.
 *
 * <p>Configuration is loaded from {@code application.properties} on the classpath
 * and can be overridden per-property by an environment variable. The environment
 * variable name is derived from the property key by upper-casing it, replacing
 * dots with underscores, and prefixing it with {@code REDISET_}. For example the
 * property {@code server.port} is overridden by {@code REDISET_SERVER_PORT}.
 */
public final class ServerConfig {

    /** Prefix applied to all environment-variable overrides. */
    public static final String ENV_PREFIX = "REDISET_";

    private static final String DEFAULT_RESOURCE = "/application.properties";

    private final Properties properties;
    private final Map<String, String> environment;

    ServerConfig(Properties properties, Map<String, String> environment) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.environment = Objects.requireNonNull(environment, "environment");
    }

    /**
     * Loads configuration from the default classpath resource, applying overrides
     * from the current process environment.
     */
    public static ServerConfig load() {
        return load(DEFAULT_RESOURCE, System.getenv());
    }

    /**
     * Loads configuration from a specific classpath resource, applying overrides
     * from the supplied environment map. Exposed for tests.
     */
    public static ServerConfig load(String resource, Map<String, String> environment) {
        Properties props = new Properties();
        try (InputStream in = ServerConfig.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new ConfigurationException("Configuration resource not found: " + resource);
            }
            props.load(in);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to load configuration: " + resource, e);
        }
        return new ServerConfig(props, Map.copyOf(environment));
    }

    /** Returns the raw string value for a key, or {@code defaultValue} if absent. */
    public String getString(String key, String defaultValue) {
        String envKey = ENV_PREFIX + key.toUpperCase(Locale.ROOT).replace('.', '_');
        String envValue = environment.get(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return properties.getProperty(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        return parse(key, defaultValue, Integer::parseInt);
    }

    public long getLong(String key, long defaultValue) {
        return parse(key, defaultValue, Long::parseLong);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key, null);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    private <T> T parse(String key, T defaultValue, Function<String, T> parser) {
        String value = getString(key, null);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return parser.apply(value.trim());
        } catch (NumberFormatException e) {
            throw new ConfigurationException(
                    "Invalid numeric value for '" + key + "': " + value, e);
        }
    }
}
