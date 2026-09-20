package io.rediset.datatype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.rediset.datatype.SortedSetValue.Entry;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SortedSetValueTest {

    private SortedSetValue zset;

    @BeforeEach
    void setUp() {
        zset = new SortedSetValue();
    }

    private static byte[] b(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    private List<String> rankMembers(int start, int stop) {
        return zset.rangeByRank(start, stop).stream().map(Entry::memberAsString).toList();
    }

    @Test
    void shouldReportSortedSetType() {
        assertEquals(DataType.SORTED_SET, zset.type());
    }

    @Test
    void addShouldReturnTrueOnlyForNewMembers() {
        assertTrue(zset.add(b("a"), 1.0));
        assertFalse(zset.add(b("a"), 2.0)); // update
        assertEquals(2.0, zset.score(b("a")));
        assertEquals(1, zset.size());
    }

    @Test
    void shouldOrderByScore() {
        zset.add(b("c"), 3);
        zset.add(b("a"), 1);
        zset.add(b("b"), 2);
        assertEquals(List.of("a", "b", "c"), rankMembers(0, -1));
    }

    @Test
    void shouldBreakScoreTiesLexicographically() {
        zset.add(b("banana"), 1);
        zset.add(b("apple"), 1);
        assertEquals(List.of("apple", "banana"), rankMembers(0, -1));
    }

    @Test
    void updatingScoreShouldReorder() {
        zset.add(b("a"), 1);
        zset.add(b("b"), 2);
        zset.add(b("a"), 5); // move a to the end
        assertEquals(List.of("b", "a"), rankMembers(0, -1));
    }

    @Test
    void removeShouldReturnTrueWhenPresent() {
        zset.add(b("a"), 1);
        assertTrue(zset.remove(b("a")));
        assertFalse(zset.remove(b("a")));
        assertTrue(zset.isEmpty());
    }

    @Test
    void scoreShouldReturnNullForMissingMember() {
        assertNull(zset.score(b("nope")));
    }

    @Test
    void rangeShouldSupportNegativeIndicesAndClamping() {
        for (int i = 0; i < 5; i++) {
            zset.add(b("m" + i), i);
        }
        assertEquals(List.of("m0", "m1"), rankMembers(0, 1));
        assertEquals(List.of("m3", "m4"), rankMembers(-2, -1));
        assertEquals(5, rankMembers(0, 100).size());
        assertTrue(rankMembers(3, 1).isEmpty());
    }
}
