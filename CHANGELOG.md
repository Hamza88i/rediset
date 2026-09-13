# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Initial repository skeleton: Maven build, licensing, contribution guides, and
  the first Architecture Decision Records (ADR-001 Java version, ADR-002
  networking model).
- Configuration layer (`ServerConfig`) with `application.properties` defaults and
  `REDISET_`-prefixed environment-variable overrides.
- Networking layer: `RedisetServer` TCP acceptor with virtual-thread-per-client
  servicing, connection accounting with a max-clients limit, per-connection fault
  isolation, idle-timeout support, and graceful, idempotent shutdown.
- `ClientSession` per-connection state and the `ConnectionHandler` strategy
  interface that keeps the server layer decoupled from the protocol/command
  layers.
