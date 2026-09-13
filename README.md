# RediSet

> A Redis-inspired in-memory database written in Java 21.

RediSet is a from-scratch, educational-yet-serious in-memory key-value database
inspired by the design ideas behind Redis. It is **not** a Redis clone, contains
no Redis source code, and does not aim for wire compatibility with Redis. It
implements its own original wire protocol, command set, storage engine, and
persistence layer.

**Status:** work in progress. This README stub is expanded incrementally as the
project is built (see `CHANGELOG.md` and `docs/`).

## Quick facts

| | |
|---|---|
| Language | Java 21 (LTS) |
| Build | Maven |
| Default port | `6412` |
| License | Apache-2.0 |
| Base package | `io.rediset` |

## Building

```bash
mvn package
```

More documentation lives under [`docs/`](docs/). See
[`CONTRIBUTING.md`](CONTRIBUTING.md) to get involved.
