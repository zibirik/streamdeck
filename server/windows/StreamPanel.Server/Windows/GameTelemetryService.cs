using System.Text.Json;

namespace StreamPanel.Server.Windows;

public interface IGameTelemetryService
{
    void UpdateFromCounterStrike(JsonElement payload);
    GameTelemetryStatus GetStatus(string? foregroundProcess, string? foregroundTitle);
    GameTelemetryStatus LastStatus { get; }
    DateTimeOffset? LastUpdateUtc { get; }
    int ReceivedPayloads { get; }

    /// <summary>True while Counter-Strike keeps sending data (it sends a heartbeat every few seconds).</summary>
    bool IsCounterStrikeLive { get; }
}

/// <summary>
/// Turns what games tell us into one simple <see cref="GameTelemetryStatus"/> for the tablet.
/// Counter-Strike 2 talks to us through Game State Integration (it POSTs JSON); other games are only detected by window.
/// </summary>
public sealed class GameTelemetryService(TimeProvider? clock = null) : IGameTelemetryService
{
    /// <summary>CS2 is asked for a heartbeat every 5 seconds; a few missed beats means the game is gone.</summary>
    public static readonly TimeSpan LiveWindow = TimeSpan.FromSeconds(30);

    private readonly TimeProvider _clock = clock ?? TimeProvider.System;
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

    public bool IsCounterStrikeLive
    {
        get
        {
            lock (_gate)
            {
                return _lastCounterStrikeStatus is not null && _clock.GetUtcNow() - _lastCounterStrikeUpdate < LiveWindow;
            }
        }
    }

    public void UpdateFromCounterStrike(JsonElement payload)
    {
        var team = ReadString(payload, "player", "team");
        var ctScore = ReadInt(payload, "map", "team_ct", "score");
        var tScore = ReadInt(payload, "map", "team_t", "score");

        // "Our" score is the score of the observed player's side, not always CT.
        var (teamScore, enemyScore) = string.Equals(team, "T", StringComparison.OrdinalIgnoreCase)
            ? (tScore, ctScore)
            : (ctScore, tScore);

        var weapon = ReadActiveWeapon(payload);
        var activity = ReadString(payload, "player", "activity");

        var status = new GameTelemetryStatus(
            Detected: true,
            Provider: ReadString(payload, "provider", "name") ?? "Counter-Strike",
            ProcessName: "cs2.exe",
            WindowTitle: null,
            MapName: ReadString(payload, "map", "name"),
            Phase: ReadString(payload, "round", "phase") ?? ReadString(payload, "map", "phase"),
            PlayerName: ReadString(payload, "player", "name"),
            Team: team,
            Health: ReadInt(payload, "player", "state", "health"),
            Armor: ReadInt(payload, "player", "state", "armor"),
            AmmoClip: weapon?.AmmoClip,
            AmmoReserve: weapon?.AmmoReserve,
            TeamScore: teamScore,
            EnemyScore: enemyScore,
            PingMs: null,
            Note: string.Equals(activity, "menu", StringComparison.OrdinalIgnoreCase)
                ? "Counter-Strike is in the menu"
                : "CS2 Game State Integration",
            Weapon: weapon?.Name,
            Money: ReadInt(payload, "player", "state", "money"),
            Kills: ReadInt(payload, "player", "match_stats", "kills"),
            Deaths: ReadInt(payload, "player", "match_stats", "deaths"),
            Assists: ReadInt(payload, "player", "match_stats", "assists"),
            Mvps: ReadInt(payload, "player", "match_stats", "mvps"),
            Round: ReadInt(payload, "map", "round"),
            Bomb: ReadString(payload, "round", "bomb"),
            Activity: activity);

        lock (_gate)
        {
            _lastCounterStrikeStatus = status;
            _lastCounterStrikeUpdate = _clock.GetUtcNow();
            _receivedPayloads++;
        }
    }

