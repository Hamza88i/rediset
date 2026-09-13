package io.rediset.protocol;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * A value in the RediSet wire protocol (RSP). This sealed hierarchy models the
 * closed set of frame types described in {@code docs/protocol/protocol.md}:
 * simple string, error, integer, bulk string, array, and null.
 *
 * <p>{@code Reply} is used both for outbound replies and, via
 * {@link ProtocolDecoder}, for the inbound request frame (which is always an
 * {@link ArrayReply} of {@link BulkStringReply} elements).
 */
public sealed interface Reply
        permits Reply.SimpleStringReply, Reply.ErrorReply, Reply.IntegerReply,
                Reply.BulkStringReply, Reply.ArrayReply, Reply.NullReply {

    /** A short, CR/LF-free status string, encoded with the {@code +} marker. */
    record SimpleStringReply(String value) implements Reply {
        public SimpleStringReply {
            Objects.requireNonNull(value, "value");
            requireNoLineBreaks(value);
        }
    }

    /** An error message, encoded with the {@code -} marker. */
    record ErrorReply(String message) implements Reply {
        public ErrorReply {
            Objects.requireNonNull(message, "message");
            requireNoLineBreaks(message);
        }
    }

    /** A signed 64-bit integer, encoded with the {@code :} marker. */
    record IntegerReply(long value) implements Reply {
    }

    /** A binary-safe byte payload, encoded with the {@code $} marker. */
    record BulkStringReply(byte[] value) implements Reply {
        public BulkStringReply {
            Objects.requireNonNull(value, "value");
        }

        public static BulkStringReply of(String text) {
            return new BulkStringReply(text.getBytes(StandardCharsets.UTF_8));
        }

        public String asString() {
            return new String(value, StandardCharsets.UTF_8);
        }

        // Records with array components get identity-based equals/hashCode by
        // default; override for value semantics used in tests and comparisons.
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            return o instanceof BulkStringReply other
                    && java.util.Arrays.equals(value, other.value);
        }

        @Override
        public int hashCode() {
            return java.util.Arrays.hashCode(value);
        }

        @Override
        public String toString() {
            return "BulkStringReply[" + asString() + "]";
        }
    }

    /** An ordered collection of replies, encoded with the {@code *} marker. */
    record ArrayReply(List<Reply> elements) implements Reply {
        public ArrayReply {
            Objects.requireNonNull(elements, "elements");
            elements = List.copyOf(elements);
        }
    }

    /** The null value, encoded with the {@code _} marker. */
    record NullReply() implements Reply {
        private static final NullReply INSTANCE = new NullReply();

        public static NullReply instance() {
            return INSTANCE;
        }
    }

    // --- Convenience factories -------------------------------------------------

    static Reply ok() {
        return new SimpleStringReply("OK");
    }

    static Reply simple(String value) {
        return new SimpleStringReply(value);
    }

    static Reply error(String message) {
        return new ErrorReply(message);
    }

    static Reply integer(long value) {
        return new IntegerReply(value);
    }

    static Reply bulk(String value) {
        return BulkStringReply.of(value);
    }

    static Reply bulk(byte[] value) {
        return new BulkStringReply(value);
    }

    static Reply nil() {
        return NullReply.instance();
    }

    static Reply array(List<Reply> elements) {
        return new ArrayReply(elements);
    }

    private static void requireNoLineBreaks(String value) {
        if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("value must not contain CR or LF");
        }
    }
}
