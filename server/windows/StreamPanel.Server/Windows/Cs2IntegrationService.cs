using System.Diagnostics;
using System.Text.RegularExpressions;
using Microsoft.Win32;

namespace StreamPanel.Server.Windows;

public sealed record Cs2InstallStatus(bool Found, bool Installed, string? ConfigDirectory, string? FilePath);

public sealed record Cs2InstallResult(bool Ok, string Code, string Message, string? FilePath);

/// <summary>
/// Counter-Strike 2 only talks to us if a small file sits in its cfg folder. This finds that folder
/// (through the running game, Steam's library list, and appmanifest_730) and puts the file there,
/// so nobody has to dig through directories by hand.
/// </summary>
public interface ICs2IntegrationService
{
    Cs2InstallStatus GetStatus();
    Cs2InstallResult Install(string uri);
    Cs2InstallResult Uninstall();
}

public sealed partial class Cs2IntegrationService(IConfiguration configuration) : ICs2IntegrationService
{
    public const string FileName = "gamestate_integration_streampanel.cfg";

    /// <summary>Short path CS2 posts to. The game's HTTP client is picky about long URLs and cannot send headers.</summary>
    public const string GsiPath = "/gsi";

    private static readonly string[] GameFolders =
    [
        "Counter-Strike Global Offensive",
        "Counter-Strike 2",
    ];

    private readonly object _gate = new();
    private (DateTimeOffset At, string? Directory)? _cache;

    public Cs2InstallStatus GetStatus()
    {
        var directory = FindConfigDirectory();
        if (directory is null)
        {
            return new Cs2InstallStatus(false, false, null, null);
        }

        var path = Path.Combine(directory, FileName);
        return new Cs2InstallStatus(true, File.Exists(path), directory, path);
    }

    public Cs2InstallResult Install(string uri)
    {
        var directory = FindConfigDirectory(forceRefresh: true);
        if (directory is null)
        {
            return new Cs2InstallResult(
                false,
                "cs2_not_found",
                "Counter-Strike 2 was not found in your Steam libraries. Install the game, or download the config file by hand.",
                null);
        }

        var path = Path.Combine(directory, FileName);
        try
        {
            File.WriteAllText(path, BuildConfig(uri), new System.Text.UTF8Encoding(false));
        }
        catch (Exception exception) when (exception is IOException or UnauthorizedAccessException)
        {
            return new Cs2InstallResult(false, "write_failed", $"Could not write {path}: {exception.Message}", path);
        }

        return new Cs2InstallResult(true, "installed", "Installed. Restart Counter-Strike 2 once so it picks the file up.", path);
    }

    public Cs2InstallResult Uninstall()
    {
        var status = GetStatus();
        if (!status.Found || status.FilePath is null)
        {
            return new Cs2InstallResult(false, "cs2_not_found", "Counter-Strike 2 was not found.", null);
        }

        try
        {
            if (File.Exists(status.FilePath))
            {
                File.Delete(status.FilePath);
            }
        }
        catch (Exception exception) when (exception is IOException or UnauthorizedAccessException)
        {
            return new Cs2InstallResult(false, "write_failed", $"Could not remove {status.FilePath}: {exception.Message}", status.FilePath);
        }

        return new Cs2InstallResult(true, "removed", "Removed.", status.FilePath);
    }

    /// <summary>
    /// The file Counter-Strike reads. [uri] is where it should send its data.
    /// Tabs match Valve's own examples; CS2 has been seen ignoring space-indented files.
    /// </summary>
    public static string BuildConfig(string uri) =>
        $$"""
        "StreamPanel CS2 GSI"
        {
        	"uri" "{{uri}}"
        	"timeout" "5.0"
        	"buffer" "0.1"
        	"throttle" "0.1"
        	"heartbeat" "5.0"
        	"data"
        	{
        		"provider" "1"
        		"map" "1"
        		"round" "1"
        		"player_id" "1"
        		"player_state" "1"
        		"player_weapons" "1"
        		"player_match_stats" "1"
        	}
        	"output"
        	{
        		"precision_time" "3"
        		"precision_position" "1"
        		"precision_vector" "3"
        	}
        }
        """;

    public static string LocalGsiUri(int port) => $"http://127.0.0.1:{port}{GsiPath}";

    private string? FindConfigDirectory(bool forceRefresh = false)
    {
        lock (_gate)
        {
            // Walking the disks on every status poll would be wasteful; the game does not move around.
            if (!forceRefresh && _cache is { } cached && DateTimeOffset.UtcNow - cached.At < TimeSpan.FromSeconds(30))
            {
                return cached.Directory;
            }

            var found = LocateConfigDirectory();
            _cache = (DateTimeOffset.UtcNow, found);
            return found;
        }
    }

    private string? LocateConfigDirectory()
    {
        var overridePath = configuration["StreamPanel:Cs2CfgDirectory"];
        if (!string.IsNullOrWhiteSpace(overridePath))
        {
            return Directory.Exists(overridePath) ? overridePath : null;
        }

        // If the game is running, its exe path is the most reliable answer (other Steam libraries, D: drives, …).
        foreach (var exe in RunningGameExecutables())
        {
            var fromExe = CfgFromExecutable(exe);
            if (fromExe is not null)
            {
                return fromExe;
            }
        }

        return FindConfigDirectory(SteamLibraries());
    }

    /// <summary>Looks for the CS2 cfg folder inside each Steam library; the first library that has the game wins.</summary>
    public static string? FindConfigDirectory(IEnumerable<string> libraries)
    {
        foreach (var library in libraries)
        {
            foreach (var directory in PossibleCfgDirectories(library))
            {
                if (Directory.Exists(directory))
                {
                    return directory;
                }
            }
        }

        return null;
    }

