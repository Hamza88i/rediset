# The RediSet Wire Protocol (RSP)

RediSet speaks its own line-based, binary-safe request/response protocol called
**RSP** (RediSet Serialization Protocol). It is inspired by the general shape of
text protocols for in-memory databases but is an original design: the type markers,
framing rules, and limits below are defined by RediSet and are not copied from any
other project. RediSet does **not** claim compatibility with any other protocol.

## Goals

- Human-readable enough to debug with `nc`/`telnet`.
- Binary-safe: bulk payloads carry an explicit length, so any byte (including
  `\r`, `\n`, and NUL) is legal inside a bulk string.
- Cheap to parse incrementally from a stream.
- Small, closed set of reply types.

## Framing

Every frame ends with a **terminator** of the two bytes carriage-return and
line-feed: `\r\n` (referred to below as `CRLF`). The first byte of a frame is a
**type marker** that determines how the rest of the frame is read.

| Marker | Type | Direction |
|:------:|------|-----------|
| `+` | Simple string | reply |
| `-` | Error | reply |
| `:` | Integer | reply |
| `$` | Bulk string | request & reply |
| `*` | Array | request & reply |
| `_` | Null | reply |

### Simple string — `+`

```
+<text>CRLF
```

`<text>` must not contain CR or LF. Used for short status replies such as `+OK`.

### Error — `-`

```
-<message>CRLF
```

Same constraints as a simple string. By convention the message begins with an
uppercase error code word, for example:

```
-ERR unknown command 'FOO'
-WRONGTYPE Operation against a key holding the wrong kind of value
```

### Integer — `:`

```
:<signed 64-bit decimal>CRLF
```

For example `:1000\r\n`. The value fits in a Java `long`.

### Bulk string — `$`

```
$<length>CRLF<length bytes>CRLF
```

`<length>` is a non-negative decimal count of the payload bytes that follow. The
payload is exactly `<length>` bytes and is **binary-safe**. It is followed by a
trailing `CRLF`. Example: `$5\r\nhello\r\n`.

A length of `-1` (`$-1\r\n` with no payload) is also accepted on input as a
synonym for null, but RediSet emits null using the dedicated `_` marker.

### Array — `*`

```
*<count>CRLF<element><element>...
```

`<count>` is a non-negative decimal number of elements that follow, each a full
RSP frame. Arrays may nest. A count of `-1` (`*-1\r\n`) denotes a null array on
input; RediSet emits null using `_`.

### Null — `_`

```
_CRLF
```

A single, unambiguous null value used in replies (for example `GET` of a missing
key).

## Requests: the command frame

A client sends a command as an **array of bulk strings**. The first element is
the command name (case-insensitive); the remaining elements are the arguments.
For example, `SET name RediSet` is sent as:

```
*3\r\n$3\r\nSET\r\n$4\r\nname\r\n$7\r\nRediSet\r\n
```

### Inline commands (convenience)

To make manual testing with `telnet`/`nc` easy, RediSet also accepts an
**inline** form: a plain line of whitespace-separated tokens terminated by
`CRLF` (or a bare `LF`). For example, typing:

```
PING
```

is equivalent to the array form `*1\r\n$4\r\nPING\r\n`. Inline commands do not
support binary-safe arguments; use the array form for arbitrary bytes.

## Limits (backpressure)

To protect the server from hostile or buggy clients, the decoder enforces
configurable limits. Exceeding a limit produces a protocol error and closes the
connection:

| Limit | Property | Meaning |
|-------|----------|---------|
| Max bulk string size | `protocol.max-bulk-string-size` | Largest single bulk payload. |
| Max array size | `protocol.max-array-size` | Largest element count in an array. |
| Max request size | `protocol.max-request-size` | Largest total bytes for one request frame. |

## Error handling

- An unknown type marker is a fatal protocol error.
- A malformed length (non-numeric, negative where not allowed, or overflowing) is
  a fatal protocol error.
- A missing or incorrect terminator is a fatal protocol error.
- A truncated stream (EOF mid-frame) is reported as end-of-stream, which the
  server treats as a client disconnect.

Fatal protocol errors are reported to the client as an `-ERR ...` reply where
possible, after which the connection is closed. A single client's protocol
violation never affects other clients (see the networking fault-isolation tests).
