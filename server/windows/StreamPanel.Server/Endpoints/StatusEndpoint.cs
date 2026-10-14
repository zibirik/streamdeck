using StreamPanel.Server.CommandProtocol;
using StreamPanel.Server.Infrastructure;
using StreamPanel.Server.Windows;

namespace StreamPanel.Server.Endpoints;

public static class StatusEndpoint
{
    public static void MapStatusEndpoints(this WebApplication app, DateTimeOffset startedAt)
    {
        // Public, secret-free identity endpoint. The tablet uses it for LAN discovery
        // (it must work before the user has typed a PIN).
        app.MapGet("/api", (PinAuthService pinAuth) => Results.Ok(new
        {
            name = "StreamPanel Server",
            version = AppVersion.Current,
            protocolVersion = 1,
            machineName = Environment.MachineName,
            pinRequired = pinAuth.IsRequired,
            websocket = "/ws",
            status = "/status"
        }));

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
            IGameTelemetryService gameTelemetryService,
            ICs2IntegrationService cs2Integration
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
                .Where(IsUsableDrive)
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
            var systemRoot = Path.GetPathRoot(Environment.SystemDirectory)?.TrimEnd('\\') ?? "C:";
            var primaryDrive = storageDrives.FirstOrDefault(d =>
                d.name.StartsWith(systemRoot, StringComparison.OrdinalIgnoreCase));
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
                cs2 = Cs2Summary(cs2Integration, gameTelemetryService),
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
    }

    /// <summary>Is the Counter-Strike integration set up, and is the game sending data right now?</summary>
    private static object Cs2Summary(ICs2IntegrationService integration, IGameTelemetryService telemetry)
    {
        var install = integration.GetStatus();
        return new
        {
            found = install.Found,
            installed = install.Installed,
            live = telemetry.IsCounterStrikeLive,
            receivedPayloads = telemetry.ReceivedPayloads,
        };
    }

    private static bool IsUsableDrive(DriveInfo drive)
    {
        try
        {
            return drive.IsReady && drive.TotalSize > 0;
        }
        catch (IOException)
        {
            return false;
        }
        catch (UnauthorizedAccessException)
        {
            return false;
        }
    }
}
