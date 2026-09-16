package io.rediset.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GlobPatternTest {

    @Test
    void starShouldMatchEverything() {
        assertTrue(GlobPattern.matches("*", ""));
        assertTrue(GlobPattern.matches("*", "anything"));
    }

    @Test
    void shouldMatchLiteral() {
        assertTrue(GlobPattern.matches("key", "key"));
        assertFalse(GlobPattern.matches("key", "keys"));
    }

    @Test
    void shouldMatchPrefixAndSuffixStars() {
        assertTrue(GlobPattern.matches("user:*", "user:123"));
        assertTrue(GlobPattern.matches("*:name", "user:name"));
        assertFalse(GlobPattern.matches("user:*", "admin:1"));
    }

    @Test
    void questionMarkShouldMatchSingleCharacter() {
        assertTrue(GlobPattern.matches("h?llo", "hello"));
        assertTrue(GlobPattern.matches("h?llo", "hallo"));
        assertFalse(GlobPattern.matches("h?llo", "hllo"));
    }

    @Test
    void shouldMatchCharacterClass() {
        assertTrue(GlobPattern.matches("gr[ae]y", "gray"));
        assertTrue(GlobPattern.matches("gr[ae]y", "grey"));
        assertFalse(GlobPattern.matches("gr[ae]y", "groy"));
    }

    @Test
    void shouldMatchCharacterRange() {
        assertTrue(GlobPattern.matches("item[0-9]", "item5"));
        assertFalse(GlobPattern.matches("item[0-9]", "itemx"));
    }

    @Test
    void shouldMatchNegatedCharacterClass() {
        assertTrue(GlobPattern.matches("gr[^e]y", "gray"));
        assertFalse(GlobPattern.matches("gr[^e]y", "grey"));
    }

    @Test
    void shouldTreatEscapedWildcardAsLiteral() {
        assertTrue(GlobPattern.matches("a\\*b", "a*b"));
        assertFalse(GlobPattern.matches("a\\*b", "axb"));
    }

    @Test
    void multipleStarsShouldCollapse() {
        assertTrue(GlobPattern.matches("a**b", "aXYZb"));
    }
}
