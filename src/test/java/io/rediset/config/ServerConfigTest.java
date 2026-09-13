package io.rediset.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ServerConfigTest {

    @Test
    void shouldReadDefaultPortFromProperties() {
        ServerConfig config = ServerConfig.load("/application.properties", Map.of());
        assertEquals(6412, config.getInt("server.port", -1));
    }

    @Test
    void shouldReturnDefaultWhenKeyMissing() {
        ServerConfig config = ServerConfig.load("/application.properties", Map.of());
        assertEquals("fallback", config.getString("does.not.exist", "fallback"));
    }

    @Test
    void shouldOverridePropertyWithEnvironmentVariable() {
        ServerConfig config = ServerConfig.load(
                "/application.properties", Map.of("REDISET_SERVER_PORT", "7000"));
        assertEquals(7000, config.getInt("server.port", -1));
    }

    @Test
    void shouldIgnoreBlankEnvironmentOverride() {
        ServerConfig config = ServerConfig.load(
                "/application.properties", Map.of("REDISET_SERVER_PORT", "   "));
        assertEquals(6412, config.getInt("server.port", -1));
    }

    @Test
    void shouldParseBooleanValues() {
        ServerConfig config = ServerConfig.load(
                "/application.properties", Map.of("REDISET_PERSISTENCE_AOF_ENABLED", "true"));
        assertTrue(config.getBoolean("persistence.aof.enabled", false));
        assertFalse(config.getBoolean("persistence.snapshot.enabled.missing", false));
    }

    @Test
    void shouldThrowOnInvalidNumericValue() {
        ServerConfig config = ServerConfig.load(
                "/application.properties", Map.of("REDISET_SERVER_PORT", "not-a-number"));
        assertThrows(ConfigurationException.class, () -> config.getInt("server.port", -1));
    }

    @Test
    void shouldThrowWhenResourceMissing() {
        assertThrows(ConfigurationException.class,
                () -> ServerConfig.load("/no-such-resource.properties", Map.of()));
    }
}
