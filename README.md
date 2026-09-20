# RediSet

> A Redis-inspired in-memory database written in Java 21.

RediSet is a from-scratch, educational-yet-serious in-memory key-value database
inspired by the design ideas behind Redis. It is **not** a Redis clone, contains
no Redis source code, and does not aim for wire compatibility with Redis. It
implements its own original wire protocol (RSP), command set, storage engine, and
(in later milestones) persistence, transactions, pub/sub, and replication.

**Status:** MVP. The core server, protocol, command engine, and key/value store
are implemented and tested. See `CHANGELOG.md` for the running list of features
and the roadmap near the bottom of this file for what is planned.

## Quick facts

| | |
|---|---|
| Language | Java 21 (LTS) |
| Build | Maven |
| Default port | `6412` |
| Base package | `io.rediset` |
| License | Apache-2.0 |

## Architecture

```
Client
  │  TCP
  ▼
RedisetServer (acceptor + virtual-thread-per-connection)
  │
  ▼
ProtocolDecoder ──► CommandParser ──► CommandRegistry ──► CommandExecutor
                                                              │
                                                              ▼
                                                        StorageEngine
  ▲                                                           │
  └──────────────── ResponseWriter ◄── ProtocolEncoder ◄──────┘
```

The dependency direction is strictly one-way:
`server → protocol → command → storage`. See
[`docs/architecture/overview.md`](docs/architecture/overview.md) and the
[ADRs](docs/architecture/adr/).

## Building

```bash
mvn package
```

This produces a runnable fat jar at `target/rediset-server.jar`.

## Running

```bash
java -jar target/rediset-server.jar
# RediSet listens on 0.0.0.0:6412 by default
```

Configuration can be overridden via environment variables, for example:

```bash
REDISET_SERVER_PORT=7000 java -jar target/rediset-server.jar
```

## Talking to RediSet

RediSet speaks its own protocol, **RSP**, documented in
[`docs/protocol/protocol.md`](docs/protocol/protocol.md). Commands can be sent in
the array-of-bulk-strings form or the convenience inline form. For quick manual
testing you can use any raw TCP tool. Example with a small Python client:

```python
import socket
s = socket.create_connection(("127.0.0.1", 6412))
s.sendall(b"SET name RediSet\r\n")   # inline form
print(s.recv(100))                    # b'+OK\r\n'
s.sendall(b"GET name\r\n")
print(s.recv(100))                    # b'$7\r\nRediSet\r\n'
```

## Supported commands (current)

| Command | Description |
|---------|-------------|
| `PING [message]` | Liveness check; replies `PONG` or echoes `message`. |
| `ECHO message` | Returns `message`. |
| `SET key value` | Stores a string value. |
| `GET key` | Returns a string value, or null. |
| `DEL key [key ...]` | Deletes keys; returns the count removed. |
| `EXISTS key [key ...]` | Returns how many of the keys exist. |
| `KEYS pattern` | Returns keys matching a glob pattern. |
| `DBSIZE` | Returns the number of keys. |
| `TYPE key` | Returns a key's value type, or `none`. |
| `EXPIRE` / `PEXPIRE` / `TTL` / `PTTL` / `PERSIST` | Key time-to-live management. |
| `LPUSH` / `RPUSH` / `LPOP` / `RPOP` / `LRANGE` / `LLEN` / `LINDEX` | List operations. |
| `SADD` / `SREM` / `SMEMBERS` / `SISMEMBER` / `SCARD` | Set operations. |
| `HSET` / `HGET` / `HDEL` / `HGETALL` / `HKEYS` / `HVALS` / `HLEN` | Hash operations. |
| `ZADD` / `ZSCORE` / `ZRANGE` / `ZREM` / `ZCARD` | Sorted-set operations. |

The full, always-current reference lives in
[`docs/commands/command-reference.md`](docs/commands/command-reference.md).

## Testing

```bash
mvn test
```

The suite includes unit tests (protocol, storage, config, utilities), end-to-end
integration tests over real sockets, and (in later steps) concurrency and
persistence tests.

## Project status

- ✅ **Stable:** networking, RSP protocol, command engine, key/value store.
- 🚧 **In progress / planned:** TTL/expiration, richer data types, persistence,
  transactions, pub/sub, eviction, replication, metrics/INFO, CLI, Docker, CI,
  and benchmarks.

## Roadmap

See the build plan reflected in `CHANGELOG.md`. Upcoming milestones: expiration,
List/Set/Hash/SortedSet data types, snapshot + append-only persistence,
transactions, pub/sub, eviction, replication, observability, a CLI, DevOps
tooling, and benchmarks.

## Limitations (honest)

- No authentication, TLS, or ACLs yet — run only inside a trusted network.
- Not yet durable across restarts (persistence arrives in a later milestone).
- Not a Redis clone and not wire-compatible with Redis.

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md) and the
[development guide](docs/contributing/development.md).

## License

Apache-2.0. See [`LICENSE`](LICENSE) and [`NOTICE`](NOTICE).
