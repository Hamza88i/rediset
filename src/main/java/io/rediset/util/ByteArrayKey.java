package io.rediset.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * An immutable wrapper around a {@code byte[]} that provides value-based
 * {@code equals}/{@code hashCode}, so binary-safe payloads can be used as keys or
 * members in hash-based collections (sets, hashes). Raw {@code byte[]} cannot be
 * used directly because arrays use identity equality.
 */
public final class ByteArrayKey {

    private final byte[] bytes;
    private final int hash;

    public ByteArrayKey(byte[] bytes) {
        this.bytes = bytes.clone();
        this.hash = Arrays.hashCode(this.bytes);
    }

    public static ByteArrayKey of(String text) {
        return new ByteArrayKey(text.getBytes(StandardCharsets.UTF_8));
    }

    /** Returns a copy of the wrapped bytes. */
    public byte[] bytes() {
        return bytes.clone();
    }

    public String asString() {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof ByteArrayKey other && Arrays.equals(bytes, other.bytes);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return "ByteArrayKey[" + asString() + "]";
    }
}
