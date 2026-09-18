package io.rediset.datatype;

import io.rediset.util.ByteArrayKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An unordered collection of unique, binary-safe members.
 *
 * <h2>Thread-safety</h2>
 * Members are stored in a {@link ConcurrentHashMap}-backed set keyed by
 * {@link ByteArrayKey} (which gives byte arrays value equality). Individual
 * add/remove/contains operations are atomic; snapshot reads
 * ({@link #members()}) copy the current membership.
 */
public final class SetValue implements RedisetValue {

    private final Set<ByteArrayKey> members = ConcurrentHashMap.newKeySet();

    @Override
    public DataType type() {
        return DataType.SET;
    }

    /** Adds a member. Returns {@code true} if it was not already present. */
    public boolean add(byte[] member) {
        return members.add(new ByteArrayKey(member));
    }

    /** Removes a member. Returns {@code true} if it was present. */
    public boolean remove(byte[] member) {
        return members.remove(new ByteArrayKey(member));
    }

    /** Returns {@code true} if the member is present. */
    public boolean contains(byte[] member) {
        return members.contains(new ByteArrayKey(member));
    }

    /** Returns the number of members. */
    public int size() {
        return members.size();
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    /** Returns a snapshot copy of all members. */
    public List<byte[]> members() {
        List<byte[]> copy = new ArrayList<>(members.size());
        for (ByteArrayKey member : members) {
            copy.add(member.bytes());
        }
        return copy;
    }
}
