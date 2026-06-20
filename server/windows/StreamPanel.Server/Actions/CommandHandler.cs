using System.Diagnostics;
using System.Runtime.InteropServices;
using StreamPanel.Server.CommandProtocol;
using StreamPanel.Server.Infrastructure;
using StreamPanel.Server.Windows;

namespace StreamPanel.Server.Actions;

public interface ICommandHandler
{
    Task<PcCommandResponse> HandleAsync(PcCommand command, CancellationToken cancellationToken);
}

public sealed class CommandHandler(
    IInputSimulator inputSimulator,
    IVolumeController volumeController,
    IWindowController windowController,
    IAudioOutputSwitcher audioOutputSwitcher,
    IProcessController processController,
    IClipboardService clipboardService,
    PinAuthService pinAuthService,
    IConfiguration configuration,
    ILogger<CommandHandler> logger
) : ICommandHandler
{
    private const int SupportedProtocolVersion = 1;

    public Task<PcCommandResponse> HandleAsync(PcCommand command, CancellationToken cancellationToken)
    {
        if (command.ProtocolVersion != SupportedProtocolVersion)
        {
            return Task.FromResult(PcCommandResponse.Failure(command.Id, $"Unsupported protocol version {command.ProtocolVersion}."));
        }

        try
        {
            var response = command.Type switch
            {
                PcCommandType.OPEN_URL => OpenUrl(command),
                PcCommandType.LAUNCH_PROCESS => LaunchProcess(command),
                PcCommandType.SEND_TEXT => SendText(command),
                PcCommandType.HOTKEY => SendHotkey(command),
                PcCommandType.MOUSE_COMMAND => SendMouseCommand(command),
                PcCommandType.MEDIA_COMMAND => SendMediaCommand(command),
                PcCommandType.VOLUME_COMMAND => SendVolumeCommand(command),
                PcCommandType.WINDOW_COMMAND => SendWindowCommand(command),
                PcCommandType.OPEN_FOLDER => OpenFolder(command),
                PcCommandType.OPEN_URLS => OpenUrls(command),
                PcCommandType.SYSTEM_COMMAND => RunSystemCommand(command),
                PcCommandType.CUSTOM => HandleCustom(command),
                _ => PcCommandResponse.Failure(command.Id, $"Unsupported command type {command.Type}.")
            };
            return Task.FromResult(response);
        }
        catch (Exception exception)
        {
            logger.LogError(exception, "Command {CommandId} failed", command.Id);
            return Task.FromResult(PcCommandResponse.Failure(command.Id, exception.Message));
        }
    }

    private static PcCommandResponse OpenUrl(PcCommand command)
    {
        var url = Required(command, "url");
        Process.Start(new ProcessStartInfo(url) { UseShellExecute = true });
        return PcCommandResponse.Success(command.Id, "Opened URL.");
    }

    private static PcCommandResponse OpenUrls(PcCommand command)
    {
        var raw = Required(command, "urls");
        var urls = raw.Split(['|', ';', '\n', '\r'], StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);
        if (urls.Length == 0)
        {
            return PcCommandResponse.Failure(command.Id, "No URLs provided.");
        }

        foreach (var url in urls)
        {
            Process.Start(new ProcessStartInfo(url) { UseShellExecute = true });
        }

        return PcCommandResponse.Success(command.Id, $"Opened {urls.Length} URLs.");
    }

    private static PcCommandResponse OpenFolder(PcCommand command)
    {
        var path = PathExpander.Expand(Required(command, "path"));
        if (!Directory.Exists(path))
        {
            return PcCommandResponse.Failure(command.Id, $"Folder not found: {path}");
        }

        Process.Start(new ProcessStartInfo("explorer.exe", path) { UseShellExecute = true });
        return PcCommandResponse.Success(command.Id, $"Opened folder {path}.");
    }

    private static PcCommandResponse LaunchProcess(PcCommand command)
    {
        var path = PathExpander.Expand(Required(command, "path"));
        var runAsAdmin = string.Equals(command.Payload.GetValueOrDefault("runAsAdmin"), "true", StringComparison.OrdinalIgnoreCase);
        var startInfo = new ProcessStartInfo(path) { UseShellExecute = true };
        if (runAsAdmin)
        {
            startInfo.Verb = "runas";
        }
        Process.Start(startInfo);
        return PcCommandResponse.Success(command.Id, runAsAdmin ? $"Launched as admin: {path}." : $"Launched {path}.");
    }

    private PcCommandResponse SendText(PcCommand command)
    {
        var text = Required(command, "text");
        inputSimulator.SendText(text);
        return PcCommandResponse.Success(command.Id, "Text sent.");
    }

    private PcCommandResponse SendHotkey(PcCommand command)
    {
        var keys = Required(command, "keys");
        inputSimulator.SendHotkey(keys);
        return PcCommandResponse.Success(command.Id, $"Hotkey {keys} sent.");
    }

    private PcCommandResponse SendMouseCommand(PcCommand command)
    {
        var mouseCommand = Required(command, "command").ToLowerInvariant();
        var x = command.Payload.TryGetValue("x", out var rawX) ? int.Parse(rawX) : 0;
        var y = command.Payload.TryGetValue("y", out var rawY) ? int.Parse(rawY) : 0;
        var delta = command.Payload.TryGetValue("delta", out var rawDelta) ? int.Parse(rawDelta) : 0;

        switch (mouseCommand)
        {
            case "move":
                inputSimulator.MoveMouse(x, y);
                break;
            case "left_click":
                inputSimulator.LeftClick();
                break;
            case "right_click":
                inputSimulator.RightClick();
                break;
            case "scroll":
                inputSimulator.Scroll(delta);
                break;
            default:
                return PcCommandResponse.Failure(command.Id, $"Unsupported mouse command '{mouseCommand}'.");
        }

        return PcCommandResponse.Success(command.Id, $"Mouse command '{mouseCommand}' sent.");
    }

    private PcCommandResponse SendMediaCommand(PcCommand command)
    {
        var mediaKey = Required(command, "action");
        inputSimulator.SendMediaKey(mediaKey);
        return PcCommandResponse.Success(command.Id, $"Media key '{mediaKey}' sent.");
    }

    private PcCommandResponse SendVolumeCommand(PcCommand command)
    {
        var action = Required(command, "action").ToLowerInvariant();
        switch (action)
        {
            case "get":
                var current = volumeController.GetVolumePercent();
                return PcCommandResponse.Success(command.Id, $"volume:{current}");
            case "get_mute":
                return PcCommandResponse.Success(command.Id, $"muted:{volumeController.GetMuted()}");
            case "get_mic_mute":
                return PcCommandResponse.Success(command.Id, $"micMuted:{volumeController.GetMicMuted()}");
            case "set":
                var percent = int.Parse(Required(command, "percent"));
                volumeController.SetVolumePercent(percent);
                break;
            case "up":
                volumeController.VolumeUp(command.Payload.TryGetValue("steps", out var upSteps) ? int.Parse(upSteps) : 2);
                break;
            case "down":
                volumeController.VolumeDown(command.Payload.TryGetValue("steps", out var downSteps) ? int.Parse(downSteps) : 2);
                break;
            case "mute":
                volumeController.Mute();
                break;
            case "mic_mute":
            case "mic_mute_toggle":
                volumeController.ToggleMicMute();
                break;
            case "switch_output":
                var device = Required(command, "device");
                audioOutputSwitcher.SwitchTo(device);
                break;
            default:
                return PcCommandResponse.Failure(command.Id, $"Unsupported volume action '{action}'.");
        }

        return PcCommandResponse.Success(command.Id, $"Volume action '{action}' executed.");
    }

    private PcCommandResponse SendWindowCommand(PcCommand command)
    {
        var action = Required(command, "action").ToLowerInvariant();
        switch (action)
        {
            case "minimize_all":
                windowController.MinimizeAll();
                break;
            case "maximize_active":
                windowController.MaximizeActive();
                break;
            case "minimize_active":
                windowController.MinimizeActive();
                break;
            case "close_active":
                windowController.CloseActive();
                break;
            case "alt_tab":
                windowController.AltTab();
                break;
            case "fullscreen":
                windowController.FullscreenToggle();
                break;
            case "snap_left":
                windowController.SnapLeft();
                break;
            case "snap_right":
                windowController.SnapRight();
                break;
            case "move_monitor":
                var monitor = int.Parse(command.Payload.TryGetValue("monitor", out var rawMonitor) ? rawMonitor : "2");
                windowController.MoveActiveToMonitor(monitor);
                break;
            default:
                return PcCommandResponse.Failure(command.Id, $"Unsupported window action '{action}'.");
        }

        return PcCommandResponse.Success(command.Id, $"Window action '{action}' executed.");
    }

    private PcCommandResponse RunSystemCommand(PcCommand command)
    {
        var name = Required(command, "name").ToLowerInvariant();
        var allowed = configuration
            .GetSection("StreamPanel:AllowedSystemCommands")
            .Get<string[]>()?
            .Select(value => value.ToLowerInvariant())
            .ToHashSet(StringComparer.OrdinalIgnoreCase) ?? [];

        if (!allowed.Contains(name))
        {
            return PcCommandResponse.Failure(command.Id, $"System command '{name}' is not allowlisted.");
        }

        switch (name)
        {
            case "lock":
                Process.Start(new ProcessStartInfo("rundll32.exe", "user32.dll,LockWorkStation") { UseShellExecute = false });
                break;
            case "sleep":
                Process.Start(new ProcessStartInfo("rundll32.exe", "powrprof.dll,SetSuspendState 0,1,0") { UseShellExecute = false });
                break;
            case "task_manager":
                inputSimulator.SendHotkey("CTRL+SHIFT+ESC");
                break;
            case "screenshot":
                inputSimulator.SendHotkey("WIN+SHIFT+S");
                break;
            case "kill_process":
                processController.KillForegroundProcess();
                break;
            case "get_clipboard":
                var clipText = clipboardService.GetText() ?? string.Empty;
                return PcCommandResponse.Success(command.Id, $"clipboard:{clipText}");
            case "set_clipboard":
                clipboardService.SetText(command.Payload.TryGetValue("text", out var text) ? text : string.Empty);
                return PcCommandResponse.Success(command.Id, "Clipboard updated.");
            case "clean_temp":
                CleanTempFolders();
                break;
            case "focus_mode_on":
                Process.Start(new ProcessStartInfo("powershell.exe", "-NoProfile -Command \"New-Item -Path HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Notifications\\Settings -Force | Out-Null; Set-ItemProperty -Path HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Notifications\\Settings -Name NOC_GLOBAL_SETTING_TOASTS_ENABLED -Value 0\"") { UseShellExecute = false, CreateNoWindow = true });
                break;
            case "focus_mode_off":
                Process.Start(new ProcessStartInfo("powershell.exe", "-NoProfile -Command \"Set-ItemProperty -Path HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Notifications\\Settings -Name NOC_GLOBAL_SETTING_TOASTS_ENABLED -Value 1\"") { UseShellExecute = false, CreateNoWindow = true });
                break;
            case "kill_process_pid":
                var pid = int.Parse(Required(command, "pid"));
                Process.GetProcessById(pid).Kill(entireProcessTree: true);
                break;
            case "git_pull":
                RunShell("git", "pull", GetWorkDir(command));
                break;
            case "git_status":
                RunShell("git", "status", GetWorkDir(command));
                break;
            case "docker_ps":
                RunShell("docker", "ps", null);
                break;
            case "docker_compose_up":
                RunShell("docker", "compose up -d", GetWorkDir(command));
                break;
            case "wifi_restart":
                RunShell(
                    "powershell.exe",
                    "-NoProfile -Command \"Get-NetAdapter | Where-Object Status -eq 'Up' | Restart-NetAdapter -Confirm:$false\"",
                    null);
                break;
            case "flush_dns":
                RunShell("ipconfig", "/flushdns", null);
                break;
            case "empty_recycle_bin":
                RunShell(
                    "powershell.exe",
                    "-NoProfile -Command \"Clear-RecycleBin -Force -ErrorAction SilentlyContinue\"",
                    null);
                break;
            case "discord_mute":
                FocusProcess("Discord");
                Thread.Sleep(150);
                inputSimulator.SendHotkey("CTRL+SHIFT+M");
                break;
            case "discord_deafen":
                FocusProcess("Discord");
                Thread.Sleep(150);
                inputSimulator.SendHotkey("CTRL+SHIFT+D");
                break;
            case "discord_ptt":
                FocusProcess("Discord");
                Thread.Sleep(150);
                inputSimulator.SendHotkey(command.Payload.GetValueOrDefault("keys", "CTRL+`"));
                break;
        }

        return PcCommandResponse.Success(command.Id, $"System command '{name}' executed.");
    }

    private PcCommandResponse HandleCustom(PcCommand command)
    {
        if (command.Payload.TryGetValue("action", out var action) &&
            action.Equals("ping", StringComparison.OrdinalIgnoreCase))
        {
            return PcCommandResponse.Success(command.Id, "pong");
        }

        if (command.Payload.TryGetValue("action", out var authAction) &&
            authAction.Equals("auth", StringComparison.OrdinalIgnoreCase))
        {
            command.Payload.TryGetValue("pin", out var pin);
            return pinAuthService.TryAuthenticate(pin)
                ? PcCommandResponse.Success(command.Id, "Authenticated.")
                : PcCommandResponse.Failure(command.Id, "Invalid PIN.");
        }

        return PcCommandResponse.Success(command.Id, "Custom command accepted.");
    }

    private static string GetWorkDir(PcCommand command)
    {
        if (command.Payload.TryGetValue("path", out var path) && !string.IsNullOrWhiteSpace(path))
        {
            return PathExpander.Expand(path);
        }

        return Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
    }

    private static void RunShell(string fileName, string arguments, string? workingDirectory)
    {
        var startInfo = new ProcessStartInfo(fileName, arguments)
        {
            UseShellExecute = false,
            CreateNoWindow = true,
        };
        if (!string.IsNullOrWhiteSpace(workingDirectory))
        {
            startInfo.WorkingDirectory = workingDirectory;
        }

        Process.Start(startInfo);
    }

    private static void CleanTempFolders()
    {
        foreach (var path in new[] { Path.GetTempPath(), Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Temp") })
        {
            if (!Directory.Exists(path)) continue;
            foreach (var file in Directory.EnumerateFiles(path))
            {
                try { File.Delete(file); } catch { /* skip locked files */ }
            }
        }
    }

    private static void FocusProcess(string processName)
    {
        var process = Process.GetProcessesByName(processName)
            .FirstOrDefault(p => p.MainWindowHandle != IntPtr.Zero);
        if (process == null)
        {
            var updatePath = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "Discord",
                "Update.exe");
            if (File.Exists(updatePath))
            {
                Process.Start(new ProcessStartInfo(updatePath, "--processStart Discord.exe") { UseShellExecute = true });
            }
            Thread.Sleep(800);
            process = Process.GetProcessesByName(processName)
                .FirstOrDefault(p => p.MainWindowHandle != IntPtr.Zero);
        }

        if (process?.MainWindowHandle is { } handle && handle != IntPtr.Zero)
        {
            ShowWindow(handle, 9); // SW_RESTORE
            SetForegroundWindow(handle);
        }
    }

    private static string Required(PcCommand command, string key)
    {
        if (command.Payload.TryGetValue(key, out var value) && !string.IsNullOrWhiteSpace(value))
        {
            return value;
        }

        throw new InvalidOperationException($"Missing payload key '{key}'.");
    }

    [DllImport("user32.dll")]
    private static extern bool SetForegroundWindow(IntPtr hWnd);

    [DllImport("user32.dll")]
    private static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
}
