using System.Text;
using System.Text.Json;
using StreamPanel.Server.Infrastructure;
using StreamPanel.Server.Windows;

namespace StreamPanel.Server.Endpoints;

public static class Cs2Endpoints
{
    public static void MapCs2Endpoints(this WebApplication app)
    {
        app.MapGet(Cs2IntegrationService.GsiPath, Ready);
        app.MapGet("/integrations/cs2/gsi", Ready);

        // Short path first: CS2's HTTP client has historically failed on long URLs and cannot send a PIN header.
        app.MapPost(Cs2IntegrationService.GsiPath, Ingest);
        app.MapPost("/integrations/cs2/gsi", Ingest);

        app.MapGet("/integrations/cs2/status", (IGameTelemetryService telemetry, ICs2IntegrationService cs2) =>
        {
            var install = cs2.GetStatus();
            return Results.Ok(new
            {
                ok = true,
                live = telemetry.IsCounterStrikeLive,
                found = install.Found,
                installed = install.Installed,
                configDirectory = install.ConfigDirectory,
                receivedPayloads = telemetry.ReceivedPayloads,
                lastUpdateUtc = telemetry.LastUpdateUtc,
                lastStatus = telemetry.LastStatus
            });
        });

        // One tap from the tablet (or the web page): find the game and drop the config file next to it.
        app.MapPost("/integrations/cs2/install", (ICs2IntegrationService cs2, IConfiguration configuration) =>
        {
            var result = cs2.Install(LocalGsiUri(configuration));
            return ResultOf(result);
        });

        app.MapDelete("/integrations/cs2/install", (ICs2IntegrationService cs2) => ResultOf(cs2.Uninstall()));

        app.MapGet("/integrations/cs2/config", (HttpContext context, IConfiguration configuration) =>
            Results.Text(BuildConfig(context, configuration), "text/plain; charset=utf-8"));

        app.MapGet("/integrations/cs2/download-config", (HttpContext context, IConfiguration configuration) =>
            Results.File(
                Encoding.UTF8.GetBytes(BuildConfig(context, configuration)),
                "text/plain",
                Cs2IntegrationService.FileName));
    }

    private static IResult Ready(IGameTelemetryService telemetry) => Results.Ok(new
    {
        ok = true,
        message = "CS2 GSI endpoint is ready. Counter-Strike 2 should POST JSON here.",
        receivedPayloads = telemetry.ReceivedPayloads,
        lastUpdateUtc = telemetry.LastUpdateUtc,
        lastStatus = telemetry.LastStatus
    });

    /// <summary>
    /// CS2 posts JSON, sometimes with no Content-Type and occasionally an empty heartbeat.
    /// A 400 here makes the game stop sending, so empty bodies still count as "we heard you".
    /// </summary>
    private static async Task<IResult> Ingest(HttpContext context, IGameTelemetryService telemetry)
    {
        string text;
        try
        {
            using var reader = new StreamReader(context.Request.Body, Encoding.UTF8, detectEncodingFromByteOrderMarks: true, leaveOpen: true);
            text = await reader.ReadToEndAsync(context.RequestAborted);
        }
        catch (IOException)
        {
            return Results.Text("ok");
        }

        if (string.IsNullOrWhiteSpace(text))
        {
            telemetry.UpdateFromCounterStrike(JsonDocument.Parse("{}").RootElement.Clone());
            return Results.Text("ok");
        }

        try
        {
            using var payload = JsonDocument.Parse(text);
            telemetry.UpdateFromCounterStrike(payload.RootElement.Clone());
            return Results.Text("ok");
        }
        catch (JsonException)
        {
            return Results.BadRequest("invalid_json");
        }
    }

    private static IResult ResultOf(Cs2InstallResult result) =>
        Results.Json(
            new { ok = result.Ok, code = result.Code, message = result.Message, path = result.FilePath },
            statusCode: result.Ok ? StatusCodes.Status200OK
                : result.Code == "cs2_not_found" ? StatusCodes.Status404NotFound
                : StatusCodes.Status500InternalServerError);

    /// <summary>Counter-Strike runs on this PC, so it always talks to loopback (and never needs a PIN header).</summary>
    public static string LocalGsiUri(IConfiguration configuration)
    {
        var port = configuration.GetValue("StreamPanel:Port", 17820);
        return Cs2IntegrationService.LocalGsiUri(port);
    }

    private static string BuildConfig(HttpContext context, IConfiguration configuration)
    {
        if (ClientAddress.IsLocal(context))
        {
            return Cs2IntegrationService.BuildConfig(LocalGsiUri(configuration));
        }

        // Asked from another device: it will be copied to the gaming PC by hand. CS2 cannot send headers,
        // so the generated file points at the short /gsi path (PIN is not required on that POST).
        var host = context.Request.Host.Host;
        var port = configuration.GetValue("StreamPanel:Port", 17820);
        return Cs2IntegrationService.BuildConfig($"http://{host}:{port}{Cs2IntegrationService.GsiPath}");
    }
}
