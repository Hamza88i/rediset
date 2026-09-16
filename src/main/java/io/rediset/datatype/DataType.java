package io.rediset.datatype;

/**
 * The set of value types a RediSet key can hold. Used for type checks and for the
 * {@code TYPE} command's human-readable names.
 */
public enum DataType {
    STRING("string"),
    LIST("list"),
    SET("set"),
    HASH("hash"),
    SORTED_SET("zset");

    private final String displayName;

    DataType(String displayName) {
        this.displayName = displayName;
    }

    /** The lowercase name reported to clients (for example {@code string}). */
    public String displayName() {
        return displayName;
    }
}
