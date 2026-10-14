using System.Text.Json;
using StreamPanel.Server.Windows;

namespace StreamPanel.Server.Tests;

public class GameTelemetryServiceTests
{
    /// <summary>A trimmed copy of what Counter-Strike 2 really sends: a T-side player holding the AK, knife in slot 0.</summary>
    private const string TerroristPayload = """
        {
          "provider": { "name": "Counter-Strike 2", "appid": 730, "version": 14000, "steamid": "7656", "timestamp": 1700000000 },
          "map": {
            "mode": "competitive", "name": "de_mirage", "phase": "live", "round": 7,
            "team_ct": { "score": 4 }, "team_t": { "score": 2 }
          },
          "round": { "phase": "live", "bomb": "planted" },
          "player": {
            "steamid": "7656", "name": "zibirik", "team": "T", "activity": "playing",
            "state": { "health": 87, "armor": 100, "helmet": true, "money": 4150 },
            "match_stats": { "kills": 5, "assists": 1, "deaths": 2, "mvps": 1, "score": 13 },
            "weapons": {
              "weapon_0": { "name": "weapon_knife", "type": "Knife", "state": "holstered" },
              "weapon_1": { "name": "weapon_glock", "type": "Pistol", "ammo_clip": 20, "ammo_clip_max": 20, "ammo_reserve": 120, "state": "holstered" },
              "weapon_2": { "name": "weapon_ak47", "type": "Rifle", "ammo_clip": 17, "ammo_clip_max": 30, "ammo_reserve": 90, "state": "active" }
            }
          }
        }
        """;

    [Fact]
    public void Terrorist_ScoreIsReadFromThePlayersOwnSide()
    {
        var service = new GameTelemetryService();

        service.UpdateFromCounterStrike(Parse(TerroristPayload));
        var status = service.LastStatus;

        // T has 2 rounds and CT has 4: "our" score is 2, the enemy's is 4. It used to say the opposite.
        Assert.Equal(2, status.TeamScore);
        Assert.Equal(4, status.EnemyScore);
        Assert.Equal("T", status.Team);
    }

    [Fact]
    public void Ammo_ComesFromTheWeaponInHand_NotFromSlotZeroOrOne()
    {
        var service = new GameTelemetryService();

        service.UpdateFromCounterStrike(Parse(TerroristPayload));
        var status = service.LastStatus;

        Assert.Equal("AK47", status.Weapon);
        Assert.Equal(17, status.AmmoClip);
        Assert.Equal(90, status.AmmoReserve);
    }

    [Fact]
    public void ReadsTheRestOfTheHud()
    {
        var service = new GameTelemetryService();

        service.UpdateFromCounterStrike(Parse(TerroristPayload));
        var status = service.LastStatus;

        Assert.True(status.Detected);
        Assert.Equal("Counter-Strike 2", status.Provider);
        Assert.Equal("de_mirage", status.MapName);
        Assert.Equal("live", status.Phase);
        Assert.Equal("zibirik", status.PlayerName);
        Assert.Equal(87, status.Health);
        Assert.Equal(100, status.Armor);
        Assert.Equal(4150, status.Money);
        Assert.Equal(5, status.Kills);
        Assert.Equal(2, status.Deaths);
        Assert.Equal(1, status.Assists);
        Assert.Equal(1, status.Mvps);
        Assert.Equal(7, status.Round);
        Assert.Equal("planted", status.Bomb);
    }

    [Fact]
    public void CounterTerrorist_ScoreUsesCtSide()
    {
        var service = new GameTelemetryService();
        var payload = TerroristPayload.Replace("\"team\": \"T\"", "\"team\": \"CT\"");

        service.UpdateFromCounterStrike(Parse(payload));

        Assert.Equal(4, service.LastStatus.TeamScore);
        Assert.Equal(2, service.LastStatus.EnemyScore);
    }

    [Fact]
    public void MenuHeartbeat_WithoutPlayerData_DoesNotCrashAndSaysMenu()
    {
        var service = new GameTelemetryService();

        service.UpdateFromCounterStrike(Parse("""{ "provider": { "name": "Counter-Strike 2" }, "player": { "activity": "menu" } }"""));
        var status = service.LastStatus;

        Assert.True(status.Detected);
        Assert.Null(status.Health);
        Assert.Null(status.AmmoClip);
        Assert.Equal("menu", status.Activity);
    }

    [Fact]
    public void EmptyObject_IsStillAcceptedAsAHeartbeat()
    {
        var service = new GameTelemetryService();

        service.UpdateFromCounterStrike(Parse("{}"));

        Assert.Equal(1, service.ReceivedPayloads);
        Assert.True(service.LastStatus.Detected);
    }

    [Fact]
    public void IsLive_OnlyWhileHeartbeatsKeepComing()
    {
        var clock = new ManualClock();
        var service = new GameTelemetryService(clock);
        Assert.False(service.IsCounterStrikeLive);

        service.UpdateFromCounterStrike(Parse(TerroristPayload));
        Assert.True(service.IsCounterStrikeLive);

        clock.Advance(GameTelemetryService.LiveWindow - TimeSpan.FromSeconds(1));
        Assert.True(service.IsCounterStrikeLive);

        clock.Advance(TimeSpan.FromSeconds(2));
        Assert.False(service.IsCounterStrikeLive);
    }

    [Fact]
    public void GetStatus_FallsBackToWindowDetection_WhenDataIsStale()
    {
        var clock = new ManualClock();
        var service = new GameTelemetryService(clock);
        service.UpdateFromCounterStrike(Parse(TerroristPayload));
        clock.Advance(TimeSpan.FromMinutes(5));

        var status = service.GetStatus("cs2", "Counter-Strike 2");

        Assert.True(status.Detected);
        Assert.Null(status.Health);
        Assert.Contains("integration", status.Note, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public void GetStatus_ForAnUnrelatedWindow_IsNotDetected()
    {
        var status = new GameTelemetryService().GetStatus("notepad", "Untitled - Notepad");

        Assert.False(status.Detected);
    }

    [Theory]
    [InlineData("weapon_ak47", "AK47")]
    [InlineData("weapon_usp_silencer", "USP Silencer")]
    [InlineData("weapon_awp", "AWP")]
    [InlineData("weapon_m4a1", "M4A1")]
    [InlineData("weapon_knife", "Knife")]
    [InlineData("", null)]
    [InlineData(null, null)]
    public void FriendlyWeaponName_IsReadable(string? raw, string? expected) =>
        Assert.Equal(expected, GameTelemetryService.FriendlyWeaponName(raw));

    private static JsonElement Parse(string json) => JsonDocument.Parse(json).RootElement.Clone();

    private sealed class ManualClock : TimeProvider
    {
        private DateTimeOffset _now = new(2026, 1, 1, 12, 0, 0, TimeSpan.Zero);

        public override DateTimeOffset GetUtcNow() => _now;

        public void Advance(TimeSpan by) => _now += by;
    }
}
