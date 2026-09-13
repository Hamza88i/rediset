# ADR-002: Networking model — blocking sockets with virtual threads

- **Status:** Accepted
- **Date:** 2026-09-13

## Context

RediSet serves clients over TCP. Before writing any server code we must decide
how the server handles concurrent connections. The two classic options on the
JVM are:

1. **Non-blocking NIO (selector / event loop):** a small number of OS threads
   drive many connections via `Selector`. High scalability, but the code becomes
   an explicit state machine: partial reads, write-readiness, and buffer
   management are all manual. This is error-prone and hard to read.
2. **Blocking sockets, one thread per connection:** each connection gets its own
   thread that reads and writes with simple, linear, blocking calls. Very
   readable, but historically limited because platform threads are expensive
   (~1MB stack, scheduler pressure at thousands of threads).

Java 21 introduces a third practical point in the design space: **virtual
threads** (Project Loom). Virtual threads are cheap (millions are feasible) and
are scheduled onto a small carrier pool. Blocking I/O on a virtual thread parks
the virtual thread without blocking a carrier OS thread.

## Decision

Use **blocking sockets with one virtual thread per client connection.** A single
platform-thread acceptor loop accepts connections and hands each to a virtual
thread that runs the read → decode → execute → write loop with straightforward
blocking calls.

## Rationale

- **Readability first.** The per-connection code reads top to bottom with no
  selector state machine. This directly serves RediSet's priority order
  (correctness > maintainability > clarity > performance).
- **Scalability without NIO complexity.** Virtual threads remove the historical
  reason to avoid thread-per-connection, so we get high connection counts without
  hand-written event loops.
- **Testability.** Blocking client/server code is trivial to drive from
  integration tests using plain `Socket`.
- **Java 21 native.** This leans on a stabilized Java 21 feature (ADR-001)
  rather than a third-party framework, keeping the core engine understandable
  with mostly the JDK standard library.

## Consequences

- We depend on virtual threads (`Thread.ofVirtual()` /
  `Executors.newVirtualThreadPerTaskExecutor()`).
- We must be careful not to hold coarse `synchronized` locks across blocking I/O
  on virtual threads (pinning). RediSet's storage engine uses concurrent
  collections and short critical sections rather than long `synchronized`
  blocks, which avoids this pitfall.
- CPU-bound work per request must stay small; the carrier pool is sized to
  available processors.
- If we ever need extreme single-box connection density with minimal memory, a
  future ADR could revisit NIO for the acceptor path. For RediSet's goals this is
  not necessary.
