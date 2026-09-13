package io.rediset.protocol;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProtocolDecoderTest {

    private static ProtocolDecoder decoder(String input) {
        return decoder(input.getBytes(StandardCharsets.UTF_8), ProtocolLimits.defaults());
    }

    private static ProtocolDecoder decoder(byte[] input, ProtocolLimits limits) {
        InputStream in = new ByteArrayInputStream(input);
        return new ProtocolDecoder(in, limits);
    }

    @Test
    void shouldDecodeArrayCommand() throws IOException {
        ProtocolDecoder decoder = decoder("*2\r\n$3\r\nGET\r\n$3\r\nfoo\r\n");
        List<byte[]> tokens = decoder.readRequest();
        assertEquals(2, tokens.size());
        assertEquals("GET", new String(tokens.get(0), StandardCharsets.UTF_8));
        assertEquals("foo", new String(tokens.get(1), StandardCharsets.UTF_8));
    }

    @Test
    void shouldDecodeBinarySafeBulkArgument() throws IOException {
        byte[] payload = {'a', '\r', '\n', 0, 'z'};
        java.io.ByteArrayOutputStream raw = new java.io.ByteArrayOutputStream();
        raw.writeBytes("*2\r\n$3\r\nSET\r\n$5\r\n".getBytes(StandardCharsets.UTF_8));
        raw.writeBytes(payload);
        raw.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));

        ProtocolDecoder decoder = decoder(raw.toByteArray(), ProtocolLimits.defaults());
        List<byte[]> tokens = decoder.readRequest();
        assertArrayEquals(payload, tokens.get(1));
    }

    @Test
    void shouldDecodeInlineCommand() throws IOException {
        ProtocolDecoder decoder = decoder("PING\r\n");
        List<byte[]> tokens = decoder.readRequest();
        assertEquals(1, tokens.size());
        assertEquals("PING", new String(tokens.get(0), StandardCharsets.UTF_8));
    }

    @Test
    void shouldDecodeInlineCommandWithArguments() throws IOException {
        ProtocolDecoder decoder = decoder("SET name RediSet\r\n");
        List<byte[]> tokens = decoder.readRequest();
        assertEquals(3, tokens.size());
        assertEquals("RediSet", new String(tokens.get(2), StandardCharsets.UTF_8));
    }

    @Test
    void shouldDecodeInlineCommandWithBareLineFeed() throws IOException {
        ProtocolDecoder decoder = decoder("PING\n");
        List<byte[]> tokens = decoder.readRequest();
        assertEquals("PING", new String(tokens.get(0), StandardCharsets.UTF_8));
    }

    @Test
    void shouldDecodeMultipleSequentialRequests() throws IOException {
        ProtocolDecoder decoder = decoder("*1\r\n$4\r\nPING\r\n*1\r\n$4\r\nPING\r\n");
        assertEquals("PING", new String(decoder.readRequest().get(0), StandardCharsets.UTF_8));
        assertEquals("PING", new String(decoder.readRequest().get(0), StandardCharsets.UTF_8));
    }

    @Test
    void shouldThrowEofOnCleanEndOfStream() {
        ProtocolDecoder decoder = decoder("");
        assertThrows(EOFException.class, decoder::readRequest);
    }

    @Test
    void shouldThrowEofOnTruncatedBulkPayload() {
        ProtocolDecoder decoder = decoder("*1\r\n$10\r\nshort");
        assertThrows(EOFException.class, decoder::readRequest);
    }

    @Test
    void shouldRejectNonBulkElementInArray() {
        ProtocolDecoder decoder = decoder("*1\r\n:5\r\n");
        assertThrows(ProtocolException.class, decoder::readRequest);
    }

    @Test
    void shouldRejectEmptyArrayCommand() {
        ProtocolDecoder decoder = decoder("*0\r\n");
        assertThrows(ProtocolException.class, decoder::readRequest);
    }

    @Test
    void shouldRejectInvalidLength() {
        ProtocolDecoder decoder = decoder("*abc\r\n");
        assertThrows(ProtocolException.class, decoder::readRequest);
    }

    @Test
    void shouldRejectMissingCrLfAfterBulkPayload() {
        ProtocolDecoder decoder = decoder("*1\r\n$3\r\nfooXX");
        assertThrows(ProtocolException.class, decoder::readRequest);
    }

    @Test
    void shouldRejectBulkStringOverLimit() {
        ProtocolLimits tiny = new ProtocolLimits(4, 1024, 1_000_000);
        ProtocolDecoder decoder = decoder("*1\r\n$100\r\n".getBytes(StandardCharsets.UTF_8), tiny);
        assertThrows(ProtocolException.class, decoder::readRequest);
    }

    @Test
    void shouldRejectArrayOverLimit() {
        ProtocolLimits tiny = new ProtocolLimits(1024, 2, 1_000_000);
        ProtocolDecoder decoder = decoder("*5\r\n".getBytes(StandardCharsets.UTF_8), tiny);
        assertThrows(ProtocolException.class, decoder::readRequest);
    }

    @Test
    void shouldRejectRequestOverTotalSizeLimit() {
        ProtocolLimits tiny = new ProtocolLimits(1024, 1024, 5);
        String input = "*2\r\n$3\r\nabc\r\n$3\r\ndef\r\n";
        ProtocolDecoder decoder = decoder(input.getBytes(StandardCharsets.UTF_8), tiny);
        assertThrows(ProtocolException.class, decoder::readRequest);
    }
}
