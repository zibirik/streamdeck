using System.Text.Json;

namespace StreamPanel.Server.Windows;

public interface IGameTelemetryService
{
    void UpdateFromCounterStrike(JsonElement payload);
    GameTelemetryStatus GetStatus(string? foregroundProcess, string? foregroundTitle);
    GameTelemetryStatus LastStatus { get; }
    DateTimeOffset? LastUpdateUtc { get; }
    int ReceivedPayloads { get; }
}

public sealed class GameTelemetryService : IGameTelemetryService
{
    private readonly object _gate = new();
    private GameTelemetryStatus? _lastCounterStrikeStatus;
    private DateTimeOffset _lastCounterStrikeUpdate = DateTimeOffset.MinValue;
    private int _receivedPayloads;

    public GameTelemetryStatus LastStatus
    {
        get
        {
            lock (_gate)
            {
                return _lastCounterStrikeStatus ?? EmptyStatus(null, null);
            }
        }
    }

    public DateTimeOffset? LastUpdateUtc
    {
        get
        {
            lock (_gate)
            {
                return _lastCounterStrikeStatus is null ? null : _lastCounterStrikeUpdate;
            }
        }
    }

    public int ReceivedPayloads
    {
        get
        {
            lock (_gate)
            {
                return _receivedPayloads;
            }
        }
    }

    public void UpdateFromCounterStrike(JsonElement payload)
    {
        var status = new GameTelemetryStatus(
            Detected: true,
            Provider: ReadString(payload, "provider", "name") ?? "Counter-Strike",
            ProcessName: "cs2.exe",
            WindowTitle: null,
            MapName: ReadString(payload, "map", "name"),
            Phase: ReadString(payload, "map", "phase") ?? ReadString(payload, "round", "phase"),
            PlayerName: ReadString(payload, "player", "name"),
            Team: ReadString(payload, "player", "team"),
            Health: ReadInt(payload, "player", "state", "health"),
            Armor: ReadInt(payload, "player", "state", "armor"),
            AmmoClip: ReadInt(payload, "player", "weapons", "weapon_0", "ammo_clip")
                ?? ReadInt(payload, "player", "weapons", "weapon_1", "ammo_clip"),
            AmmoReserve: ReadInt(payload, "player", "weapons", "weapon_0", "ammo_reserve")
                ?? ReadInt(payload, "player", "weapons", "weapon_1", "ammo_reserve"),
            TeamScore: ReadInt(payload, "map", "team_ct", "score"),
            EnemyScore: ReadInt(payload, "map", "team_t", "score"),
            PingMs: null,
            Note: "CS2 Game State Integration"
        );
        status = status with { AmmoClip = status.AmmoClip ?? ReadActiveWeaponInt(payload, "ammo_clip") };
        status = status with { AmmoReserve = status.AmmoReserve ?? ReadActiveWeaponInt(payload, "ammo_reserve") };

        lock (_gate)
        {
            _lastCounterStrikeStatus = status;
            _lastCounterStrikeUpdate = DateTimeOffset.UtcNow;
            _receivedPayloads++;
        }
    }

    public GameTelemetryStatus GetStatus(string? foregroundProcess, string? foregroundTitle)
    {
        var looksLikeCounterStrike = ContainsAny(foregroundProcess, "cs2", "csgo", "counter-strike")
            || ContainsAny(foregroundTitle, "counter-strike", "cs2");

        lock (_gate)
        {
            if (_lastCounterStrikeStatus is not null &&
                DateTimeOffset.UtcNow - _lastCounterStrikeUpdate < TimeSpan.FromSeconds(30))
            {
                return _lastCounterStrikeStatus with
                {
                    ProcessName = foregroundProcess ?? _lastCounterStrikeStatus.ProcessName,
                    WindowTitle = foregroundTitle
                };
            }
        }

        if (looksLikeCounterStrike)
        {
            return new GameTelemetryStatus(
                Detected: true,
                Provider: "Counter-Strike",
                ProcessName: foregroundProcess,
                WindowTitle: foregroundTitle,
                MapName: null,
                Phase: null,
                PlayerName: null,
                Team: null,
                Health: null,
                Armor: null,
                AmmoClip: null,
                AmmoReserve: null,
                TeamScore: null,
                EnemyScore: null,
                PingMs: null,
                Note: "Game detected. Add CS2 Game State Integration config for health, ammo, map and score."
            );
        }

        return EmptyStatus(foregroundProcess, foregroundTitle);
    }

    private static GameTelemetryStatus EmptyStatus(string? foregroundProcess, string? foregroundTitle)
    {
        return new GameTelemetryStatus(
            Detected: false,
            Provider: null,
            ProcessName: foregroundProcess,
            WindowTitle: foregroundTitle,
            MapName: null,
            Phase: null,
            PlayerName: null,
            Team: null,
            Health: null,
            Armor: null,
            AmmoClip: null,
            AmmoReserve: null,
            TeamScore: null,
            EnemyScore: null,
            PingMs: null,
            Note: null
        );
    }

    private static bool ContainsAny(string? value, params string[] needles)
    {
        if (string.IsNullOrWhiteSpace(value)) return false;
        return needles.Any(needle => value.Contains(needle, StringComparison.OrdinalIgnoreCase));
    }

    private static string? ReadString(JsonElement root, params string[] path)
    {
        if (!TryGet(root, out var value, path)) return null;
        return value.ValueKind == JsonValueKind.String ? value.GetString() : value.ToString();
    }

    private static int? ReadInt(JsonElement root, params string[] path)
    {
        if (!TryGet(root, out var value, path)) return null;
        if (value.ValueKind == JsonValueKind.Number && value.TryGetInt32(out var number)) return number;
        return int.TryParse(value.ToString(), out var parsed) ? parsed : null;
    }

    private static int? ReadActiveWeaponInt(JsonElement root, string propertyName)
    {
        if (!TryGet(root, out var weapons, "player", "weapons") || weapons.ValueKind != JsonValueKind.Object)
        {
            return null;
        }

        foreach (var weapon in weapons.EnumerateObject().Select(property => property.Value))
        {
            if (ReadString(weapon, "state") == "active")
            {
                return ReadInt(weapon, propertyName);
            }
        }

        return null;
    }

    private static bool TryGet(JsonElement root, out JsonElement value, params string[] path)
    {
        value = root;
        foreach (var part in path)
        {
            if (value.ValueKind != JsonValueKind.Object || !value.TryGetProperty(part, out value))
            {
                return false;
            }
        }
        return true;
    }
}

public sealed record GameTelemetryStatus(
    bool Detected,
    string? Provider,
    string? ProcessName,
    string? WindowTitle,
    string? MapName,
    string? Phase,
    string? PlayerName,
    string? Team,
    int? Health,
    int? Armor,
    int? AmmoClip,
    int? AmmoReserve,
    int? TeamScore,
    int? EnemyScore,
    int? PingMs,
    string? Note
);
