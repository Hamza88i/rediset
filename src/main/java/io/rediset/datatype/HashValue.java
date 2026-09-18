package io.rediset.datatype;

import io.rediset.util.ByteArrayKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A field-to-value map with binary-safe fields and values.
 *
 * <h2>Thread-safety</h2>
 * Backed by a {@link ConcurrentHashMap} keyed by {@link ByteArrayKey} so byte-array
 * fields compare by value. Individual field operations are atomic; snapshot reads
 * copy the relevant fields/values.
 */
public final class HashValue implements RedisetValue {

    private final Map<ByteArrayKey, byte[]> fields = new ConcurrentHashMap<>();

    @Override
    public DataType type() {
        return DataType.HASH;
    }

    /**
     * Sets {@code field} to {@code value}.
     *
     * @return {@code true} if the field did not previously exist (was created)
     */
    public boolean set(byte[] field, byte[] value) {
        return fields.put(new ByteArrayKey(field), value.clone()) == null;
    }

    /** Returns the value for {@code field}, or {@code null} if absent. */
    public byte[] get(byte[] field) {
        byte[] value = fields.get(new ByteArrayKey(field));
        return value == null ? null : value.clone();
    }

    /** Removes {@code field}. Returns {@code true} if it was present. */
    public boolean remove(byte[] field) {
        return fields.remove(new ByteArrayKey(field)) != null;
    }

    /** Returns {@code true} if {@code field} exists. */
    public boolean contains(byte[] field) {
        return fields.containsKey(new ByteArrayKey(field));
    }

    /** Returns the number of fields. */
    public int size() {
        return fields.size();
    }

    public boolean isEmpty() {
        return fields.isEmpty();
    }

    /** Returns a snapshot copy of all field names. */
    public List<byte[]> fieldNames() {
        List<byte[]> names = new ArrayList<>(fields.size());
        for (ByteArrayKey field : fields.keySet()) {
            names.add(field.bytes());
        }
        return names;
    }

    /** Returns a snapshot copy of all values. */
    public List<byte[]> values() {
        List<byte[]> copy = new ArrayList<>(fields.size());
        for (byte[] value : fields.values()) {
            copy.add(value.clone());
        }
        return copy;
    }

    /**
     * Returns a snapshot of all field/value pairs as a flat list alternating
     * field, value, field, value, ... (convenient for the HGETALL reply).
     */
    public List<byte[]> flattenedEntries() {
        List<byte[]> flat = new ArrayList<>(fields.size() * 2);
        for (Map.Entry<ByteArrayKey, byte[]> entry : fields.entrySet()) {
            flat.add(entry.getKey().bytes());
            flat.add(entry.getValue().clone());
        }
        return flat;
    }
}
