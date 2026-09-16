package io.rediset.datatype;

/**
 * A value stored under a key in the {@code StorageEngine}. Every concrete value
 * type (string, list, set, hash, sorted set) reports its {@link DataType}, which
 * the command layer uses to reject type-mismatched operations with a
 * {@code WRONGTYPE} error.
 *
 * <p>Concrete value types encapsulate their own internal representation and any
 * synchronization needed for compound mutations, so the storage engine can treat
 * them uniformly.
 */
public interface RedisetValue {

    /** The type of this value. */
    DataType type();
}
