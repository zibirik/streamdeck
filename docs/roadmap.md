# Implementation Roadmap

## Stage 1: MVP

Implemented in this repository:

- Android tablet dashboard, editor, settings and PC connection screens.
- Room/DataStore persistence.
- WebSocket and HTTP action execution.
- Windows companion server with URL, process, text and hotkey commands.
- Plugin API contracts.

## Stage 2: Visual Editor

- Drag and drop button reorder.
- Image and GIF metadata fields.
- Button resizing metadata and per-button animation presets.
- Icon pack management.

## Stage 3: OBS

- obs-websocket authentication.
- Scene switching and stream/record controls.
- Source visibility and volume command support.
- Real-time status subscriptions.

## Stage 4: Macro Engine

- Conditions, variables and loops.
- Timers.
- Trigger registry.
- Execution logs and cancellation.

## Stage 5: Integrations

- Spotify, Discord and Streamlabs action clients.
- Philips Hue, MQTT and Home Assistant action clients.
- Windows system dashboard and telemetry.

## Stage 6: Plugin Runtime

- Signed manifests.
- Plugin discovery.
- SDK samples and compatibility checks.

## Stage 7: Release

- Startup profiling and 60 FPS tuning.
- Background connection service.
- Battery optimization pass.
- Google Play packaging and privacy docs.
