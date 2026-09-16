package io.rediset.datatype;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class StringValueTest {

    @Test
    void shouldReportStringType() {
        assertEquals(DataType.STRING, StringValue.of("x").type());
    }

    @Test
    void shouldBeBinarySafe() {
        byte[] raw = {0, '\r', '\n', 127, -1};
        StringValue value = new StringValue(raw);
        assertArrayEquals(raw, value.bytes());
        assertEquals(5, value.length());
    }

    @Test
    void shouldDefensivelyCopyOnConstruction() {
        byte[] raw = {'a', 'b'};
        StringValue value = new StringValue(raw);
        raw[0] = 'z';
        assertEquals("ab", value.asString());
    }

    @Test
    void shouldDefensivelyCopyOnRead() {
        StringValue value = StringValue.of("ab");
        byte[] out = value.bytes();
        out[0] = 'z';
        assertEquals("ab", value.asString());
    }

    @Test
    void shouldImplementValueEquality() {
        assertEquals(StringValue.of("same"), StringValue.of("same"));
        assertNotEquals(StringValue.of("a"), StringValue.of("b"));
    }
}
