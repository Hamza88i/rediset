package io.rediset.expiration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.rediset.datatype.StringValue;
import io.rediset.storage.StorageEngine;
import io.rediset.util.MutableClock;
import java.time.Duration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ExpirationSweeperTest {

    private ExpirationSweeper sweeper;

    @AfterEach
    void tearDown() {
        if (sweeper != null) {
            sweeper.stop();
        }
    }

    @Test
    void shouldActivelyRemoveExpiredKeys() {
        MutableClock clock = new MutableClock(0L);
        StorageEngine engine = new StorageEngine(clock);
        engine.put("k", StringValue.of("v"));
        engine.setExpiryAfterMillis("k", 100);

        sweeper = new ExpirationSweeper(engine, new ExpirationConfig(10, 100));
        sweeper.start();
        assertTrue(sweeper.isRunning());

        // The key is not yet expired by the clock, so the sweeper leaves it.
        clock.set(50);
        // Advance past expiry; the sweeper should now reclaim it.
        clock.set(200);

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .until(() -> engine.keysWithExpiry().isEmpty());
        assertEquals(0, engine.size());
    }

    @Test
    void stopShouldBeIdempotent() {
        StorageEngine engine = new StorageEngine();
        sweeper = new ExpirationSweeper(engine, ExpirationConfig.defaults());
        sweeper.start();
        sweeper.stop();
        sweeper.stop();
        assertFalse(sweeper.isRunning());
    }

    @Test
    void startShouldBeIdempotent() {
        StorageEngine engine = new StorageEngine();
        sweeper = new ExpirationSweeper(engine, ExpirationConfig.defaults());
        sweeper.start();
        sweeper.start();
        assertTrue(sweeper.isRunning());
    }
}
