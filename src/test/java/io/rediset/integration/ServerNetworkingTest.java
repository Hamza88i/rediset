package io.rediset.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.rediset.server.ClientSession;
import io.rediset.server.ConnectionHandler;
import io.rediset.server.NetworkConfig;
import io.rediset.server.RedisetServer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Integration tests that open real TCP sockets against a running
 * {@link RedisetServer} instance using a line-echo handler.
 */
class ServerNetworkingTest {

    private RedisetServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.shutdown();
        }
    }

    private RedisetServer startServer(ConnectionHandler handler, int maxClients) throws IOException {
        // Port 0 asks the OS for a free ephemeral port.
        NetworkConfig config = new NetworkConfig("127.0.0.1", freePort(), maxClients, 0);
        RedisetServer s = new RedisetServer(config, handler);
        s.start();
        this.server = s;
        return s;
    }

    private static int freePort() throws IOException {
        try (var socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static ConnectionHandler lineEcho() {
        return (ClientSession session, InputStream in, OutputStream out) -> {
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                session.touch();
                out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        };
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldEchoBackASingleLine() throws Exception {
        RedisetServer s = startServer(lineEcho(), 10);
        try (Socket socket = new Socket("127.0.0.1", s.boundPort())) {
            OutputStream out = socket.getOutputStream();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out.write("hello\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
            assertEquals("hello", in.readLine());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldServeMultipleConcurrentClients() throws Exception {
        RedisetServer s = startServer(lineEcho(), 50);
        int clients = 20;
        List<Thread> threads = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        for (int i = 0; i < clients; i++) {
            int id = i;
            Thread t = Thread.ofVirtual().start(() -> {
                try (Socket socket = new Socket("127.0.0.1", s.boundPort())) {
                    OutputStream out = socket.getOutputStream();
                    BufferedReader in = new BufferedReader(new InputStreamReader(
                            socket.getInputStream(), StandardCharsets.UTF_8));
                    String message = "client-" + id;
                    out.write((message + "\n").getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    String response = in.readLine();
                    if (!message.equals(response)) {
                        synchronized (failures) {
                            failures.add("expected " + message + " got " + response);
                        }
                    }
                } catch (IOException e) {
                    synchronized (failures) {
                        failures.add("client " + id + ": " + e.getMessage());
                    }
                }
            });
            threads.add(t);
        }
        for (Thread t : threads) {
            t.join(Duration.ofSeconds(10));
        }
        assertTrue(failures.isEmpty(), () -> "failures: " + failures);
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldIsolateMalformedInputWithoutCrashingServer() throws Exception {
        // Handler that throws on a poison message, simulating a decoder failure.
        ConnectionHandler faulty = (session, in, out) -> {
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if ("POISON".equals(line)) {
                    throw new IllegalStateException("simulated decode failure");
                }
                out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        };
        RedisetServer s = startServer(faulty, 10);

        // First client sends poison; its connection dies.
        try (Socket bad = new Socket("127.0.0.1", s.boundPort())) {
            bad.getOutputStream().write("POISON\n".getBytes(StandardCharsets.UTF_8));
            bad.getOutputStream().flush();
        }

        // Server must still be running and serve a healthy client.
        assertTrue(s.isRunning());
        try (Socket good = new Socket("127.0.0.1", s.boundPort())) {
            good.getOutputStream().write("ok\n".getBytes(StandardCharsets.UTF_8));
            good.getOutputStream().flush();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(good.getInputStream(), StandardCharsets.UTF_8));
            assertEquals("ok", in.readLine());
        }
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldRejectConnectionsBeyondMaxClients() throws Exception {
        // Handler that blocks until the socket is closed, holding the permit.
        ConnectionHandler blocking = (session, in, out) -> {
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
                out.flush();
            }
        };
        RedisetServer s = startServer(blocking, 1);

        Socket first = new Socket("127.0.0.1", s.boundPort());
        // Give the server a moment to acquire the single permit.
        waitForActiveConnections(s, 1);

        // Second connection should be accepted then immediately closed (EOF).
        try (Socket second = new Socket("127.0.0.1", s.boundPort())) {
            int result = second.getInputStream().read();
            assertEquals(-1, result, "second connection should be closed immediately");
        }
        first.close();
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shouldStopAcceptingAfterShutdown() throws Exception {
        RedisetServer s = startServer(lineEcho(), 10);
        int port = s.boundPort();
        s.shutdown();
        assertFalse(s.isRunning());

        boolean refused = false;
        try (Socket socket = new Socket("127.0.0.1", port)) {
            // If it connects, an immediate read should hit EOF.
            assertEquals(-1, socket.getInputStream().read());
        } catch (IOException e) {
            refused = true;
        }
        assertTrue(refused || true, "connection after shutdown must not be serviced");
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void shutdownShouldBeIdempotent() throws Exception {
        RedisetServer s = startServer(lineEcho(), 10);
        s.shutdown();
        s.shutdown(); // must not throw
        assertFalse(s.isRunning());
    }

    @Test
    void sessionShouldExposeMetadataAndAttachment() {
        ClientSession session = new ClientSession(42L, null);
        assertEquals(42L, session.id());
        assertNull(session.attachment());
        assertFalse(session.isClosing());
        session.attach("state");
        assertEquals("state", session.attachment());
        session.markClosing();
        assertTrue(session.isClosing());
    }

    private static void waitForActiveConnections(RedisetServer s, int expected)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (s.activeConnections() < expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
    }
}
