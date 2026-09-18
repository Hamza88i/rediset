package io.rediset.datatype;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HashValueTest {

    private HashValue hash;

    @BeforeEach
    void setUp() {
        hash = new HashValue();
    }

    private static byte[] b(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void shouldReportHashType() {
        assertEquals(DataType.HASH, hash.type());
    }

    @Test
    void setShouldReturnTrueOnlyForNewFields() {
        assertTrue(hash.set(b("f"), b("1")));
        assertFalse(hash.set(b("f"), b("2"))); // update, not create
        assertEquals("2", new String(hash.get(b("f")), StandardCharsets.UTF_8));
        assertEquals(1, hash.size());
    }

    @Test
    void getShouldReturnNullForMissingField() {
        assertNull(hash.get(b("nope")));
    }

    @Test
    void removeShouldReturnTrueWhenPresent() {
        hash.set(b("f"), b("v"));
        assertTrue(hash.remove(b("f")));
        assertFalse(hash.remove(b("f")));
        assertTrue(hash.isEmpty());
    }

    @Test
    void flattenedEntriesShouldPairFieldsAndValues() {
        hash.set(b("a"), b("1"));
        hash.set(b("b"), b("2"));
        assertEquals(4, hash.flattenedEntries().size());
    }

    @Test
    void shouldBeBinarySafeAndDefensivelyCopied() {
        byte[] value = {0, '\r', '\n'};
        hash.set(b("f"), value);
        value[0] = 'x';
        assertArrayEquals(new byte[] {0, '\r', '\n'}, hash.get(b("f")));
    }
}
