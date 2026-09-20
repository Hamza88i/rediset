# Command Reference

This is the authoritative reference for RediSet commands. Replies use RSP types
(see [protocol](../protocol/protocol.md)): simple string (`+`), error (`-`),
integer (`:`), bulk string (`$`), array (`*`), null (`_`).

## Connection

### `PING [message]`

- **Arity:** 1–2 tokens.
- **Reply:** `+PONG` if no argument; otherwise the `message` as a bulk string.

### `ECHO message`

- **Arity:** exactly 2 tokens.
- **Reply:** `message` as a bulk string.

## Keys and strings

### `SET key value`

- **Arity:** exactly 3 tokens.
- **Reply:** `+OK`.
- Stores `value` as a string under `key`, overwriting any existing value of any
  type.

### `GET key`

- **Arity:** exactly 2 tokens.
- **Reply:** the string value as a bulk string, or null if the key is absent.
- **Errors:** `WRONGTYPE` if `key` holds a non-string value.

### `DEL key [key ...]`

- **Arity:** at least 2 tokens.
- **Reply:** integer count of keys actually removed (missing keys are ignored).

### `EXISTS key [key ...]`

- **Arity:** at least 2 tokens.
- **Reply:** integer count of keys that exist. A key given multiple times is
  counted multiple times.

### `KEYS pattern`

- **Arity:** exactly 2 tokens.
- **Reply:** array of matching keys as bulk strings.
- `pattern` supports glob wildcards: `*`, `?`, `[...]` classes (with `[^...]`
  negation), and `\` escaping. `KEYS *` returns all keys. This is an O(n) scan.

### `DBSIZE`

- **Arity:** exactly 1 token.
- **Reply:** integer number of keys.

### `TYPE key`

- **Arity:** exactly 2 tokens.
- **Reply:** simple string naming the value type (`string`, `list`, `set`,
  `hash`, `zset`), or `none` if the key does not exist.

## Expiration (TTL)

### `EXPIRE key seconds`

- **Arity:** exactly 3 tokens.
- **Reply:** `:1` if the timeout was set (or the key deleted for a non-positive
  TTL), `:0` if the key does not exist.

### `PEXPIRE key milliseconds`

- **Arity:** exactly 3 tokens.
- **Reply:** as `EXPIRE`, but the TTL is in milliseconds.

### `TTL key`

- **Arity:** exactly 2 tokens.
- **Reply:** remaining TTL in whole seconds (rounded up), `-1` if the key has no
  expiry, `-2` if the key does not exist.

### `PTTL key`

- **Arity:** exactly 2 tokens.
- **Reply:** remaining TTL in milliseconds, `-1` if no expiry, `-2` if no key.

### `PERSIST key`

- **Arity:** exactly 2 tokens.
- **Reply:** `:1` if an expiry was removed, `:0` if the key does not exist or had
  no expiry.

## Lists

A list is an ordered sequence of binary-safe elements. An empty list deletes its
key.

| Command | Arity | Description |
|---------|-------|-------------|
| `LPUSH key element [element ...]` | ≥3 | Prepend elements; returns new length. |
| `RPUSH key element [element ...]` | ≥3 | Append elements; returns new length. |
| `LPOP key` | 2 | Remove and return the first element, or null. |
| `RPOP key` | 2 | Remove and return the last element, or null. |
| `LRANGE key start stop` | 4 | Elements in `[start, stop]` (negative indices from end). |
| `LLEN key` | 2 | Number of elements, or 0. |
| `LINDEX key index` | 3 | Element at `index` (negative from end), or null. |

## Sets

A set is an unordered collection of unique, binary-safe members. An empty set
deletes its key.

| Command | Arity | Description |
|---------|-------|-------------|
| `SADD key member [member ...]` | ≥3 | Add members; returns the number newly added. |
| `SREM key member [member ...]` | ≥3 | Remove members; returns the number removed. |
| `SMEMBERS key` | 2 | All members (unspecified order). |
| `SISMEMBER key member` | 3 | `:1` if present, else `:0`. |
| `SCARD key` | 2 | Number of members, or 0. |

## Hashes

A hash maps binary-safe fields to binary-safe values. An empty hash deletes its
key.

| Command | Arity | Description |
|---------|-------|-------------|
| `HSET key field value [field value ...]` | ≥4 (even pairs) | Set fields; returns the number of new fields. |
| `HGET key field` | 3 | Field value, or null. |
| `HDEL key field [field ...]` | ≥3 | Remove fields; returns the number removed. |
| `HGETALL key` | 2 | Flat array `[field, value, ...]`. |
| `HKEYS key` | 2 | All field names. |
| `HVALS key` | 2 | All values. |
| `HLEN key` | 2 | Number of fields, or 0. |

## Sorted sets

A sorted set holds unique members ordered by a floating-point score (ties broken
lexicographically). An empty sorted set deletes its key.

| Command | Arity | Description |
|---------|-------|-------------|
| `ZADD key score member [score member ...]` | ≥4 (even pairs) | Add/update members; returns the number newly added. |
| `ZSCORE key member` | 3 | Member's score as a bulk string, or null. |
| `ZRANGE key start stop [WITHSCORES]` | 4–5 | Members by rank; `WITHSCORES` interleaves scores. |
| `ZREM key member [member ...]` | ≥3 | Remove members; returns the number removed. |
| `ZCARD key` | 2 | Number of members, or 0. |

## Errors

| Error prefix | Meaning |
|--------------|---------|
| `ERR` | Generic error (unknown command, wrong arity, invalid argument). |
| `WRONGTYPE` | Operation attempted against a key of the wrong type. |

More commands (TTL/expiration, data-structure operations, transactions, pub/sub,
replication, and introspection) are documented here as they are implemented.
