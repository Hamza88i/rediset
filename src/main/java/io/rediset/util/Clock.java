package io.rediset.util;

/**
 * A source of the current time in epoch milliseconds. Abstracting the clock lets
 * expiration logic be tested deterministically with a controllable fake clock
 * instead of relying on real wall-clock sleeps.
 */
@FunctionalInterface
public interface Clock {

    /** The system clock, backed by {@link System#currentTimeMillis()}. */
    Clock SYSTEM = System::currentTimeMillis;

    /** Returns the current time in epoch milliseconds. */
    long currentTimeMillis();
}
