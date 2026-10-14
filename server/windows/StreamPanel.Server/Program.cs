using System.Text.Json;
using System.Text.Json.Serialization;
using StreamPanel.Server.Actions;
using StreamPanel.Server.CommandProtocol;
using StreamPanel.Server.Endpoints;
using StreamPanel.Server.Infrastructure;
using StreamPanel.Server.Windows;

var builder = WebApplication.CreateBuilder(args);

// The user's own settings live outside the program folder, so unzipping a new version over the old one never loses them.
try { Directory.CreateDirectory(UserData.Directory); } catch { /* read-only profile: the file is optional */ }
builder.Configuration.AddJsonFile(UserData.SettingsFilePath, optional: true, reloadOnChange: true);

// Secure by default: with no PIN chosen, make one up (once) and show it on the PC. "Pin": "off" disables it on purpose.
var pinSetup = PinBootstrap.Resolve(builder.Configuration["StreamPanel:Pin"], UserData.Directory);
builder.Configuration["StreamPanel:Pin"] = pinSetup.Pin ?? "";
var port = builder.Configuration.GetValue("StreamPanel:Port", 17820);
var startedAt = DateTimeOffset.UtcNow;

builder.WebHost.UseUrls($"http://0.0.0.0:{port}");
builder.WebHost.ConfigureKestrel(options =>
{
    // Commands, drafts and CS2 payloads are tiny; refuse anything big before it is buffered.
    options.Limits.MaxRequestBodySize = 1024 * 1024;
    // CS2's HTTP client can dribble the body; Kestrel's default min-rate would abort the POST.
    options.Limits.MinRequestBodyDataRate = null;
});

builder.Services.AddSingleton(TimeProvider.System);
builder.Services.AddSingleton<PinAuthService>();
builder.Services.AddSingleton(sp => UrlGuard.FromConfiguration(sp.GetRequiredService<IConfiguration>()));
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
builder.Services.AddSingleton<ICs2IntegrationService, Cs2IntegrationService>();
builder.Services.AddSingleton<CommandLog>();
builder.Services.AddSingleton<ConnectionTracker>();
builder.Services.AddSingleton(new JsonSerializerOptions
{
    PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
    PropertyNameCaseInsensitive = true,
    Converters = { new JsonStringEnumConverter() }
});

var app = builder.Build();

app.UseRequestPin();
app.UseDefaultFiles();
app.UseStaticFiles();
app.UseWebSockets(new WebSocketOptions { KeepAliveInterval = TimeSpan.FromSeconds(20) });

app.MapConfiguratorEndpoints();
app.MapStatusEndpoints(startedAt);
app.MapCs2Endpoints();
app.MapCommandWebSocket();

var pinRequired = app.Services.GetRequiredService<PinAuthService>().IsRequired;
ServerBanner.Print(port, pinSetup);
TrayHost.StartIfEnabled(builder.Configuration, port, pinSetup);
app.Logger.LogInformation("StreamPanel server listening on ws://0.0.0.0:{Port}/ws (PIN {Pin})", port, pinRequired ? "required" : "NOT set");
app.Run();
