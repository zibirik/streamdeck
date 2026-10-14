# Changelog

All notable changes are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/),
and versions follow [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Changed
- Settings redesigned from scratch: a home screen of big colorful tiles, one simple page per topic,
  live connection status, "Find my PC" auto-discovery, theme previews, color swatches instead of hex codes,
  grid preview, one-tap backup/restore and an About page. Settings texts are fully localized (RU/EN).
- Text fields now follow the theme colors (they were always white, unreadable on the Light theme).

### Fixed
- Choosing a grid preset (e.g. 4×4) could keep the old row count because rows and columns were saved separately.

### Security
- PIN authentication is now enforced per WebSocket connection and for the HTTP API
  (`/status`, `/api/configurator/*`, `/integrations/*`); wrong PINs are rate-limited per IP.
- `OPEN_URL` / `OPEN_URLS` accept only allow-listed URL schemes (no more local paths or `file:` URLs).
- WebSocket messages are reassembled and size-limited; HTTP request bodies are limited to 1 MiB.
- Tokens and the PIN stored in Android DataStore are excluded from backups.

### Added
- Public `GET /api` endpoint (name, machine name, `pinRequired`) used for LAN discovery.
- Server unit tests, CI workflow, and a release workflow that publishes the APK and the Windows server.
- `LICENSE` (PolyForm Noncommercial 1.0.0) and `COMMERCIAL.md`.

### Changed
- Wrong PIN is now reported as a connection error on the tablet.
- Local server runs on newer .NET runtimes (roll-forward); release builds are self-contained.
- The deck database is no longer wiped on schema upgrades (only on downgrade).

### Fixed
- Invalid numeric payloads and unknown system commands return a clean error instead of failing.

## [0.1.15]

- Fix OBS stream/record sync, responsive studio monitors.
