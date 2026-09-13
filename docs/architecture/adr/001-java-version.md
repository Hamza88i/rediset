# ADR-001: Java 21 as the target runtime

- **Status:** Accepted
- **Date:** 2026-09-13

## Context

RediSet is an in-memory database engine. We need to choose a target JVM version.
The choice affects available language features, concurrency primitives,
performance, long-term support, and how approachable the code is to contributors.

Relevant options at the time of writing:

- **Java 17 (LTS):** widely deployed, stable, but lacks recent concurrency and
  language ergonomics improvements.
- **Java 21 (LTS):** current LTS. Adds records, pattern matching for `switch`,
  sealed types, virtual threads (Project Loom), and sequenced collections.
- **Latest non-LTS (e.g. 22/23):** newest features but short support window,
  unsuitable for something intended to look production-quality.

## Decision

Target **Java 21 (LTS)**.

## Rationale

- It is an LTS release, so it is a defensible base for a project that positions
  itself as production-quality.
- Records and pattern matching make protocol frames and command definitions
  concise and readable, directly serving our "simple, readable Java" goal.
- Sealed interfaces let us model the protocol reply hierarchy and value types in
  a closed, exhaustively-checkable way.
- Virtual threads are available should we choose a thread-per-connection model
  (see ADR-002), giving us a cheap concurrency option without a callback-heavy
  NIO design.
- Sequenced collections simplify list/deque semantics for the List data type.

## Consequences

- Contributors must have a JDK 21 toolchain. The CI pipeline pins JDK 21.
- We can freely use records, sealed types, pattern matching, and virtual
  threads throughout the codebase.
- Migrating to a newer LTS later (e.g. Java 25) should be low-risk since we only
  depend on stabilized features, not preview APIs.
