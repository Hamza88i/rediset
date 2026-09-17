package io.rediset.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.rediset.datatype.StringValue;
import io.rediset.util.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StorageEngineExpirationTest {

    private MutableClock clock;
    private StorageEngine engine;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(1_000_000L);
        engine = new StorageEngine(clock);
    }

    @Test
    void shouldReportNoExpiryForFreshKey() {
        engine.put("k", StringValue.of("v"));
        assertEquals(-1L, engine.ttlMillis("k"));
    }

    @Test
    void shouldReportMissingKeyTtl() {
        assertEquals(-2L, engine.ttlMillis("nope"));
    }

    @Test
    void shouldSetAndReportTtl() {
        engine.put("k", StringValue.of("v"));
        assertTrue(engine.setExpiryAfterMillis("k", 5_000));
        assertEquals(5_000L, engine.ttlMillis("k"));
    }

    @Test
    void shouldRemoveKeyLazilyAfterExpiration() {
        engine.put("k", StringValue.of("v"));
        engine.setExpiryAfterMillis("k", 1_000);
        clock.advance(1_000); // reach the expiry boundary
        assertNull(engine.get("k"));
        assertFalse(engine.exists("k"));
        assertEquals(-2L, engine.ttlMillis("k"));
    }

    @Test
    void shouldKeepKeyBeforeExpiration() {
        engine.put("k", StringValue.of("v"));
        engine.setExpiryAfterMillis("k", 1_000);
        clock.advance(999);
        assertTrue(engine.exists("k"));
        assertEquals(1L, engine.ttlMillis("k"));
    }

    @Test
    void settingExpiryInThePastDeletesImmediately() {
        engine.put("k", StringValue.of("v"));
        assertTrue(engine.setExpiryAfterMillis("k", -1));
        assertFalse(engine.exists("k"));
    }

    @Test
    void shouldNotSetExpiryOnMissingKey() {
        assertFalse(engine.setExpiryAfterMillis("nope", 1_000));
    }

    @Test
    void persistShouldRemoveExpiry() {
        engine.put("k", StringValue.of("v"));
        engine.setExpiryAfterMillis("k", 1_000);
        assertTrue(engine.persist("k"));
        assertEquals(-1L, engine.ttlMillis("k"));
        assertFalse(engine.persist("k")); // no expiry left to remove
    }

    @Test
    void setShouldClearExistingTtl() {
        engine.put("k", StringValue.of("v"));
        engine.setExpiryAfterMillis("k", 1_000);
        engine.put("k", StringValue.of("new")); // plain SET clears TTL
        assertEquals(-1L, engine.ttlMillis("k"));
    }

    @Test
    void deleteShouldClearExpiryEntry() {
        engine.put("k", StringValue.of("v"));
        engine.setExpiryAfterMillis("k", 10_000);
        engine.delete("k");
        assertFalse(engine.keysWithExpiry().contains("k"));
    }

    @Test
    void sizeShouldNotCountExpiredKeys() {
        engine.put("a", StringValue.of("1"));
        engine.put("b", StringValue.of("2"));
        engine.setExpiryAfterMillis("b", 1_000);
        clock.advance(1_000);
        assertEquals(1, engine.size());
    }

    @Test
    void keysShouldExcludeExpiredKeys() {
        engine.put("a", StringValue.of("1"));
        engine.put("b", StringValue.of("2"));
        engine.setExpiryAfterMillis("a", 1_000);
        clock.advance(1_000);
        assertEquals(java.util.Set.of("b"), engine.keys());
    }

    @Test
    void activeSweepShouldRemoveExpiredKeys() {
        engine.put("a", StringValue.of("1"));
        engine.put("b", StringValue.of("2"));
        engine.setExpiryAfterMillis("a", 1_000);
        engine.setExpiryAfterMillis("b", 1_000);
        clock.advance(1_000);
        int removed = engine.sweepExpired(100);
        assertEquals(2, removed);
        assertTrue(engine.keysWithExpiry().isEmpty());
    }

    @Test
    void activeSweepShouldRespectSampleSize() {
        for (int i = 0; i < 10; i++) {
            engine.put("k" + i, StringValue.of("v"));
            engine.setExpiryAfterMillis("k" + i, 1_000);
        }
        clock.advance(1_000);
        int removed = engine.sweepExpired(3);
        assertTrue(removed <= 3, "sweep must not exceed sample size");
    }
}
