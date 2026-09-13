package io.rediset.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProtocolEncoderTest {

    private static String encoded(Reply reply) {
        return new String(ProtocolEncoder.encode(reply), StandardCharsets.UTF_8);
    }

    @Test
    void shouldEncodeSimpleString() {
        assertEquals("+OK\r\n", encoded(Reply.ok()));
    }

    @Test
    void shouldEncodeError() {
        assertEquals("-ERR bad\r\n", encoded(Reply.error("ERR bad")));
    }

    @Test
    void shouldEncodePositiveAndNegativeIntegers() {
        assertEquals(":42\r\n", encoded(Reply.integer(42)));
        assertEquals(":-7\r\n", encoded(Reply.integer(-7)));
    }

    @Test
    void shouldEncodeBulkString() {
        assertEquals("$5\r\nhello\r\n", encoded(Reply.bulk("hello")));
    }

    @Test
    void shouldEncodeEmptyBulkString() {
        assertEquals("$0\r\n\r\n", encoded(Reply.bulk("")));
    }

    @Test
    void shouldEncodeBinarySafeBulkString() {
        byte[] payload = {'a', '\r', '\n', 0, 'b'};
        byte[] out = ProtocolEncoder.encode(Reply.bulk(payload));
        // Prefix "$5\r\n" then the 5 raw bytes then "\r\n".
        assertEquals('$', out[0]);
        assertEquals('5', out[1]);
        assertEquals(0, out[7]); // the embedded NUL survives
    }

    @Test
    void shouldEncodeNull() {
        assertEquals("_\r\n", encoded(Reply.nil()));
    }

    @Test
    void shouldEncodeArray() {
        Reply array = Reply.array(List.of(Reply.bulk("a"), Reply.integer(1), Reply.nil()));
        assertEquals("*3\r\n$1\r\na\r\n:1\r\n_\r\n", encoded(array));
    }

    @Test
    void shouldEncodeEmptyArray() {
        assertEquals("*0\r\n", encoded(Reply.array(List.of())));
    }

    @Test
    void shouldEncodeNestedArray() {
        Reply inner = Reply.array(List.of(Reply.integer(1), Reply.integer(2)));
        Reply outer = Reply.array(List.of(inner, Reply.bulk("x")));
        assertEquals("*2\r\n*2\r\n:1\r\n:2\r\n$1\r\nx\r\n", encoded(outer));
    }
}