    /// <summary>
    /// CS2 lives under <c>game/csgo/cfg</c>; leftover CS:GO layouts use <c>csgo/cfg</c>.
    /// Steam's install folder is usually "Counter-Strike Global Offensive" even for CS2.
    /// </summary>
    public static IEnumerable<string> PossibleCfgDirectories(string library)
    {
        foreach (var game in GameFolders)
        {
            yield return Path.Combine(library, "steamapps", "common", game, "game", "csgo", "cfg");
            yield return Path.Combine(library, "steamapps", "common", game, "csgo", "cfg");
        }

        var manifest = Path.Combine(library, "steamapps", "appmanifest_730.acf");
        if (!File.Exists(manifest))
        {
            yield break;
        }

        string text;
        try
        {
            text = File.ReadAllText(manifest);
        }
        catch (IOException)
        {
            yield break;
        }

        var installDir = ParseInstallDir(text);
        if (string.IsNullOrWhiteSpace(installDir))
        {
            yield break;
        }

        yield return Path.Combine(library, "steamapps", "common", installDir, "game", "csgo", "cfg");
        yield return Path.Combine(library, "steamapps", "common", installDir, "csgo", "cfg");
    }

    /// <summary>
    /// Walks up from <c>cs2.exe</c> (…/game/bin/win64/cs2.exe) until <c>csgo/cfg</c> or <c>game/csgo/cfg</c> appears.
    /// </summary>
    public static string? CfgFromExecutable(string exePath)
    {
        if (string.IsNullOrWhiteSpace(exePath))
        {
            return null;
        }

        string? dir;
        try
        {
            dir = Path.GetDirectoryName(Path.GetFullPath(exePath));
        }
        catch (Exception exception) when (exception is ArgumentException or NotSupportedException or PathTooLongException)
        {
            return null;
        }

        for (var i = 0; i < 8 && !string.IsNullOrEmpty(dir); i++)
        {
            var nextToGame = Path.Combine(dir, "csgo", "cfg");
            if (Directory.Exists(nextToGame))
            {
                return nextToGame;
            }

            var nested = Path.Combine(dir, "game", "csgo", "cfg");
            if (Directory.Exists(nested))
            {
                return nested;
            }

            dir = Path.GetDirectoryName(dir);
        }

        return null;
    }

    public static string? ParseInstallDir(string acfText)
    {
        var match = InstallDirPattern().Match(acfText);
        return match.Success ? match.Groups[1].Value : null;
    }

    private static IEnumerable<string> RunningGameExecutables()
    {
        foreach (var name in new[] { "cs2", "csgo" })
        {
            Process[] processes;
            try
            {
                processes = Process.GetProcessesByName(name);
            }
            catch (InvalidOperationException)
            {
                continue;
            }

            foreach (var process in processes)
            {
                using (process)
                {
                    string? path = null;
                    try
                    {
                        path = process.MainModule?.FileName;
                    }
                    catch (Exception exception) when (exception is System.ComponentModel.Win32Exception or InvalidOperationException)
                    {
                        // 32/64-bit mismatch or no permission to read the module list.
                    }

                    if (!string.IsNullOrWhiteSpace(path))
                    {
                        yield return path;
                    }
                }
            }
        }
    }

    private static IEnumerable<string> SteamLibraries()
    {
        var steamRoot = FindSteamRoot();
        if (steamRoot is null)
        {
            yield break;
        }

        yield return steamRoot;

        var libraryFile = Path.Combine(steamRoot, "steamapps", "libraryfolders.vdf");
        if (!File.Exists(libraryFile))
        {
            yield break;
        }

        string text;
        try
        {
            text = File.ReadAllText(libraryFile);
        }
        catch (IOException)
        {
            yield break;
        }

        foreach (var library in ParseLibraryFolders(text))
        {
            yield return library;
        }
    }

    private static string? FindSteamRoot()
    {
        foreach (var candidate in new[]
                 {
                     ReadRegistry(@"HKEY_CURRENT_USER\Software\Valve\Steam", "SteamPath"),
                     ReadRegistry(@"HKEY_LOCAL_MACHINE\SOFTWARE\WOW6432Node\Valve\Steam", "InstallPath"),
                     @"C:\Program Files (x86)\Steam",
                 })
        {
            if (!string.IsNullOrWhiteSpace(candidate) && Directory.Exists(candidate))
            {
                return Path.GetFullPath(candidate);
            }
        }

        return null;
    }

    private static string? ReadRegistry(string key, string name)
    {
        try
        {
            return Registry.GetValue(key, name, null) as string;
        }
        catch (Exception exception) when (exception is System.Security.SecurityException or IOException)
        {
            return null;
        }
    }

    /// <summary>Steam writes its libraries in a small text format: <c>"path"  "D:\\SteamLibrary"</c>.</summary>
    public static IReadOnlyList<string> ParseLibraryFolders(string vdfText)
    {
        var libraries = new List<string>();
        foreach (Match match in LibraryPathPattern().Matches(vdfText))
        {
            var path = match.Groups[1].Value.Replace(@"\\", @"\");
            if (!string.IsNullOrWhiteSpace(path) && !libraries.Contains(path, StringComparer.OrdinalIgnoreCase))
            {
                libraries.Add(path);
            }
        }

        return libraries;
    }

    [GeneratedRegex("\"path\"\\s+\"([^\"]+)\"", RegexOptions.IgnoreCase)]
    private static partial Regex LibraryPathPattern();

    [GeneratedRegex("\"installdir\"\\s+\"([^\"]+)\"", RegexOptions.IgnoreCase)]
    private static partial Regex InstallDirPattern();
}
