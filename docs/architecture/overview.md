# Architecture Overview

RediSet is organized as a pipeline from the network socket down to the storage
engine, with several supporting subsystems hanging off the command execution
path.

```
Client
  │  TCP
  ▼
TCP Server (acceptor + per-connection virtual thread)
  │
  ▼
Protocol Decoder ── Protocol Encoder
  │
  ▼
Command Parser ─► Command Registry ─► Command Executor
                                          │
                                          ├─► Storage Engine
                                          │       ├─ Expiration Manager
                                          │       ├─ Eviction Policy
                                          │       └─ Data Types (String/List/Set/Hash/ZSet)
                                          ├─► Persistence (Snapshot + AOF)
                                          ├─► Pub/Sub Bus
                                          ├─► Transaction Context
                                          └─► Replication stream (Primary → Replica)
  │
  ▼
Metrics / INFO
```

## Package layout and dependency direction

The dependency direction is strictly one-way:

```
server → protocol → command → storage
```

- **`server`** — TCP acceptor, connection lifecycle, session objects.
- **`protocol`** — wire format encode/decode, command parsing, response writing.
- **`command`** — the command abstraction, registry, and concrete commands.
- **`storage`** — the in-memory key space and value types.
- Supporting packages (`expiration`, `persistence`, `replication`,
  `transaction`, `pubsub`, `eviction`, `metrics`, `config`, `logging`,
  `exception`, `util`) are used by the layers above without introducing cycles.

Each subsystem is documented in its own file in this directory. Design decisions
that involved a genuine trade-off are captured as
[ADRs](adr/).
