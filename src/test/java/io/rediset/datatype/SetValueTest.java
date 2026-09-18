package io.rediset.datatype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SetValueTest {

    private SetValue set;

    @BeforeEach
    void setUp() {
        set = new SetValue();
    }

    private static byte[] b(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void shouldReportSetType() {
        assertEquals(DataType.SET, set.type());
    }

    @Test
    void addShouldReturnTrueOnlyForNewMembers() {
        assertTrue(set.add(b("a")));
        assertFalse(set.add(b("a")));
        assertEquals(1, set.size());
    }

    @Test
    void shouldUseValueEqualityForBinaryMembers() {
        set.add(b("member"));
        assertTrue(set.contains(b("member")));
        assertFalse(set.contains(b("other")));
    }

    @Test
    void removeShouldReturnTrueWhenPresent() {
        set.add(b("a"));
        assertTrue(set.remove(b("a")));
        assertFalse(set.remove(b("a")));
        assertTrue(set.isEmpty());
    }

    @Test
    void membersShouldReturnAllMembers() {
        set.add(b("a"));
        set.add(b("b"));
        Set<String> members = new HashSet<>();
        for (byte[] m : set.members()) {
            members.add(new String(m, StandardCharsets.UTF_8));
        }
        assertEquals(Set.of("a", "b"), members);
    }

    @Test
    void shouldHandleBinarySafeMembers() {
        byte[] raw = {0, '\r', '\n', 7};
        assertTrue(set.add(raw));
        assertTrue(set.contains(new byte[] {0, '\r', '\n', 7}));
    }
}
