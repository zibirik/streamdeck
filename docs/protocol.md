# Command Protocol

Android and the Windows companion server communicate over WebSocket at `/ws`.

All messages are JSON and include a protocol version.

## Command

```json
{
  "id": "a8a22942-2fe4-46d2-b1dd-5eb0467e46cb",
  "protocolVersion": 1,
  "type": "OPEN_URL",
  "payload": {
    "url": "https://example.com"
  },
  "createdAtEpochMs": 1760000000000
}
```

## Response

```json
{
  "id": "a8a22942-2fe4-46d2-b1dd-5eb0467e46cb",
  "ok": true,
  "message": "Opened URL",
  "completedAtEpochMs": 1760000001000
}
```

## Command Types

- `OPEN_URL`: opens `payload.url`. Only schemes listed in `StreamPanel:AllowedUrlSchemes` are accepted
  (default: `http`, `https`, `mailto`, `steam`, `spotify`, `discord`, `ms-settings`, `tg`, `slack`, `zoommtg`).
  Paths and `file:` URLs are rejected.
- `OPEN_URLS`: opens several URLs from `payload.urls` separated by `|`, `;` or new lines. All entries are
  validated before any is opened.
- `OPEN_FOLDER`: opens `payload.path` in Explorer.
- `LAUNCH_PROCESS`: launches a process, file or folder path (`payload.path`, optional `runAsAdmin=true`).
- `SEND_TEXT`: types Unicode text into the active foreground application.
- `HOTKEY`: sends a hotkey sequence such as `CTRL+SHIFT+M`.
- `MOUSE_COMMAND`: `move`, `left_click`, `right_click` or `scroll` (`x`, `y`, `delta` are integers).
- `MEDIA_COMMAND`: sends a media key (`payload.action`).
- `VOLUME_COMMAND`: `get`, `set`, `up`, `down`, `mute`, `mic_mute`, `switch_output`, ...
- `WINDOW_COMMAND`: `minimize_all`, `snap_left`, `move_monitor`, ...
- `SYSTEM_COMMAND`: allowlisted system actions (`StreamPanel:AllowedSystemCommands`).
- `CUSTOM`: extension point. Built in: `{"action":"ping"}` and `{"action":"auth","pin":"..."}`.

Numeric payload values are strings; a value that is not an integer produces a normal failure response
(`ok: false`) instead of a server error.

## Authentication

If `StreamPanel:Pin` is set on the server, every remote client must authenticate **per WebSocket connection**
before any command except `ping` is accepted:

```json
{ "id": "…", "protocolVersion": 1, "type": "CUSTOM", "payload": { "action": "auth", "pin": "1234" }, "createdAtEpochMs": 0 }
```

- Until `auth` succeeds, other commands return `ok: false` with `Authentication required…`.
- After 5 wrong PINs from the same IP address the client is locked out for 60 seconds.
- Requests coming from the PC itself (loopback or the PC's own address) never need a PIN.
- With an empty PIN the server is open to everyone on the network; the console prints a warning.

HTTP endpoints `/status`, `/api/configurator/*` and `/integrations/*` use the same PIN, sent as the
`X-StreamPanel-Pin` header or the `pin` query parameter (`401` = missing/wrong, `429` = locked out).
`GET /api` is public and returns `name`, `machineName` and `pinRequired`, which is what LAN discovery uses.

`POST /gsi` and `POST /integrations/cs2/gsi` are open: Counter-Strike 2 posts game-state JSON and cannot send
headers. Those paths only accept HUD data; they cannot run commands. One-tap install is `POST /integrations/cs2/install`
(PIN required from the tablet).

## Limits

- A WebSocket message may be at most `StreamPanel:MaxMessageBytes` (default 1 MiB); larger messages close the connection.
- HTTP request bodies are limited to 1 MiB.

## Android-Side Action Payloads

- `ObsCommand`: `url`, optional `password`, `command`, plus command-specific data such as `sceneName`.
- `SpotifyCommand`: `token`, `command`, optional `deviceId`, `body`, `volumePercent`.
- `DiscordWebhook`: `webhookUrl`, `content`, optional `username`.
- `HueCommand`: `bridgeUrl`, `appKey`, `resource`, `resourceId`, optional JSON `body`.
- `HomeAssistant`: `baseUrl`, `token`, `domain`, `service`, optional JSON `body`.
- `Mqtt`: `host`, optional `port`, optional `clientId`, `topic`, `message`.
- `TcpPacket` and `UdpPacket`: `host`, `port`, `message`.

## Versioning

The server rejects commands with unsupported `protocolVersion` values. New command types should be additive and keep existing payload keys stable.
