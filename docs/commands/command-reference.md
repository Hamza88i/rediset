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

## Errors

| Error prefix | Meaning |
|--------------|---------|
| `ERR` | Generic error (unknown command, wrong arity, invalid argument). |
| `WRONGTYPE` | Operation attempted against a key of the wrong type. |

More commands (TTL/expiration, data-structure operations, transactions, pub/sub,
replication, and introspection) are documented here as they are implemented.
