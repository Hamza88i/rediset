# Networking

RediSet uses a blocking-socket, thread-per-connection design powered by Java 21
virtual threads. The rationale for this over a non-blocking NIO event loop is
recorded in [ADR-002](adr/002-networking-model.md).

## Components

| Class | Responsibility |
|-------|----------------|
| `RedisetServer` | Owns the `ServerSocket`, runs the acceptor loop, enforces the max-clients limit, tracks open sockets, and performs graceful shutdown. |
| `ClientSession` | Per-connection state: numeric id, remote address, liveness flags, last-activity clock, and an attachment slot for higher layers. |
| `ConnectionHandler` | Strategy interface for the per-connection read/decode/execute/write loop. Keeps the `server` package independent of the protocol and command layers. |
| `NetworkConfig` | Immutable, validated view of the network settings (host, port, max clients, idle timeout). |

## Connection lifecycle

1. A single non-daemon **acceptor thread** (`rediset-acceptor`) loops on
   `ServerSocket.accept()`. The listening socket uses a short `SO_TIMEOUT` so the
   loop periodically wakes to observe the running flag, enabling prompt shutdown.
2. On accept, the server tries to acquire a **connection permit** from a
   `Semaphore` sized to `server.max-clients`. If none is available the connection
   is rejected and closed immediately, protecting the server from overload.
3. The accepted socket is registered in an open-sockets set and handed to a
   **virtual thread** from a `newVirtualThreadPerTaskExecutor()`.
4. The virtual thread creates a `ClientSession`, configures the socket
   (`TCP_NODELAY`, keep-alive, optional idle `SO_TIMEOUT`), and invokes the
   `ConnectionHandler`.
5. When the handler returns (client disconnect, EOF, or error), the socket is
   removed from the set, closed, and the connection permit is released.

## Fault isolation

A malformed request, an `IOException`, or even an unexpected `RuntimeException`
in a handler is caught per-connection and logged; it closes only that one
connection and **never** takes down the acceptor or other clients. This is
verified by the networking integration tests.

## Idle connections

If `server.idle-timeout-seconds` is greater than zero, the socket's read timeout
is set accordingly. A read that exceeds the timeout raises a
`SocketTimeoutException`, which the handler treats as a disconnect and the
connection is cleaned up. A value of `0` disables the idle timeout.

## Graceful shutdown

`RedisetServer.shutdown()` is idempotent and safe to call from any thread
(including a JVM shutdown hook). It:

1. Flips the running flag so the acceptor loop exits.
2. Closes the listening socket (stops accepting new connections).
3. Waits for the acceptor loop to finish, then interrupts it if necessary.
4. Shuts down the connection executor and waits up to a grace period for
   in-flight connections to drain.
5. Forcibly closes any remaining sockets and stops the executor.

No sockets, threads, or executors are leaked: the acceptor thread terminates, the
virtual-thread executor is shut down, and all tracked sockets are closed.
