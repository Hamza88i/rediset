package io.rediset.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.rediset.datatype.DataType;
import io.rediset.datatype.StringValue;
import io.rediset.exception.WrongTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StorageEngineTest {

    private StorageEngine engine;

    @BeforeEach
    void setUp() {
        engine = new StorageEngine();
    }

    @Test
    void shouldStoreAndRetrieveValue() {
        engine.put("k", StringValue.of("v"));
        StringValue value = engine.getTyped("k", StringValue.class, DataType.STRING);
        assertEquals("v", value.asString());
    }

    @Test
    void shouldReturnNullForMissingKey() {
        assertNull(engine.get("missing"));
        assertNull(engine.getTyped("missing", StringValue.class, DataType.STRING));
    }

    @Test
    void shouldOverwriteExistingValue() {
        engine.put("k", StringValue.of("first"));
        engine.put("k", StringValue.of("second"));
        assertEquals("second",
                engine.getTyped("k", StringValue.class, DataType.STRING).asString());
    }

    @Test
    void shouldReportExistenceAndSize() {
        assertFalse(engine.exists("k"));
        assertEquals(0, engine.size());
        engine.put("k", StringValue.of("v"));
        assertTrue(engine.exists("k"));
        assertEquals(1, engine.size());
    }

    @Test
    void shouldDeleteExistingKey() {
        engine.put("k", StringValue.of("v"));
        assertTrue(engine.delete("k"));
        assertFalse(engine.delete("k"));
        assertFalse(engine.exists("k"));
    }

    @Test
    void shouldThrowWrongTypeWhenTypeMismatches() {
        engine.put("k", new WrongTyped());
        assertThrows(WrongTypeException.class,
                () -> engine.getTyped("k", StringValue.class, DataType.STRING));
    }

    @Test
    void shouldNotCorruptValueOnWrongTypeAccess() {
        WrongTyped stored = new WrongTyped();
        engine.put("k", stored);
        assertThrows(WrongTypeException.class,
                () -> engine.getTyped("k", StringValue.class, DataType.STRING));
        // Value is untouched after the failed typed access.
        assertEquals(stored, engine.get("k"));
    }

    @Test
    void putIfAbsentShouldOnlyStoreWhenMissing() {
        assertTrue(engine.putIfAbsent("k", StringValue.of("a")));
        assertFalse(engine.putIfAbsent("k", StringValue.of("b")));
        assertEquals("a", engine.getTyped("k", StringValue.class, DataType.STRING).asString());
    }

    @Test
    void keysShouldReturnSnapshotOfAllKeys() {
        engine.put("a", StringValue.of("1"));
        engine.put("b", StringValue.of("2"));
        assertEquals(2, engine.keys().size());
        assertTrue(engine.keys().contains("a"));
    }

    @Test
    void clearShouldRemoveAllKeys() {
        engine.put("a", StringValue.of("1"));
        engine.clear();
        assertEquals(0, engine.size());
    }

    /** A minimal non-string value used to exercise the type-mismatch path. */
    private static final class WrongTyped implements io.rediset.datatype.RedisetValue {
        @Override
        public DataType type() {
            return DataType.LIST;
        }
    }
}
