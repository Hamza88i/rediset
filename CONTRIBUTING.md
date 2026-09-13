# Contributing to RediSet

Thanks for your interest in contributing! RediSet is built to be readable and
approachable, so contributions of all sizes are welcome.

## Development setup

- JDK 21 (LTS)
- Maven 3.9+

```bash
git clone https://github.com/rediset/rediset.git
cd rediset
mvn test
```

See [`docs/contributing/development.md`](docs/contributing/development.md) for a
detailed development guide.

## Coding guidelines

- Prefer simple, readable Java over clever Java.
- No giant classes or methods. Keep each class focused on one responsibility.
- No magic numbers, no hardcoded secrets or paths.
- Every feature must be implemented **and** tested before it is considered done.
- Keep the full test suite green: never break a previously working feature.
- Respect the one-way dependency direction:
  `server → protocol → command → storage`. No circular package dependencies.

## Commit messages

We use [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(scope): add a new capability
fix(scope): fix a bug
test(scope): add or improve tests
docs: documentation only changes
perf: performance improvement
ci: CI / build tooling changes
chore: repository maintenance
```

One logical change per commit.

## Architecture Decision Records

Significant design trade-offs are recorded as ADRs under
[`docs/architecture/adr/`](docs/architecture/adr/). If you make a non-obvious
design decision, add an ADR describing the context, options, decision, and
consequences.

## Pull requests

Please use the pull request template. Ensure `mvn test` and `mvn package` pass
before opening a PR.
