package io.rediset.util;

/**
 * A {@link Clock} whose time is set explicitly by tests, enabling deterministic
 * verification of time-dependent behavior (such as expiration) without sleeping.
 */
public final class MutableClock implements Clock {

    private long nowMillis;

    public MutableClock(long startMillis) {
        this.nowMillis = startMillis;
    }

    @Override
    public long currentTimeMillis() {
        return nowMillis;
    }

    /** Advances the clock by {@code millis}. */
    public void advance(long millis) {
        nowMillis += millis;
    }

    /** Sets the clock to an absolute time. */
    public void set(long millis) {
        nowMillis = millis;
    }
}
