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

## MVP Command Types

- `OPEN_URL`: opens a URL with the system default browser.
- `LAUNCH_PROCESS`: launches a process, file or folder path.
- `SEND_TEXT`: types Unicode text into the active foreground application.
- `HOTKEY`: sends a hotkey sequence such as `CTRL+SHIFT+M`.
- `MOUSE_COMMAND`: sends mouse movement, click or scroll commands.
- `SYSTEM_COMMAND`: reserved for allowlisted system actions.
- `CUSTOM`: extension point for plugins and future integrations.

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
