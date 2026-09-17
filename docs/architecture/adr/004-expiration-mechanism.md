# ADR-004: Key expiration mechanism

- **Status:** Accepted
- **Date:** 2026-09-14

## Context

RediSet keys may be given a time-to-live (TTL) via `EXPIRE`/`PEXPIRE`. Once a
key's TTL elapses it must appear to be gone: reads must not return it, `EXISTS`
must report it absent, and `DBSIZE` must not count it. We need a mechanism that
delivers those semantics without wasting resources.

Options considered:

1. **One timer thread per key.** Schedule a deletion task for each key with a
   TTL. Semantically simple but catastrophic at scale: thousands of keys become
   thousands of scheduled tasks/threads, with heavy memory and scheduler
   overhead. **Explicitly rejected.**
2. **Purely lazy expiration.** Only check-and-remove when a key is accessed.
   Cheap and simple, but a key that is set with a TTL and never touched again
   lives forever in memory, so memory is not reclaimed. Insufficient on its own.
3. **Purely active/eager sweeping.** A background thread scans the whole key
   space on an interval and removes expired keys. Reclaims memory, but a full
   scan every tick is O(n) and wasteful when few keys have TTLs.
4. **Lazy expiration + sampled background sweep (chosen).** Combine (2) and a
   bounded, sampling variant of (3).

## Decision

Use **lazy expiration on access, plus a background sweeper that samples a bounded
number of keys with TTLs per tick.**

- **Lazy path:** every keyed read/write checks whether the key has an expiry that
  has passed; if so it removes the key and treats it as absent. This guarantees
  correct semantics the instant a key is observed after expiry, regardless of the
  sweeper.
- **Active path:** a single scheduled sweeper thread wakes on a configurable
  interval (`expiration.sweep-interval-millis`), samples up to
  `expiration.sweep-sample-size` keys that have a TTL, and removes the expired
  ones. This reclaims memory for keys that are never accessed again, at bounded
  cost per tick.

Expiries are tracked in a dedicated `ConcurrentHashMap<String, Long>` (absolute
expiry timestamps in epoch millis) kept alongside the value map, so keys without
a TTL cost nothing extra and the sweeper only iterates keys that actually have
one.

## Consequences

- **Correctness is decoupled from the sweeper's timeliness.** Even if the sweeper
  is slow or sampling misses a key, the lazy check makes an expired key invisible
  the moment anything looks at it.
- **Bounded background cost.** The sweeper never does an unbounded full scan in a
  single tick; work per tick is capped by the sample size.
- **A single scheduler thread**, not one-per-key, so the memory/scheduler cost is
  constant regardless of how many keys carry a TTL.
- **Slight memory lag possible:** a key that expired but is never accessed and not
  yet sampled remains in memory until a future sweep. This is an accepted,
  documented trade-off and is tunable via the two config properties.
- The value map and the expiry map must be kept consistent: deleting a key (for
  any reason) also drops its expiry entry, and overwriting a key with plain `SET`
  clears any previous TTL. These invariants are enforced in `StorageEngine` and
  covered by tests.
