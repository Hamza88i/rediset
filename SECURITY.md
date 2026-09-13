# Security Policy

## Supported Versions

RediSet is currently pre-1.0 and under active development. Only the latest
release line receives security fixes.

| Version | Supported |
|---------|-----------|
| 0.1.x   | ✅        |

## Reporting a Vulnerability

If you discover a security vulnerability, please **do not** open a public issue.
Instead, report it privately through GitHub's
[private vulnerability reporting](https://docs.github.com/en/code-security/security-advisories)
feature, or contact the maintainers directly.

Please include:

- A description of the vulnerability and its impact
- Steps to reproduce
- Any relevant logs or proof-of-concept code

We will acknowledge your report and work with you on a coordinated disclosure.

## Security Considerations

RediSet, like the systems that inspired it, is designed to run inside a trusted
network. It currently does **not** implement authentication, TLS, or ACLs. Do
**not** expose a RediSet instance directly to an untrusted network without an
external security layer (firewall, VPN, or reverse proxy). See the limitations
section of the README for the current, honest security posture.
