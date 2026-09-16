package io.rediset.util;

/**
 * A small glob matcher supporting the wildcards used by the {@code KEYS} command:
 * {@code *} (any sequence, including empty), {@code ?} (any single character),
 * {@code [...]} character classes (with {@code [^...]} negation), and {@code \}
 * escaping. Matching is performed directly against the pattern without compiling
 * to a regular expression, keeping the behavior explicit and dependency-free.
 */
public final class GlobPattern {

    private GlobPattern() {
    }

    /** Returns {@code true} if {@code input} matches {@code pattern}. */
    public static boolean matches(String pattern, String input) {
        return matches(pattern, 0, input, 0);
    }

    private static boolean matches(String pattern, int p, String input, int i) {
        int pn = pattern.length();
        int in = input.length();
        while (p < pn) {
            char pc = pattern.charAt(p);
            switch (pc) {
                case '*' -> {
                    // Collapse consecutive stars, then try to match the rest.
                    while (p + 1 < pn && pattern.charAt(p + 1) == '*') {
                        p++;
                    }
                    if (p + 1 == pn) {
                        return true; // trailing star matches everything remaining
                    }
                    for (int k = i; k <= in; k++) {
                        if (matches(pattern, p + 1, input, k)) {
                            return true;
                        }
                    }
                    return false;
                }
                case '?' -> {
                    if (i >= in) {
                        return false;
                    }
                    i++;
                    p++;
                }
                case '[' -> {
                    if (i >= in) {
                        return false;
                    }
                    int close = pattern.indexOf(']', p + 1);
                    if (close < 0) {
                        // Unterminated class: treat '[' literally.
                        if (input.charAt(i) != '[') {
                            return false;
                        }
                        i++;
                        p++;
                    } else {
                        if (!matchesClass(pattern, p + 1, close, input.charAt(i))) {
                            return false;
                        }
                        i++;
                        p = close + 1;
                    }
                }
                case '\\' -> {
                    // Escape: next pattern char is literal.
                    if (p + 1 < pn) {
                        p++;
                    }
                    if (i >= in || input.charAt(i) != pattern.charAt(p)) {
                        return false;
                    }
                    i++;
                    p++;
                }
                default -> {
                    if (i >= in || input.charAt(i) != pc) {
                        return false;
                    }
                    i++;
                    p++;
                }
            }
        }
        return i == in;
    }

    private static boolean matchesClass(String pattern, int start, int end, char c) {
        boolean negated = start < end && pattern.charAt(start) == '^';
        int idx = negated ? start + 1 : start;
        boolean matched = false;
        while (idx < end) {
            char lo = pattern.charAt(idx);
            if (idx + 2 < end && pattern.charAt(idx + 1) == '-') {
                char hi = pattern.charAt(idx + 2);
                if (c >= lo && c <= hi) {
                    matched = true;
                }
                idx += 3;
            } else {
                if (c == lo) {
                    matched = true;
                }
                idx++;
            }
        }
        return matched != negated;
    }
}
