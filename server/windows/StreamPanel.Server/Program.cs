using System.Diagnostics;
using System.Net.WebSockets;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using StreamPanel.Server.Actions;
using StreamPanel.Server.CommandProtocol;
using StreamPanel.Server.Infrastructure;
using StreamPanel.Server.Windows;

var builder = WebApplication.CreateBuilder(args);
var port = builder.Configuration.GetValue("StreamPanel:Port", 17820);
var startedAt = DateTimeOffset.UtcNow;
var configuratorDraftPath = Path.Combine(
    Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
    "StreamPanel",
    "web-configurator-draft.json");

builder.WebHost.UseUrls($"http://0.0.0.0:{port}");
builder.Services.AddSingleton<PinAuthService>();
builder.Services.AddSingleton<ICommandHandler, CommandHandler>();
builder.Services.AddSingleton<IInputSimulator, InputSimulator>();
builder.Services.AddSingleton<IVolumeController, VolumeController>();
builder.Services.AddSingleton<IWindowController, WindowController>();
builder.Services.AddSingleton<IAudioOutputSwitcher, AudioOutputSwitcher>();
builder.Services.AddSingleton<IProcessController, ProcessController>();
builder.Services.AddSingleton<IMediaInfoService, MediaInfoService>();
builder.Services.AddSingleton<IForegroundWindowTracker, ForegroundWindowTracker>();
builder.Services.AddSingleton<IHardwareStatsService, HardwareStatsService>();
builder.Services.AddSingleton<IClipboardService, ClipboardService>();
builder.Services.AddSingleton<INetworkStatsService, NetworkStatsService>();
builder.Services.AddSingleton<IProcessMonitorService, ProcessMonitorService>();
builder.Services.AddSingleton<IGameTelemetryService, GameTelemetryService>();
builder.Services.AddSingleton<CommandLog>();
builder.Services.AddSingleton<ConnectionTracker>();
builder.Services.AddSingleton(new JsonSerializerOptions
{
    PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
    PropertyNameCaseInsensitive = true,
    Converters = { new JsonStringEnumConverter() }
});

var app = builder.Build();

app.UseDefaultFiles();
app.UseStaticFiles();

app.UseWebSockets(new WebSocketOptions
{
    KeepAliveInterval = TimeSpan.FromSeconds(20)
});

app.MapGet("/", () => Results.Content(ReadConfiguratorHtml(app.Environment), "text/html; charset=utf-8"));

app.MapGet("/configurator", () => Results.Content(ReadConfiguratorHtml(app.Environment), "text/html; charset=utf-8"));

app.MapGet("/api", () => Results.Ok(new
{
    name = "StreamPanel Server",
    protocolVersion = 1,
    websocket = "/ws",
    status = "/status"
}));

app.MapGet("/api/configurator/draft", () => Results.Text(
    ReadConfiguratorDraft(configuratorDraftPath),
    "application/json; charset=utf-8"));

app.MapPost("/api/configurator/draft", async (HttpContext context) =>
{
    using var reader = new StreamReader(context.Request.Body, Encoding.UTF8);
    var raw = await reader.ReadToEndAsync(context.RequestAborted);
    using var _ = JsonDocument.Parse(raw);
    Directory.CreateDirectory(Path.GetDirectoryName(configuratorDraftPath)!);
    await File.WriteAllTextAsync(configuratorDraftPath, raw, Encoding.UTF8, context.RequestAborted);
    return Results.Ok(new { ok = true, path = configuratorDraftPath });
});

