package io.rediset.protocol;

import io.rediset.protocol.Reply.ArrayReply;
import io.rediset.protocol.Reply.BulkStringReply;
import io.rediset.protocol.Reply.ErrorReply;
import io.rediset.protocol.Reply.IntegerReply;
import io.rediset.protocol.Reply.NullReply;
import io.rediset.protocol.Reply.SimpleStringReply;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Serializes {@link Reply} values into RediSet wire protocol (RSP) bytes.
 *
 * <p>Encoding is a pure function of the reply; the encoder holds no per-connection
 * state and its static methods are safe to call from any thread.
 */
public final class ProtocolEncoder {

    private static final byte[] CRLF = {'\r', '\n'};

    private ProtocolEncoder() {
    }

    /** Encodes a reply to a fresh byte array. */
    public static byte[] encode(Reply reply) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            write(out, reply);
        } catch (IOException e) {
            // ByteArrayOutputStream never throws IOException.
            throw new IllegalStateException("unexpected I/O error encoding reply", e);
        }
        return out.toByteArray();
    }

    /** Writes a reply directly to an output stream. */
    public static void write(OutputStream out, Reply reply) throws IOException {
        switch (reply) {
            case SimpleStringReply r -> writeLine(out, '+', r.value());
            case ErrorReply r -> writeLine(out, '-', r.message());
            case IntegerReply r -> writeLine(out, ':', Long.toString(r.value()));
            case BulkStringReply r -> writeBulk(out, r.value());
            case ArrayReply r -> writeArray(out, r);
            case NullReply ignored -> {
                out.write('_');
                out.write(CRLF);
            }
        }
    }

    private static void writeLine(OutputStream out, char marker, String value) throws IOException {
        out.write(marker);
        out.write(value.getBytes(StandardCharsets.UTF_8));
        out.write(CRLF);
    }

    private static void writeBulk(OutputStream out, byte[] value) throws IOException {
        out.write('$');
        out.write(Integer.toString(value.length).getBytes(StandardCharsets.US_ASCII));
        out.write(CRLF);
        out.write(value);
        out.write(CRLF);
    }

    private static void writeArray(OutputStream out, ArrayReply array) throws IOException {
        out.write('*');
        out.write(Integer.toString(array.elements().size()).getBytes(StandardCharsets.US_ASCII));
        out.write(CRLF);
        for (Reply element : array.elements()) {
            write(out, element);
        }
    }
}
