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
}
