package io.rediset.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.rediset.config.ServerConfig;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NetworkConfigTest {

    @Test
    void shouldBuildFromServerConfigDefaults() {
        ServerConfig config = ServerConfig.load("/application.properties", Map.of());
        NetworkConfig network = NetworkConfig.from(config);
        assertEquals(6412, network.port());
        assertEquals("0.0.0.0", network.host());
    }

    @Test
    void shouldRejectPortOutOfRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new NetworkConfig("0.0.0.0", 0, 10, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new NetworkConfig("0.0.0.0", 70_000, 10, 0));
    }

    @Test
    void shouldRejectNonPositiveMaxClients() {
        assertThrows(IllegalArgumentException.class,
                () -> new NetworkConfig("0.0.0.0", 6412, 0, 0));
    }

    @Test
    void shouldRejectNegativeIdleTimeout() {
        assertThrows(IllegalArgumentException.class,
                () -> new NetworkConfig("0.0.0.0", 6412, 10, -1));
    }
}