app.MapGet("/status", (
    CommandLog commandLog,
    IVolumeController volumeController,
    IWindowController windowController,
    IMediaInfoService mediaInfoService,
    IForegroundWindowTracker foregroundWindowTracker,
    IHardwareStatsService hardwareStatsService,
    IClipboardService clipboardService,
    INetworkStatsService networkStatsService,
    IProcessMonitorService processMonitorService,
    ConnectionTracker connectionTracker,
    PinAuthService pinAuthService,
    IGameTelemetryService gameTelemetryService
) =>
{
    hardwareStatsService.Refresh();
    networkStatsService.Refresh();
    int? volumePercent = null;
    bool? muted = null;
    bool? micMuted = null;
    try
    {
        volumePercent = volumeController.GetVolumePercent();
        muted = volumeController.GetMuted();
        micMuted = volumeController.GetMicMuted();
    }
    catch
    {
        // Volume may be unavailable on some systems.
    }

    var (title, artist) = mediaInfoService.GetNowPlaying();
    var mediaPlaying = mediaInfoService.IsMediaPlaying();
    var topProcesses = processMonitorService.GetTopProcesses(5)
        .Select(p => new { name = p.Name, pid = p.Pid, memoryMb = p.MemoryMb })
        .ToList();
    var storageDrives = DriveInfo.GetDrives()
        .Where(d => d.IsReady && d.TotalSize > 0)
        .Select(d => new
        {
            name = d.Name.TrimEnd('\\'),
            label = string.IsNullOrWhiteSpace(d.VolumeLabel) ? null : d.VolumeLabel,
            driveType = d.DriveType.ToString(),
            totalGb = Math.Round(d.TotalSize / 1024d / 1024d / 1024d, 1),
            freeGb = Math.Round(d.AvailableFreeSpace / 1024d / 1024d / 1024d, 1),
            freePercent = Math.Round(d.AvailableFreeSpace / (double)d.TotalSize * 100, 1)
        })
        .ToList();
    var primaryDrive = storageDrives.FirstOrDefault(d => d.name.StartsWith(
        Path.GetPathRoot(Environment.SystemDirectory)?.TrimEnd('\\') ?? "C:",
        StringComparison.OrdinalIgnoreCase));
    var foregroundProcess = foregroundWindowTracker.GetProcessName();
    var foregroundTitle = foregroundWindowTracker.GetWindowTitle();

    return Results.Ok(new
    {
        name = "StreamPanel Server",
        protocolVersion = 1,
        machineName = Environment.MachineName,
        startedAt,
        uptimeSeconds = (long)(DateTimeOffset.UtcNow - startedAt).TotalSeconds,
        activeConnections = connectionTracker.ActiveConnections,
        monitorCount = windowController.GetMonitorCount(),
        volumePercent,
        muted,
        micMuted,
        nowPlayingTitle = title,
        nowPlayingArtist = artist,
        mediaPlaying,
        foregroundProcess,
        downloadMbps = networkStatsService.DownloadMbps,
        uploadMbps = networkStatsService.UploadMbps,
        networkInterface = networkStatsService.ActiveInterface,
        diskFreePercent = primaryDrive?.freePercent,
        storageDrives,
        topProcesses,
        foregroundTitle,
        gameInfo = gameTelemetryService.GetStatus(foregroundProcess, foregroundTitle),
        cpuPercent = hardwareStatsService.CpuPercent,
        ramPercent = hardwareStatsService.RamPercent,
        clipboardPreview = clipboardService.GetText() is { Length: > 0 } clip
            ? (clip.Length > 120 ? clip[..120] : clip)
            : null,
        pinRequired = pinAuthService.IsRequired,
        activeClients = connectionTracker.ActiveConnections,
        recentCommands = commandLog.Recent()
    });
});

app.MapGet("/integrations/cs2/gsi", (HttpContext context, IGameTelemetryService gameTelemetryService) => Results.Ok(new
{
    ok = true,
    message = "CS2 GSI endpoint is ready. Send POST JSON here from Counter-Strike 2.",
    configUrl = $"{context.Request.Scheme}://{context.Request.Host}/integrations/cs2/config",
    statusUrl = $"{context.Request.Scheme}://{context.Request.Host}/integrations/cs2/status",
    receivedPayloads = gameTelemetryService.ReceivedPayloads,
    lastUpdateUtc = gameTelemetryService.LastUpdateUtc,
    lastStatus = gameTelemetryService.LastStatus
}));

app.MapPost("/integrations/cs2/gsi", async (
    HttpContext context,
    IGameTelemetryService gameTelemetryService,
    JsonSerializerOptions jsonOptions
) =>
{
    using var payload = await JsonDocument.ParseAsync(context.Request.Body, cancellationToken: context.RequestAborted);
    gameTelemetryService.UpdateFromCounterStrike(payload.RootElement.Clone());
    return Results.Ok(new { ok = true, source = "cs2-gsi" });
});

app.MapGet("/integrations/cs2/status", (IGameTelemetryService gameTelemetryService) => Results.Ok(new
{
    ok = true,
    receivedPayloads = gameTelemetryService.ReceivedPayloads,
    lastUpdateUtc = gameTelemetryService.LastUpdateUtc,
    lastStatus = gameTelemetryService.LastStatus
}));

app.MapGet("/integrations/cs2/config", (HttpContext context) =>
{
    var uri = $"{context.Request.Scheme}://{context.Request.Host}/integrations/cs2/gsi";
    var config = $$"""
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
    return Results.Text(config, "text/plain; charset=utf-8");
});

app.MapGet("/integrations/cs2/download-config", (HttpContext context) =>
{
    var uri = $"{context.Request.Scheme}://{context.Request.Host}/integrations/cs2/gsi";
    var config = $$"""
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
    return Results.File(Encoding.UTF8.GetBytes(config), "text/plain", "gamestate_integration_streampanel.cfg");
});

