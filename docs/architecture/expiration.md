# Expiration (TTL)

RediSet keys can be given a time-to-live. Expiration follows the **lazy + active
sampling** strategy decided in [ADR-004](adr/004-expiration-mechanism.md); the
alternative of one timer thread per key is explicitly rejected there.

## Two cooperating mechanisms

### 1. Lazy expiration (on access)

Every keyed access in `StorageEngine` first calls `expireIfNeeded(key)`. If the
key has an expiry timestamp that is now in the past, the key and its expiry entry
are removed and the access proceeds as if the key were absent. This guarantees
correct semantics the instant a key is observed after it expires, independent of
the background sweeper.

Reads and writes covered: `get`, `getTyped`, `exists`, `putIfAbsent`, `compute`,
`getOrCreate`, `ttlMillis`, `keys`, and `size` (which sweeps its counted set).

### 2. Active sweeping (background)

`ExpirationSweeper` runs a **single** scheduled daemon thread. Every
`expiration.sweep-interval-millis` it asks the engine to examine up to
`expiration.sweep-sample-size` keys that currently have a TTL and removes any that
have expired. This reclaims memory for keys that expire but are never accessed
again, at a bounded, predictable cost per tick. A failure in one sweep is logged
and never kills the scheduler thread.

## Consistency invariants

The value map and the expiry map are kept consistent inside `StorageEngine`:

- Deleting a key (for any reason, including lazy/active expiry) also removes its
  expiry entry.
- Overwriting a key with plain `SET` (`put`) clears any previous TTL.
- Setting an expiry in the past deletes the key immediately rather than storing a
  timestamp that is already elapsed.
- When lazily expiring, the expiry entry is removed before the value, so a
  concurrent reader never sees a live value carrying a dangling expired timestamp.

## Testability

Expiration time is read through a `Clock` abstraction. Tests inject a controllable
fake clock and advance it explicitly, so TTL behavior is verified deterministically
without real sleeps.

## Commands

| Command | Description | Reply |
|---------|-------------|-------|
| `EXPIRE key seconds` | Set a TTL in seconds. Non-positive TTL deletes the key. | `:1` set/deleted, `:0` no such key |
| `PEXPIRE key milliseconds` | Set a TTL in milliseconds. | `:1` / `:0` |
| `TTL key` | Remaining TTL in seconds. | seconds, `-1` no expiry, `-2` no key |
| `PTTL key` | Remaining TTL in milliseconds. | millis, `-1` no expiry, `-2` no key |
| `PERSIST key` | Remove a key's TTL. | `:1` removed, `:0` no key / no TTL |
