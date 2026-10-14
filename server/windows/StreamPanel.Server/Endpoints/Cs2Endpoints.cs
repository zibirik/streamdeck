using System.Text;
using System.Text.Json;
using StreamPanel.Server.Infrastructure;
using StreamPanel.Server.Windows;

namespace StreamPanel.Server.Endpoints;

public static class Cs2Endpoints
{
    public static void MapCs2Endpoints(this WebApplication app)
    {
        app.MapGet("/integrations/cs2/gsi", (HttpContext context, IGameTelemetryService telemetry) => Results.Ok(new
        {
            ok = true,
            message = "CS2 GSI endpoint is ready. Send POST JSON here from Counter-Strike 2.",
            configUrl = $"{BaseUrl(context)}/integrations/cs2/config",
            statusUrl = $"{BaseUrl(context)}/integrations/cs2/status",
            receivedPayloads = telemetry.ReceivedPayloads,
            lastUpdateUtc = telemetry.LastUpdateUtc,
            lastStatus = telemetry.LastStatus
        }));

        app.MapPost("/integrations/cs2/gsi", async (HttpContext context, IGameTelemetryService telemetry) =>
        {
            try
            {
                using var payload = await JsonDocument.ParseAsync(context.Request.Body, cancellationToken: context.RequestAborted);
                telemetry.UpdateFromCounterStrike(payload.RootElement.Clone());
                return Results.Ok(new { ok = true, source = "cs2-gsi" });
            }
            catch (JsonException)
            {
                return Results.BadRequest(new { ok = false, error = "invalid_json" });
            }
        });

        app.MapGet("/integrations/cs2/status", (IGameTelemetryService telemetry) => Results.Ok(new
        {
            ok = true,
            receivedPayloads = telemetry.ReceivedPayloads,
            lastUpdateUtc = telemetry.LastUpdateUtc,
            lastStatus = telemetry.LastStatus
        }));

        app.MapGet("/integrations/cs2/config", (HttpContext context, PinAuthService pinAuth) =>
            Results.Text(BuildConfig(context, pinAuth), "text/plain; charset=utf-8"));

        app.MapGet("/integrations/cs2/download-config", (HttpContext context, PinAuthService pinAuth) =>
            Results.File(
                Encoding.UTF8.GetBytes(BuildConfig(context, pinAuth)),
                "text/plain",
                "gamestate_integration_streampanel.cfg"));
    }

    private static string BaseUrl(HttpContext context) => $"{context.Request.Scheme}://{context.Request.Host}";

    private static string BuildConfig(HttpContext context, PinAuthService pinAuth)
    {
        var uri = $"{BaseUrl(context)}/integrations/cs2/gsi";
        // CS2 cannot send custom headers, so the PIN travels in the URL of the generated file.
        if (pinAuth.PinForGeneratedConfig is { } pin)
        {
            uri += $"?pin={Uri.EscapeDataString(pin)}";
        }

        return $$"""
        "StreamPanel CS2 GSI"
        {
          "uri" "{{uri}}"
          "timeout" "5.0"
          "buffer"  "0.1"
          "throttle" "0.1"
          "heartbeat" "10.0"
          "data"
          {
            "provider" "1"
            "map" "1"
            "round" "1"
            "player_id" "1"
            "player_state" "1"
            "player_weapons" "1"
          }
        }
        """;
    }
}
