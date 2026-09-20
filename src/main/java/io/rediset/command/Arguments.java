package io.rediset.command;

import io.rediset.exception.InvalidArgumentException;

/**
 * Small helpers for validating and converting command arguments, with consistent
 * error messages.
 */
public final class Arguments {

    private Arguments() {
    }

    /** Parses {@code text} as a 64-bit signed integer, or raises a protocol error. */
    public static long parseLong(String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new InvalidArgumentException("value is not an integer or out of range");
        }
    }

    /** Parses {@code text} as an integer that must be strictly positive. */
    public static long parsePositiveLong(String text) {
        long value = parseLong(text);
        if (value <= 0) {
            throw new InvalidArgumentException("value must be a positive integer");
        }
        return value;
    }

    /** Parses {@code text} as a double score, rejecting NaN, or raises an error. */
    public static double parseDouble(String text) {
        double value;
        try {
            value = Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new InvalidArgumentException("value is not a valid float");
        }
        if (Double.isNaN(value)) {
            throw new InvalidArgumentException("value is not a valid float");
        }
        return value;
    }

    /**
     * Formats a score for the wire: integral values are rendered without a
     * trailing {@code .0} (for example {@code 3} rather than {@code 3.0}), matching
     * the conventional compact representation.
     */
    public static String formatScore(double score) {
        if (score == Math.floor(score) && !Double.isInfinite(score)) {
            return Long.toString((long) score);
        }
        return Double.toString(score);
    }
}
