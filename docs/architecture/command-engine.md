# Command Engine

The command engine turns a decoded request into a reply. It is deliberately
table-driven: there is no giant `switch` or `if/else` chain anywhere in the
dispatch path.

## Components

| Class | Responsibility |
|-------|----------------|
| `Command` | Interface every command implements: `name()`, `arity()`, `validate()`, `execute()`. |
| `CommandContext` | Bundles the `ClientSession` with shared services (storage, pub/sub, etc. added in later steps). |
| `CommandRegistry` | Case-insensitive name → `Command` map backed by a `ConcurrentHashMap`. |
| `CommandRegistryFactory` | Builds a registry populated with all built-in commands. |
| `CommandExecutor` | Looks up, validates, and runs a command; converts failures into protocol error replies. |
| `RedisetConnectionHandler` | The `ConnectionHandler` that runs the decode → parse → execute → write loop. |

## Dispatch flow

```
bytes ─► ProtocolDecoder ─► CommandParser ─► ParsedCommand
                                                  │
                                                  ▼
                              CommandExecutor.execute(context, parsed)
                                                  │
                        registry.get(name) ──► Command.validate() ──► Command.execute()
                                                  │
                                                  ▼
                                               Reply ─► ResponseWriter ─► bytes
```

## Arity convention

`Command.arity()` follows a common convention for variadic commands:

- A **positive** value `n` requires exactly `n` tokens (command name included).
- A **negative** value `-n` requires **at least** `n` tokens.

The default `validate()` enforces this. Commands with richer rules (for example
`PING`, which accepts zero or one argument) override `validate()` but still
enforce an arity bound.

## Error handling

`CommandExecutor` is the single place that decides how failures become replies:

- An unknown command → `-ERR unknown command '...'`.
- A `CommandException` subclass (arity, wrong type, invalid argument) → an error
  reply built from its error code and message.
- An unexpected `RuntimeException` → logged at ERROR and returned as
  `-ERR internal error`, so one buggy command never crashes the connection.

## Stateless commands

Command instances are stateless singletons. All per-request state travels through
`CommandContext` and the `ParsedCommand` arguments, so one instance safely serves
many concurrent connections without synchronization.

## Built-in commands (this step)

| Command | Arity | Behavior |
|---------|-------|----------|
| `PING [message]` | 1–2 tokens | Replies `+PONG`, or echoes `message` as a bulk string. |
| `ECHO message` | 2 tokens | Replies with `message` as a bulk string. |

The storage-backed commands (`SET`, `GET`, …) are added in the next step.
