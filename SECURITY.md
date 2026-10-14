# Security Policy

StreamPanel's Windows server can run actions on your PC (launch programs, send keystrokes, manage windows),
so security reports are taken seriously.

## Supported versions

Only the latest [release](https://github.com/zibirik/streamdeck/releases/latest) receives security fixes.

## Reporting a vulnerability

**Please do not open a public issue for security problems.**

Use GitHub's private reporting instead:
<https://github.com/zibirik/streamdeck/security/advisories/new>

Include what you found, how to reproduce it, and the version. You will get an answer as soon as possible,
and a fix and credit (if you want it) in the release notes.

## Security model in short

- The server listens on your local network over plain HTTP/WebSocket. Use it on a network you trust.
- Set a **PIN** (`StreamPanel:Pin` in `appsettings.json`). Without it, anyone on the network can control the PC.
- With a PIN, every remote connection must authenticate; wrong PINs are rate-limited per IP.
- Requests coming from the PC itself never need the PIN.
- Do not expose port 17820 to the internet (no port forwarding). Use a VPN if you need remote access.

See [`docs/protocol.md`](docs/protocol.md#authentication) for details.
