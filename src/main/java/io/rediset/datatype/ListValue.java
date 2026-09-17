package io.rediset.datatype;

import java.util.ArrayList;
import java.util.List;

/**
 * A list of binary-safe elements with O(1) push/pop at both ends, backed by a
 * doubly-linked deque.
 *
 * <h2>Thread-safety</h2>
 * Each instance guards its internal deque with its own intrinsic lock. All public
 * operations synchronize on {@code this}, so compound reads (for example a range
 * scan) observe a consistent snapshot and concurrent mutations do not interleave.
 * Because the storage engine never holds a global lock, contention is confined to
 * the individual list being mutated.
 */
public final class ListValue implements RedisetValue {

    private final java.util.ArrayDeque<byte[]> elements = new java.util.ArrayDeque<>();

    @Override
    public DataType type() {
        return DataType.LIST;
    }

    /** Prepends an element (left push). Returns the new length. */
    public synchronized int pushLeft(byte[] element) {
        elements.addFirst(element.clone());
        return elements.size();
    }

    /** Appends an element (right push). Returns the new length. */
    public synchronized int pushRight(byte[] element) {
        elements.addLast(element.clone());
        return elements.size();
    }

    /** Removes and returns the first element, or {@code null} if empty. */
    public synchronized byte[] popLeft() {
        return elements.isEmpty() ? null : elements.pollFirst();
    }

    /** Removes and returns the last element, or {@code null} if empty. */
    public synchronized byte[] popRight() {
        return elements.isEmpty() ? null : elements.pollLast();
    }

    /** Returns the number of elements. */
    public synchronized int size() {
        return elements.size();
    }

    public synchronized boolean isEmpty() {
        return elements.isEmpty();
    }

    /**
     * Returns the element at {@code index}, supporting negative indices counting
     * from the end ({@code -1} is the last element). Returns {@code null} if the
     * index is out of range.
     */
    public synchronized byte[] get(int index) {
        int size = elements.size();
        int resolved = index < 0 ? size + index : index;
        if (resolved < 0 || resolved >= size) {
            return null;
        }
        int i = 0;
        for (byte[] element : elements) {
            if (i++ == resolved) {
                return element.clone();
            }
        }
        return null;
    }

    /**
     * Returns the elements in the inclusive index range {@code [start, stop]},
     * with negative indices counting from the end and the range clamped to the
     * list bounds. Returns an empty list if the range is empty.
     */
    public synchronized List<byte[]> range(int start, int stop) {
        int size = elements.size();
        if (size == 0) {
            return List.of();
        }
        int from = start < 0 ? Math.max(size + start, 0) : Math.min(start, size);
        int toInclusive = stop < 0 ? size + stop : Math.min(stop, size - 1);
        if (toInclusive < 0 || from > toInclusive || from >= size) {
            return List.of();
        }
        List<byte[]> result = new ArrayList<>(toInclusive - from + 1);
        int i = 0;
        for (byte[] element : elements) {
            if (i >= from && i <= toInclusive) {
                result.add(element.clone());
            }
            if (i > toInclusive) {
                break;
            }
            i++;
        }
        return result;
    }

    /** Returns a snapshot copy of all elements, head to tail. */
    public synchronized List<byte[]> toList() {
        List<byte[]> copy = new ArrayList<>(elements.size());
        for (byte[] element : elements) {
            copy.add(element.clone());
        }
        return copy;
    }
}
