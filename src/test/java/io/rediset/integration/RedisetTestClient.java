package io.rediset.integration;

import io.rediset.protocol.ProtocolEncoder;
import io.rediset.protocol.Reply;
import io.rediset.protocol.Reply.ArrayReply;
import io.rediset.protocol.Reply.BulkStringReply;
import io.rediset.protocol.Reply.ErrorReply;
import io.rediset.protocol.Reply.IntegerReply;
import io.rediset.protocol.Reply.NullReply;
import io.rediset.protocol.Reply.SimpleStringReply;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A minimal RSP client used by integration tests. It sends commands in the
 * array-of-bulk-strings request form and decodes a single reply frame.
 */
final class RedisetTestClient implements AutoCloseable {

    private final Socket socket;
    private final OutputStream out;
    private final BufferedInputStream in;

    RedisetTestClient(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.out = socket.getOutputStream();
        this.in = new BufferedInputStream(socket.getInputStream());
    }

    /** Sends a command and returns the decoded reply. */
    Reply command(String... tokens) throws IOException {
        List<Reply> args = new ArrayList<>();
        for (String token : tokens) {
            args.add(Reply.bulk(token));
        }
        out.write(ProtocolEncoder.encode(new ArrayReply(args)));
        out.flush();
        return readReply();
    }

    /** Sends a raw inline line (for testing the inline command form). */
    Reply inline(String line) throws IOException {
        out.write((line + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
        return readReply();
    }

    private Reply readReply() throws IOException {
        int marker = in.read();
        if (marker == -1) {
            throw new EOFException("server closed connection");
        }
        return switch (marker) {
            case '+' -> new SimpleStringReply(readLine());
            case '-' -> new ErrorReply(readLine());
            case ':' -> new IntegerReply(Long.parseLong(readLine()));
            case '_' -> {
                readLine();
                yield NullReply.instance();
            }
            case '$' -> readBulk();
            case '*' -> readArray();
            default -> throw new IOException("unexpected marker: " + (char) marker);
        };
    }

    private Reply readBulk() throws IOException {
        int length = Integer.parseInt(readLine());
        if (length < 0) {
            yieldNothing();
            return NullReply.instance();
        }
        byte[] payload = in.readNBytes(length);
        readLine(); // trailing CRLF
        return new BulkStringReply(payload);
    }

    private void yieldNothing() {
        // no-op helper for readability
    }

    private Reply readArray() throws IOException {
        int count = Integer.parseInt(readLine());
        List<Reply> elements = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            elements.add(readReply());
        }
        return new ArrayReply(elements);
    }

    private String readLine() throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\r') {
                in.read(); // consume LF
                break;
            }
            buffer.write(b);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