app.Map("/ws", async (
    HttpContext context,
    ICommandHandler commandHandler,
    JsonSerializerOptions jsonOptions,
    CommandLog commandLog,
    ConnectionTracker connectionTracker,
    ILoggerFactory loggerFactory
) =>
{
    if (!context.WebSockets.IsWebSocketRequest)
    {
        context.Response.StatusCode = StatusCodes.Status400BadRequest;
        return;
    }

    var logger = loggerFactory.CreateLogger("WebSocket");
    using var socket = await context.WebSockets.AcceptWebSocketAsync();
    connectionTracker.Connected();
    logger.LogInformation("Tablet connected from {RemoteIp} ({Active} active)", context.Connection.RemoteIpAddress, connectionTracker.ActiveConnections);

    try
    {
        var buffer = new byte[16 * 1024];
        while (socket.State == WebSocketState.Open && !context.RequestAborted.IsCancellationRequested)
        {
            var result = await socket.ReceiveAsync(new ArraySegment<byte>(buffer), context.RequestAborted);
            if (result.MessageType == WebSocketMessageType.Close)
            {
                await socket.CloseAsync(WebSocketCloseStatus.NormalClosure, "Closed by client", context.RequestAborted);
                break;
            }

            var text = Encoding.UTF8.GetString(buffer, 0, result.Count);
            var stopwatch = Stopwatch.StartNew();
            var handling = await HandleMessage(text, commandHandler, jsonOptions, context.RequestAborted);
            stopwatch.Stop();

            if (handling.Command is not null)
            {
                commandLog.Add(handling.Command, handling.Response);
                logger.LogInformation(
                    "Command {Type} {Id} => {Ok} ({ElapsedMs}ms): {Message}",
                    handling.Command.Type,
                    handling.Command.Id,
                    handling.Response.Ok,
                    stopwatch.ElapsedMilliseconds,
                    handling.Response.Message
                );
            }

            var responseText = JsonSerializer.Serialize(handling.Response, jsonOptions);
            var responseBytes = Encoding.UTF8.GetBytes(responseText);
            await socket.SendAsync(new ArraySegment<byte>(responseBytes), WebSocketMessageType.Text, true, context.RequestAborted);
        }
    }
    finally
    {
        connectionTracker.Disconnected();
        logger.LogInformation("Tablet disconnected ({Active} active)", connectionTracker.ActiveConnections);
    }
});

ServerBanner.Print(port);
TrayHost.StartIfEnabled(builder.Configuration, port);
app.Logger.LogInformation("StreamPanel server listening on ws://0.0.0.0:{Port}/ws", port);
app.Run();

static async Task<(PcCommand? Command, PcCommandResponse Response)> HandleMessage(
    string text,
    ICommandHandler commandHandler,
    JsonSerializerOptions jsonOptions,
    CancellationToken cancellationToken
)
{
    try
    {
        var command = JsonSerializer.Deserialize<PcCommand>(text, jsonOptions);
        if (command is null)
        {
            return (null, PcCommandResponse.Failure("unknown", "Command body is empty."));
        }

        var response = await commandHandler.HandleAsync(command, cancellationToken);
        return (command, response);
    }
    catch (JsonException exception)
    {
        return (null, PcCommandResponse.Failure("unknown", $"Invalid JSON: {exception.Message}"));
    }
}

static string ReadConfiguratorHtml(IWebHostEnvironment environment)
{
    var path = Path.Combine(environment.WebRootPath ?? Path.Combine(AppContext.BaseDirectory, "wwwroot"), "index.html");
    return File.Exists(path)
        ? File.ReadAllText(path)
        : """
        <!doctype html>
        <html><head><title>StreamPanel</title></head>
        <body style="font-family:system-ui;background:#050508;color:white">
        <h1>StreamPanel Configurator</h1>
        <p>index.html was not found in wwwroot.</p>
        </body></html>
        """;
}

static string ReadConfiguratorDraft(string path)
{
    if (File.Exists(path)) return File.ReadAllText(path);
    return """
    {
      "version": 1,
      "buttons": [],
      "macros": [],
      "windowPresets": [],
      "layout": {
        "phoneTabs": ["Deck", "Tools", "Menu"],
        "panels": ["Hardware", "StreamChat", "GameStatus", "Media", "QuickActions"]
      }
    }
    """;
}
