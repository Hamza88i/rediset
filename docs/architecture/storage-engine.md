# Storage Engine

The storage engine is RediSet's in-memory key space: a mapping from string keys
to typed values.

## Data model

| Concept | Type | Notes |
|---------|------|-------|
| Key | `String` | UTF-8 decoded from the wire. |
| Value | `RedisetValue` | Sealed set of types: string, list, set, hash, sorted set. |
| Type tag | `DataType` | Every value reports its type for type-checking. |

The engine stores values in a single `ConcurrentHashMap<String, RedisetValue>`.

## Thread-safety

The engine holds **no global lock**. Instead:

- Individual operations (`get`, `put`, `delete`, `exists`) are atomic because they
  map directly onto `ConcurrentHashMap` operations.
- Compound read-modify-write operations that must be atomic (for example a future
  `INCR`, or list/set mutations) are expressed with the map's atomic combinators
  `compute` / `computeIfAbsent`, or delegate to the value type's own internal
  synchronization.
- Unrelated keys never contend with one another, so throughput scales with the
  number of independent keys under contention.

This model is analyzed in more depth, with tests, under
[concurrency](concurrency.md) and [ADR-006](adr/006-concurrency-model.md).

## Type safety

`StorageEngine.getTyped(key, class, expectedType)` returns the value only if it
matches the expected `DataType`; otherwise it throws `WrongTypeException`, which
the command layer renders as `-WRONGTYPE ...`. Because a mismatched type never
returns a value, no command can corrupt a value by treating it as the wrong type.

## String values

`StringValue` wraps a defensively-copied `byte[]`, so it is:

- **Binary-safe** — any byte sequence can be stored and retrieved intact.
- **Immutable** — writes replace the whole value, so a reader never observes a
  half-updated payload.

## MVP commands (this step)

| Command | Arity | Reply | Description |
|---------|-------|-------|-------------|
| `SET key value` | 3 | `+OK` | Store a string value, overwriting any existing value. |
| `GET key` | 2 | bulk / null | Return the string value, or null if absent. `WRONGTYPE` if non-string. |
| `DEL key [key ...]` | ≥2 | integer | Delete keys; return the number removed. |
| `EXISTS key [key ...]` | ≥2 | integer | Return how many of the given keys exist (duplicates counted). |
| `KEYS pattern` | 2 | array | Return keys matching a glob pattern (`*`, `?`, `[...]`). |
| `DBSIZE` | 1 | integer | Return the number of keys. |
| `TYPE key` | 2 | simple | Return the value's type name, or `none`. |

`KEYS` performs an O(n) scan and is intended for debugging and small data sets.