    public GameTelemetryStatus GetStatus(string? foregroundProcess, string? foregroundTitle)
    {
        var looksLikeCounterStrike = ContainsAny(foregroundProcess, "cs2", "csgo", "counter-strike")
            || ContainsAny(foregroundTitle, "counter-strike", "cs2");

        lock (_gate)
        {
            if (_lastCounterStrikeStatus is not null && _clock.GetUtcNow() - _lastCounterStrikeUpdate < LiveWindow)
            {
                return _lastCounterStrikeStatus with
                {
                    ProcessName = looksLikeCounterStrike ? foregroundProcess ?? "cs2.exe" : "cs2.exe",
                    WindowTitle = looksLikeCounterStrike ? foregroundTitle : null,
                };
            }
        }

        if (looksLikeCounterStrike)
        {
            return EmptyStatus(foregroundProcess, foregroundTitle) with
            {
                Detected = true,
                Provider = "Counter-Strike",
                Note = "Game detected. Waiting for live data — restart CS2 once after installing the integration.",
            };
        }

        return EmptyStatus(foregroundProcess, foregroundTitle);
    }

    private static GameTelemetryStatus EmptyStatus(string? foregroundProcess, string? foregroundTitle) =>
        new(
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
            Note: null);

    private static bool ContainsAny(string? value, params string[] needles)
    {
        if (string.IsNullOrWhiteSpace(value)) return false;
        return needles.Any(needle => value.Contains(needle, StringComparison.OrdinalIgnoreCase));
    }

    private static string? ReadString(JsonElement root, params string[] path)
    {
        if (!TryGet(root, out var value, path)) return null;
        return value.ValueKind switch
        {
            JsonValueKind.String => value.GetString(),
            JsonValueKind.Null or JsonValueKind.Undefined => null,
            _ => value.ToString(),
        };
    }

    private static int? ReadInt(JsonElement root, params string[] path)
    {
        if (!TryGet(root, out var value, path)) return null;
        if (value.ValueKind == JsonValueKind.Number && value.TryGetInt32(out var number)) return number;
        return int.TryParse(value.ToString(), out var parsed) ? parsed : null;
    }

    private sealed record ActiveWeapon(string? Name, int? AmmoClip, int? AmmoReserve);

    /// <summary>The weapon in the player's hands. Slot numbers are not stable (the knife is often weapon_0), so look at "state".</summary>
    private static ActiveWeapon? ReadActiveWeapon(JsonElement root)
    {
        if (!TryGet(root, out var weapons, "player", "weapons") || weapons.ValueKind != JsonValueKind.Object)
        {
            return null;
        }

        foreach (var weapon in weapons.EnumerateObject().Select(property => property.Value))
        {
            if (!string.Equals(ReadString(weapon, "state"), "active", StringComparison.OrdinalIgnoreCase))
            {
                continue;
            }

            return new ActiveWeapon(
                FriendlyWeaponName(ReadString(weapon, "name")),
                ReadInt(weapon, "ammo_clip"),
                ReadInt(weapon, "ammo_reserve"));
        }

        return null;
    }

    /// <summary>"weapon_ak47" becomes "AK47", "weapon_usp_silencer" becomes "USP Silencer".</summary>
    public static string? FriendlyWeaponName(string? raw)
    {
        if (string.IsNullOrWhiteSpace(raw)) return null;
        var name = raw.StartsWith("weapon_", StringComparison.OrdinalIgnoreCase) ? raw["weapon_".Length..] : raw;
        var words = name.Split('_', StringSplitOptions.RemoveEmptyEntries)
            .Select(word => word.Length <= 4 && word.Any(char.IsDigit) || word.Length <= 3
                ? word.ToUpperInvariant()
                : char.ToUpperInvariant(word[0]) + word[1..]);
        return string.Join(' ', words);
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
    string? Note,
    string? Weapon = null,
    int? Money = null,
    int? Kills = null,
    int? Deaths = null,
    int? Assists = null,
    int? Mvps = null,
    int? Round = null,
    string? Bomb = null,
    string? Activity = null);
