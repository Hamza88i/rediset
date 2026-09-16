package io.rediset.storage;

import io.rediset.datatype.DataType;
import io.rediset.datatype.RedisetValue;
import io.rediset.exception.WrongTypeException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * The in-memory key space: a concurrent map from key to {@link RedisetValue}.
 *
 * <h2>Thread-safety</h2>
 * The backing {@link ConcurrentHashMap} makes individual key operations
 * (get/put/remove/replace) atomic and safe under concurrent access from many
 * connection threads. Compound read-modify-write operations that must be atomic
 * (for example {@code INCR}, or list/set mutations) are expressed with the
 * map's atomic combinators ({@link ConcurrentMap#compute},
 * {@link ConcurrentMap#merge}) or delegate to the value type's own internal
 * synchronization. The engine itself holds no global lock, so unrelated keys
 * never contend with one another.
 *
 * <p>Type safety is enforced here: {@link #getTyped} raises
 * {@link WrongTypeException} when a key holds a value of a different type, so no
 * command can corrupt a value by treating it as the wrong type.
 */
public final class StorageEngine {

    private final ConcurrentMap<String, RedisetValue> data = new ConcurrentHashMap<>();

    /** Returns the value for {@code key}, or {@code null} if absent. */
    public RedisetValue get(String key) {
        return data.get(key);
    }

    /**
     * Returns the value for {@code key} cast to {@code expectedClass}, or
     * {@code null} if the key is absent.
     *
     * @throws WrongTypeException if the key holds a value of a different type
     */
    public <T extends RedisetValue> T getTyped(String key, Class<T> expectedClass, DataType expected) {
        RedisetValue value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value.type() != expected) {
            throw new WrongTypeException();
        }
        return expectedClass.cast(value);
    }

    /** Stores {@code value} under {@code key}, replacing any existing value. */
    public void put(String key, RedisetValue value) {
        data.put(key, value);
    }

    /**
     * Stores {@code value} only if {@code key} is currently absent.
     *
     * @return {@code true} if the value was stored
     */
    public boolean putIfAbsent(String key, RedisetValue value) {
        return data.putIfAbsent(key, value) == null;
    }

    /**
     * Atomically computes and stores a value for {@code key}. The remapping
     * function receives the current value (or {@code null}) and returns the new
     * value, or {@code null} to remove the key. Used for atomic read-modify-write
     * commands.
     */
    public RedisetValue compute(String key,
            java.util.function.BiFunction<String, RedisetValue, RedisetValue> remapping) {
        return data.compute(key, remapping);
    }

    /**
     * Returns the existing value for {@code key}, or atomically creates one with
     * {@code factory} if absent. The factory may run more than once under
     * contention but only one result is stored.
     */
    public RedisetValue getOrCreate(String key, Supplier<RedisetValue> factory) {
        return data.computeIfAbsent(key, ignored -> factory.get());
    }

    /**
     * Removes {@code key}.
     *
     * @return {@code true} if a value was present and removed
     */
    public boolean delete(String key) {
        return data.remove(key) != null;
    }

    /** Returns {@code true} if {@code key} currently has a value. */
    public boolean exists(String key) {
        return data.containsKey(key);
    }

    /** Returns the number of keys currently stored. */
    public int size() {
        return data.size();
    }

    /** Returns a snapshot of all current keys. */
    public Set<String> keys() {
        return Set.copyOf(data.keySet());
    }

    /** Removes all keys. */
    public void clear() {
        data.clear();
    }
}
