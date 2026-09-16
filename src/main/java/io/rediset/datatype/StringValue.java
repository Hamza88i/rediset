package io.rediset.datatype;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * An immutable, binary-safe string value. A new {@code StringValue} is created on
 * each write, so readers never observe a partially updated payload.
 */
public final class StringValue implements RedisetValue {

    private final byte[] bytes;

    public StringValue(byte[] bytes) {
        // Defensive copy so callers cannot mutate the stored payload afterwards.
        this.bytes = bytes.clone();
    }

    public static StringValue of(String text) {
        return new StringValue(text.getBytes(StandardCharsets.UTF_8));
    }

    /** Returns a copy of the raw bytes. */
    public byte[] bytes() {
        return bytes.clone();
    }

    /** The payload length in bytes. */
    public int length() {
        return bytes.length;
    }

    public String asString() {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public DataType type() {
        return DataType.STRING;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof StringValue other && Arrays.equals(bytes, other.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public String toString() {
        return "StringValue[" + asString() + "]";
    }
}
