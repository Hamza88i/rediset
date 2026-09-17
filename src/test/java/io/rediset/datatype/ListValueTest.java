package io.rediset.datatype;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListValueTest {

    private ListValue list;

    @BeforeEach
    void setUp() {
        list = new ListValue();
    }

    private static byte[] b(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private static String s(byte[] b) {
        return new String(b, StandardCharsets.UTF_8);
    }

    @Test
    void shouldReportListType() {
        assertEquals(DataType.LIST, list.type());
    }

    @Test
    void pushRightShouldAppend() {
        assertEquals(1, list.pushRight(b("a")));
        assertEquals(2, list.pushRight(b("b")));
        assertEquals(List.of("a", "b"), list.toList().stream().map(ListValueTest::s).toList());
    }

    @Test
    void pushLeftShouldPrepend() {
        list.pushLeft(b("a"));
        list.pushLeft(b("b"));
        assertEquals(List.of("b", "a"), list.toList().stream().map(ListValueTest::s).toList());
    }

    @Test
    void popShouldRemoveFromCorrectEnds() {
        list.pushRight(b("a"));
        list.pushRight(b("b"));
        list.pushRight(b("c"));
        assertEquals("a", s(list.popLeft()));
        assertEquals("c", s(list.popRight()));
        assertEquals(1, list.size());
    }

    @Test
    void popOnEmptyShouldReturnNull() {
        assertNull(list.popLeft());
        assertNull(list.popRight());
    }

    @Test
    void getShouldSupportNegativeIndex() {
        list.pushRight(b("a"));
        list.pushRight(b("b"));
        list.pushRight(b("c"));
        assertEquals("a", s(list.get(0)));
        assertEquals("c", s(list.get(-1)));
        assertNull(list.get(5));
        assertNull(list.get(-5));
    }

    @Test
    void rangeShouldClampAndSupportNegativeIndices() {
        for (String v : List.of("a", "b", "c", "d", "e")) {
            list.pushRight(b(v));
        }
        assertEquals(List.of("a", "b", "c"),
                list.range(0, 2).stream().map(ListValueTest::s).toList());
        assertEquals(List.of("d", "e"),
                list.range(-2, -1).stream().map(ListValueTest::s).toList());
        assertEquals(List.of("a", "b", "c", "d", "e"),
                list.range(0, 100).stream().map(ListValueTest::s).toList());
        assertTrue(list.range(3, 1).isEmpty());
    }

    @Test
    void shouldBeBinarySafeAndDefensivelyCopied() {
        byte[] raw = {0, '\r', '\n'};
        list.pushRight(raw);
        raw[0] = 'x';
        assertArrayEquals(new byte[] {0, '\r', '\n'}, list.get(0));
    }
}
