package io.rediset.storage;

import io.rediset.datatype.DataType;
import io.rediset.datatype.RedisetValue;
import io.rediset.exception.WrongTypeException;
import io.rediset.util.Clock;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * The in-memory key space: a concurrent map from key to {@link RedisetValue},
 * with an associated map of absolute expiry timestamps.
 *
 * <h2>Thread-safety</h2>
 * The backing {@link ConcurrentHashMap}s make individual key operations atomic and
 * safe under concurrent access from many connection threads. Compound
 * read-modify-write operations that must be atomic (for example {@code INCR}, or
 * list/set mutations) are expressed with the map's atomic combinators
 * ({@link ConcurrentMap#compute}) or delegate to the value type's own internal
 * synchronization. The engine itself holds no global lock, so unrelated keys never
 * contend with one another.
 *
 * <h2>Expiration</h2>
 * Expiry is handled lazily: every keyed access first checks whether the key has an
 * expiry that has passed and, if so, removes it and treats it as absent (see
 * ADR-004). A background sweeper (the {@code expiration} package) complements this
 * by reclaiming never-accessed expired keys. The value map and expiry map are kept
 * consistent here: removing a key drops its expiry, and overwriting a key with
 * {@link #put} clears any previous TTL.
 *
 * <h2>Type safety</h2>
 * {@link #getTyped} raises {@link WrongTypeException} when a key holds a value of a
 * different type, so no command can corrupt a value by treating it as the wrong
 * type.
 */
public final class StorageEngine {

    /** Sentinel meaning "no expiry" in the expiry map. */
    private static final long NO_EXPIRY = -1L;

    private final ConcurrentMap<String, RedisetValue> data = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> expiries = new ConcurrentHashMap<>();
    private final Clock clock;

    public StorageEngine() {
        this(Clock.SYSTEM);
    }

    public StorageEngine(Clock clock) {
        this.clock = clock;
    }

    // --- Reads -----------------------------------------------------------------

    /** Returns the value for {@code key}, or {@code null} if absent or expired. */
    public RedisetValue get(String key) {
        if (expireIfNeeded(key)) {
            return null;
        }
        return data.get(key);
    }

    /**
     * Returns the value for {@code key} cast to {@code expectedClass}, or
     * {@code null} if the key is absent or expired.
     *
     * @throws WrongTypeException if the key holds a value of a different type
     */
    public <T extends RedisetValue> T getTyped(String key, Class<T> expectedClass, DataType expected) {
        RedisetValue value = get(key);
        if (value == null) {
            return null;
        }
        if (value.type() != expected) {
            throw new WrongTypeException();
        }
        return expectedClass.cast(value);
    }

    /** Returns {@code true} if {@code key} currently has a live value. */
    public boolean exists(String key) {
        if (expireIfNeeded(key)) {
            return false;
        }
        return data.containsKey(key);
    }

    // --- Writes ----------------------------------------------------------------

    /**
     * Stores {@code value} under {@code key}, replacing any existing value and
     * clearing any previous TTL (matching {@code SET} semantics).
     */
    public void put(String key, RedisetValue value) {
        data.put(key, value);
        expiries.remove(key);
    }

    /**
     * Stores {@code value} only if {@code key} is currently absent (after honoring
     * any pending expiry).
     *
     * @return {@code true} if the value was stored
     */
    public boolean putIfAbsent(String key, RedisetValue value) {
        expireIfNeeded(key);
        boolean stored = data.putIfAbsent(key, value) == null;
        if (stored) {
            expiries.remove(key);
        }
        return stored;
    }

    /**
     * Atomically computes and stores a value for {@code key}. Any pending expiry
     * is honored first. The remapping function receives the current live value (or
     * {@code null}) and returns the new value, or {@code null} to remove the key.
     * A structural change here does not by itself set or clear a TTL beyond
     * removing the expiry when the key is removed.
     */
    public RedisetValue compute(String key,
            java.util.function.BiFunction<String, RedisetValue, RedisetValue> remapping) {
        expireIfNeeded(key);
        return data.compute(key, (k, current) -> {
            RedisetValue next = remapping.apply(k, current);
            if (next == null) {
                expiries.remove(k);
            }
            return next;
        });
    }

    /**
     * Returns the existing live value for {@code key}, or atomically creates one
     * with {@code factory} if absent or expired.
     */
    public RedisetValue getOrCreate(String key, Supplier<RedisetValue> factory) {
        expireIfNeeded(key);
        return data.computeIfAbsent(key, ignored -> factory.get());
    }

    /**
     * Atomically returns the existing value for {@code key} (cast to
     * {@code expectedClass}), creating one with {@code factory} if the key is
     * absent or expired. Used by collection commands that must fetch-or-create in
     * one step.
     *
     * @throws WrongTypeException if an existing value has a different type
     */
    public <T extends RedisetValue> T getOrCreateTyped(String key, DataType expected,
            Class<T> expectedClass, Supplier<T> factory) {
        expireIfNeeded(key);
        RedisetValue value = data.compute(key, (k, current) -> {
            if (current == null) {
                return factory.get();
            }
            if (current.type() != expected) {
                throw new WrongTypeException();
            }
            return current;
        });
        return expectedClass.cast(value);
    }

    /**
     * Removes {@code key} if the given value is now empty. Used by collection
     * commands so that a list/set/hash that becomes empty deletes its key, matching
     * the convention that an empty aggregate does not linger.
     */
    public void removeIfEmpty(String key, java.util.function.Predicate<RedisetValue> isEmpty) {
        data.computeIfPresent(key, (k, current) -> {
            if (isEmpty.test(current)) {
                expiries.remove(k);
                return null;
            }
            return current;
        });
    }

    /**
     * Removes {@code key} and any associated expiry.
     *
     * @return {@code true} if a live value was present and removed
     */
    public boolean delete(String key) {
        expiries.remove(key);
        return data.remove(key) != null;
    }

    // --- TTL management --------------------------------------------------------

    /**
     * Sets an absolute expiry timestamp (epoch millis) on an existing key.
     *
     * @return {@code true} if the key exists and the expiry was set
     */
    public boolean setExpiryAt(String key, long whenEpochMillis) {
        if (!exists(key)) {
            return false;
        }
        if (whenEpochMillis <= clock.currentTimeMillis()) {
            // An expiry in the past means the key is deleted immediately.
            delete(key);
            return true;
        }
        expiries.put(key, whenEpochMillis);
        return true;
    }

    /**
     * Sets an expiry {@code millisFromNow} milliseconds in the future on an
     * existing key, using the engine's clock.
     *
     * @return {@code true} if the key exists and the expiry was set
     */
    public boolean setExpiryAfterMillis(String key, long millisFromNow) {
        return setExpiryAt(key, clock.currentTimeMillis() + millisFromNow);
    }

    /**
     * Removes any expiry from {@code key}, making it persistent.
     *
     * @return {@code true} if the key existed and had an expiry that was removed
     */
    public boolean persist(String key) {
        if (!exists(key)) {
            return false;
        }
        return expiries.remove(key) != null;
    }

    /**
     * Returns the remaining time-to-live for {@code key} in milliseconds, or a
     * negative sentinel: {@code -2} if the key does not exist, {@code -1} if it
     * exists but has no expiry.
     */
    public long ttlMillis(String key) {
        if (expireIfNeeded(key) || !data.containsKey(key)) {
            return -2L;
        }
        Long expiry = expiries.get(key);
        if (expiry == null) {
            return -1L;
        }
        return Math.max(0L, expiry - clock.currentTimeMillis());
    }

    // --- Introspection / maintenance ------------------------------------------

    /** Returns the number of live keys. */
    public int size() {
        sweepExpiredLazilyForSize();
        return data.size();
    }

    /** Returns a snapshot of all live keys. */
    public Set<String> keys() {
        Set<String> live = new HashSet<>();
        for (String key : data.keySet()) {
            if (!expireIfNeeded(key)) {
                live.add(key);
            }
        }
        return live;
    }

    /** Returns a snapshot of keys that currently have an expiry set. */
    public Set<String> keysWithExpiry() {
        return Set.copyOf(expiries.keySet());
    }

    /**
     * Actively expires keys, examining at most {@code sampleSize} keys that have a
     * TTL. Intended to be called periodically by the background sweeper. Returns
     * the number of keys removed by this call.
     */
    public int sweepExpired(int sampleSize) {
        if (sampleSize < 1) {
            return 0;
        }
        int examined = 0;
        int removed = 0;
        for (String key : expiries.keySet()) {
            if (examined >= sampleSize) {
                break;
            }
            examined++;
            if (expireIfNeeded(key)) {
                removed++;
            }
        }
        return removed;
    }

    /** Removes all keys and expiries. */
    public void clear() {
        data.clear();
        expiries.clear();
    }

    /**
     * If {@code key} has an expiry that has passed, removes it. Returns
     * {@code true} if the key was expired (and thus removed) by this call.
     */
    boolean expireIfNeeded(String key) {
        Long expiry = expiries.get(key);
        if (expiry == null) {
            return false;
        }
        if (expiry <= clock.currentTimeMillis()) {
            // Remove the expiry first, then the value, so a concurrent reader
            // never sees a value with a dangling expired timestamp.
            expiries.remove(key, expiry);
            data.remove(key);
            return true;
        }
        return false;
    }

    private void sweepExpiredLazilyForSize() {
        // Ensure size() does not count keys that are already past their expiry.
        for (String key : expiries.keySet()) {
            expireIfNeeded(key);
        }
    }
}
