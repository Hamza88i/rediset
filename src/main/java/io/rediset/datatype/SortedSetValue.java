package io.rediset.datatype;

import io.rediset.util.ByteArrayKey;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * A sorted set: unique binary-safe members each associated with a floating-point
 * score, kept ordered by score and, for equal scores, by the lexicographic byte
 * order of the member.
 *
 * <h2>Representation</h2>
 * Two structures are maintained together under a single intrinsic lock:
 * <ul>
 *   <li>a {@link Map} from member to score for O(1) score lookup and membership;</li>
 *   <li>a {@link TreeSet} of {@link Entry} (score, member) for ordered range
 *       queries.</li>
 * </ul>
 * All public methods synchronize on {@code this}, so the two structures never
 * diverge and range reads observe a consistent snapshot.
 */
public final class SortedSetValue implements RedisetValue {

    /** An immutable (score, member) pair used as the ordering key. */
    public record Entry(double score, byte[] member) {
        public String memberAsString() {
            return new ByteArrayKey(member).asString();
        }
    }

    private static final Comparator<Entry> ORDER = Comparator
            .comparingDouble(Entry::score)
            .thenComparing(e -> new ByteArrayKey(e.member()).asString());

    private final Map<ByteArrayKey, Double> scores = new HashMap<>();
    private final TreeSet<Entry> ordered = new TreeSet<>(ORDER);

    @Override
    public DataType type() {
        return DataType.SORTED_SET;
    }

    /**
     * Adds {@code member} with {@code score}, or updates its score if already
     * present.
     *
     * @return {@code true} if the member was newly added, {@code false} if it
     *         already existed (score updated)
     */
    public synchronized boolean add(byte[] member, double score) {
        ByteArrayKey key = new ByteArrayKey(member);
        Double existing = scores.get(key);
        if (existing != null) {
            ordered.remove(new Entry(existing, key.bytes()));
            scores.put(key, score);
            ordered.add(new Entry(score, key.bytes()));
            return false;
        }
        scores.put(key, score);
        ordered.add(new Entry(score, key.bytes()));
        return true;
    }

    /** Returns the score of {@code member}, or {@code null} if not present. */
    public synchronized Double score(byte[] member) {
        return scores.get(new ByteArrayKey(member));
    }

    /** Removes {@code member}. Returns {@code true} if it was present. */
    public synchronized boolean remove(byte[] member) {
        ByteArrayKey key = new ByteArrayKey(member);
        Double existing = scores.remove(key);
        if (existing == null) {
            return false;
        }
        ordered.remove(new Entry(existing, key.bytes()));
        return true;
    }

    /** Returns the number of members. */
    public synchronized int size() {
        return scores.size();
    }

    public synchronized boolean isEmpty() {
        return scores.isEmpty();
    }

    /**
     * Returns the members in the inclusive rank range {@code [start, stop]} ordered
     * by ascending score, with negative indices counting from the end. The range is
     * clamped to the set bounds.
     */
    public synchronized List<Entry> rangeByRank(int start, int stop) {
        int total = ordered.size();
        if (total == 0) {
            return List.of();
        }
        int from = start < 0 ? Math.max(total + start, 0) : Math.min(start, total);
        int toInclusive = stop < 0 ? total + stop : Math.min(stop, total - 1);
        if (toInclusive < 0 || from > toInclusive || from >= total) {
            return List.of();
        }
        List<Entry> result = new ArrayList<>(toInclusive - from + 1);
        int i = 0;
        for (Entry entry : ordered) {
            if (i >= from && i <= toInclusive) {
                result.add(new Entry(entry.score(), entry.member().clone()));
            }
            if (i > toInclusive) {
                break;
            }
            i++;
        }
        return result;
    }
}
