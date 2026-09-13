package io.rediset.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A TCP server that accepts client connections and services each one on its own
 * virtual thread (see ADR-002). The read/decode/execute/write loop itself is
 * delegated to a {@link ConnectionHandler}, keeping this class focused on socket
 * lifecycle, concurrency, connection accounting, and graceful shutdown.
 *
 * <p>The server is thread-safe with respect to {@link #start()} and
 * {@link #shutdown()}: shutdown may be invoked from any thread (for example a JVM
 * shutdown hook) and is idempotent.
 */
public final class RedisetServer {

    private static final Logger log = LoggerFactory.getLogger(RedisetServer.class);

    private static final int SHUTDOWN_GRACE_SECONDS = 10;
    private static final int ACCEPT_SO_TIMEOUT_MILLIS = 250;

    private final NetworkConfig config;
    private final ConnectionHandler handler;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong connectionIdSequence = new AtomicLong();
    private final Semaphore connectionPermits;
    private final Set<Socket> openSockets = ConcurrentHashMap.newKeySet();
    private final CountDownLatch stopped = new CountDownLatch(1);

    private volatile ServerSocket serverSocket;
    private volatile ExecutorService connectionExecutor;
    private volatile Thread acceptThread;
    private volatile int boundPort = -1;

    public RedisetServer(NetworkConfig config, ConnectionHandler handler) {
        this.config = config;
        this.handler = handler;
        this.connectionPermits = new Semaphore(config.maxClients());
    }

    /**
     * Binds the listening socket and starts accepting connections on a dedicated
     * acceptor thread. Returns once the socket is bound; connection servicing
     * happens asynchronously.
     */
    public synchronized void start() throws IOException {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Server already started");
        }

        ServerSocket socket = new ServerSocket();
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(config.host(), config.port()));
        socket.setSoTimeout(ACCEPT_SO_TIMEOUT_MILLIS);
        this.serverSocket = socket;
        this.boundPort = socket.getLocalPort();
        this.connectionExecutor = Executors.newVirtualThreadPerTaskExecutor();

        Thread thread = new Thread(this::acceptLoop, "rediset-acceptor");
        thread.setDaemon(false);
        this.acceptThread = thread;
        thread.start();

        log.info("RediSet listening on {}:{} (max clients: {})",
                config.host(), boundPort, config.maxClients());
    }

    private void acceptLoop() {
        while (running.get()) {
            Socket client;
            try {
                client = serverSocket.accept();
            } catch (SocketException e) {
                // Socket closed as part of shutdown; exit quietly if stopping.
                if (running.get()) {
                    log.warn("Accept failed", e);
                }
                break;
            } catch (java.net.SocketTimeoutException e) {
                // Periodic wakeup so we can observe the running flag.
                continue;
            } catch (IOException e) {
                if (running.get()) {
                    log.warn("Accept failed", e);
                }
                continue;
            }

            if (!connectionPermits.tryAcquire()) {
                log.warn("Rejecting connection from {}: max clients ({}) reached",
                        client.getRemoteSocketAddress(), config.maxClients());
                closeQuietly(client);
                continue;
            }

            openSockets.add(client);
            connectionExecutor.execute(() -> serviceConnection(client));
        }
        stopped.countDown();
    }

    private void serviceConnection(Socket socket) {
        long id = connectionIdSequence.incrementAndGet();
        ClientSession session = new ClientSession(id, socket.getRemoteSocketAddress());
        try {
            configureSocket(socket);
            log.debug("Client connected: {}", session);
            try (InputStream in = socket.getInputStream();
                 OutputStream out = socket.getOutputStream()) {
                handler.handle(session, in, out);
            }
        } catch (SocketException e) {
            log.debug("Client {} disconnected: {}", session.id(), e.getMessage());
        } catch (IOException e) {
            log.debug("I/O error on client {}: {}", session.id(), e.getMessage());
        } catch (RuntimeException e) {
            // A malformed request or handler bug must never take down the server.
            log.warn("Unhandled error servicing client {}", session.id(), e);
        } finally {
            openSockets.remove(socket);
            closeQuietly(socket);
            connectionPermits.release();
            log.debug("Client disconnected: {}", session);
        }
    }

    private void configureSocket(Socket socket) throws SocketException {
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        int idle = config.idleTimeoutSeconds();
        if (idle > 0) {
            socket.setSoTimeout((int) Duration.ofSeconds(idle).toMillis());
        }
    }

    /**
     * Initiates a graceful shutdown: stops accepting new connections, closes the
     * listening socket, waits briefly for in-flight connections to drain, then
     * forcibly closes any that remain and stops the connection executor.
     * Idempotent and safe to call from any thread.
     */
    public void shutdown() {
        if (!running.compareAndSet(true, false)) {
            return;
        }
        log.info("Shutting down RediSet server...");

        closeQuietly(serverSocket);
        awaitAcceptLoopStop();

        ExecutorService executor = connectionExecutor;
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(SHUTDOWN_GRACE_SECONDS, TimeUnit.SECONDS)) {
                    log.info("Forcing {} in-flight connection(s) to close", openSockets.size());
                    openSockets.forEach(RedisetServer::closeQuietly);
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }
        }
        log.info("RediSet server stopped");
    }

    private void awaitAcceptLoopStop() {
        try {
            stopped.await(SHUTDOWN_GRACE_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Thread thread = acceptThread;
        if (thread != null) {
            thread.interrupt();
        }
    }

    /** Blocks the calling thread until the server stops accepting connections. */
    public void awaitTermination() throws InterruptedException {
        stopped.await();
    }

    public boolean isRunning() {
        return running.get();
    }

    /** Returns the actually bound port (useful when configured with port 0). */
    public int boundPort() {
        return boundPort;
    }

    /** Returns the current number of active client connections. */
    public int activeConnections() {
        return openSockets.size();
    }

    private static void closeQuietly(java.io.Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
            // Best-effort close during cleanup.
        }
    }
}
