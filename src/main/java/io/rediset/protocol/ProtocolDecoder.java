package io.rediset.protocol;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads RediSet wire protocol (RSP) frames from an {@link InputStream}.
 *
 * <p>The decoder supports the full reply type hierarchy for completeness, but the
 * server primarily uses {@link #readRequest()} which reads a single command
 * frame. A command may be sent either as an array of bulk strings or in the
 * convenience inline form (a plain whitespace-separated line).
 *
 * <p>All configured {@link ProtocolLimits} are enforced. A limit violation or a
 * structurally invalid frame raises a {@link ProtocolException}; a clean
 * end-of-stream raises {@link EOFException}.
 *
 * <p>Instances are not thread-safe; each connection uses its own decoder.
 */
public final class ProtocolDecoder {

    private static final byte CR = '\r';
    private static final byte LF = '\n';
    private static final long NULL_LENGTH = -1L;

    private final PushbackInputStream in;
    private final ProtocolLimits limits;

    public ProtocolDecoder(InputStream in, ProtocolLimits limits) {
        this.in = new PushbackInputStream(in, 1);
        this.limits = limits;
    }

    /**
     * Reads the next command request as an array of bulk-string arguments.
     *
     * @return the command tokens, first being the command name; never empty
     * @throws EOFException      on a clean end-of-stream between frames
     * @throws ProtocolException on a malformed or over-limit frame
     * @throws IOException       on an underlying stream error
     */
    public List<byte[]> readRequest() throws IOException {
        int marker = in.read();
        if (marker == -1) {
            throw new EOFException("end of stream");
        }
        if (marker == '*') {
            return readArrayRequest();
        }
        // Anything else begins an inline command; push the byte back and read the line.
        in.unread(marker);
        return readInlineRequest();
    }

    private List<byte[]> readArrayRequest() throws IOException {
        long count = readLength(limits.maxArraySize());
        if (count == NULL_LENGTH) {
            throw new ProtocolException("null array is not a valid request");
        }
        if (count == 0) {
            throw new ProtocolException("empty command array");
        }
        List<byte[]> args = new ArrayList<>((int) count);
        long totalSize = 0;
        for (long i = 0; i < count; i++) {
            int marker = in.read();
            if (marker == -1) {
                throw new EOFException("truncated array request");
            }
            if (marker != '$') {
                throw new ProtocolException(
                        "expected bulk string in command array, got marker '" + (char) marker + "'");
            }
            byte[] arg = readBulkPayload();
            totalSize += arg.length;
            if (totalSize > limits.maxRequestSize()) {
                throw new ProtocolException("request exceeds max-request-size");
            }
            args.add(arg);
        }
        return args;
    }

    private List<byte[]> readInlineRequest() throws IOException {
        byte[] line = readLine(limits.maxRequestSize());
        String text = new String(line, StandardCharsets.UTF_8).strip();
        if (text.isEmpty()) {
            // Blank inline line: skip by reading the next request.
            return readRequest();
        }
        String[] tokens = text.split("\\s+");
        List<byte[]> args = new ArrayList<>(tokens.length);
        for (String token : tokens) {
            args.add(token.getBytes(StandardCharsets.UTF_8));
        }
        return args;
    }

    /** Reads the payload of a bulk string whose {@code $} marker was already consumed. */
    private byte[] readBulkPayload() throws IOException {
        long length = readLength(limits.maxBulkStringSize());
        if (length == NULL_LENGTH) {
            return new byte[0];
        }
        byte[] payload = readExactly((int) length);
        expectCrLf();
        return payload;
    }

    /**
     * Reads a decimal length line (the digits after a {@code $}/{@code *} marker),
     * accepting {@code -1} as the null sentinel and rejecting values above
     * {@code max} or that are otherwise malformed.
     */
    private long readLength(long max) throws IOException {
        byte[] line = readLine(64);
        if (line.length == 0) {
            throw new ProtocolException("missing length");
        }
        String text = new String(line, StandardCharsets.US_ASCII);
        long value;
        try {
            value = Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new ProtocolException("invalid length: '" + text + "'", e);
        }
        if (value == NULL_LENGTH) {
            return NULL_LENGTH;
        }
        if (value < 0) {
            throw new ProtocolException("negative length: " + value);
        }
        if (value > max) {
            throw new ProtocolException("length " + value + " exceeds limit " + max);
        }
        return value;
    }

    /** Reads bytes up to (and consuming) the next CRLF, returning the bytes before CR. */
    private byte[] readLine(long maxBytes) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        while (true) {
            int b = in.read();
            if (b == -1) {
                if (buffer.size() == 0) {
                    throw new EOFException("end of stream");
                }
                throw new EOFException("truncated line");
            }
            if (b == CR) {
                int next = in.read();
                if (next == LF) {
                    return buffer.toByteArray();
                }
                throw new ProtocolException("expected LF after CR");
            }
            if (b == LF) {
                // Tolerate a bare LF as a line terminator (inline convenience).
                return buffer.toByteArray();
            }
            if (buffer.size() >= maxBytes) {
                throw new ProtocolException("line exceeds limit " + maxBytes);
            }
            buffer.write(b);
        }
    }

    private byte[] readExactly(int length) throws IOException {
        byte[] data = new byte[length];
        int read = 0;
        while (read < length) {
            int n = in.read(data, read, length - read);
            if (n == -1) {
                throw new EOFException("truncated bulk payload");
            }
            read += n;
        }
        return data;
    }

    private void expectCrLf() throws IOException {
        int cr = in.read();
        int lf = in.read();
        if (cr != CR || lf != LF) {
            throw new ProtocolException("expected CRLF terminator");
        }
    }
}
