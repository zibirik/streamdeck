using Microsoft.Extensions.Configuration;
using StreamPanel.Server.Windows;

namespace StreamPanel.Server.Tests;

public sealed class Cs2IntegrationServiceTests : IDisposable
{
    private readonly string _root = Path.Combine(Path.GetTempPath(), "streampanel-cs2-" + Guid.NewGuid().ToString("N"));

    public void Dispose()
    {
        if (Directory.Exists(_root)) Directory.Delete(_root, recursive: true);
    }

    [Fact]
    public void ParseLibraryFolders_ReadsEveryPath_AndUnescapesBackslashes()
    {
        const string vdf = """
            "libraryfolders"
            {
              "0"
              {
                "path"		"C:\\Program Files (x86)\\Steam"
                "label"		""
                "apps" { "730" "30000000000" }
              }
              "1"
              {
                "path"		"D:\\SteamLibrary"
              }
            }
            """;

        var libraries = Cs2IntegrationService.ParseLibraryFolders(vdf);

        Assert.Equal([@"C:\Program Files (x86)\Steam", @"D:\SteamLibrary"], libraries);
    }

    [Fact]
    public void ParseLibraryFolders_IgnoresRubbishAndDuplicates()
    {
        Assert.Empty(Cs2IntegrationService.ParseLibraryFolders("not a vdf file"));
        Assert.Single(Cs2IntegrationService.ParseLibraryFolders("\"path\" \"D:\\\\Games\"\n\"path\" \"d:\\\\games\""));
    }

    [Fact]
    public void FindConfigDirectory_PicksTheLibraryThatHasTheGame()
    {
        var empty = Path.Combine(_root, "LibraryA");
        var withGame = Path.Combine(_root, "LibraryB");
        Directory.CreateDirectory(empty);
        var cfg = MakeCfgDirectory(withGame);

        Assert.Equal(cfg, Cs2IntegrationService.FindConfigDirectory([empty, withGame]));
    }

    [Fact]
    public void FindConfigDirectory_ReturnsNull_WhenNoLibraryHasTheGame() =>
        Assert.Null(Cs2IntegrationService.FindConfigDirectory([_root]));

    [Fact]
    public void Install_WritesTheConfigNextToTheGame_AndUninstallRemovesIt()
    {
        var cfg = MakeCfgDirectory(Path.Combine(_root, "Steam"));
        var service = Create(cfg);
        Assert.False(service.GetStatus().Installed);

        var installed = service.Install("http://127.0.0.1:17820/integrations/cs2/gsi");

        Assert.True(installed.Ok, installed.Message);
        Assert.Equal("installed", installed.Code);
        var file = Path.Combine(cfg, Cs2IntegrationService.FileName);
        Assert.True(File.Exists(file));
        Assert.Contains("\"uri\" \"http://127.0.0.1:17820/integrations/cs2/gsi\"", File.ReadAllText(file));
        Assert.True(service.GetStatus().Installed);

        var removed = service.Uninstall();

        Assert.True(removed.Ok);
        Assert.False(File.Exists(file));
        Assert.False(service.GetStatus().Installed);
    }

    [Fact]
    public void Install_WithoutTheGame_SaysSoClearly()
    {
        var service = Create(Path.Combine(_root, "does-not-exist"));

        var result = service.Install("http://127.0.0.1:17820/integrations/cs2/gsi");

        Assert.False(result.Ok);
        Assert.Equal("cs2_not_found", result.Code);
        Assert.False(service.GetStatus().Found);
    }

    [Fact]
    public void BuildConfig_AsksForEverythingTheHudNeeds()
    {
        var config = Cs2IntegrationService.BuildConfig("http://127.0.0.1:17820/integrations/cs2/gsi");

        foreach (var wanted in new[] { "provider", "map", "round", "player_id", "player_state", "player_weapons", "player_match_stats" })
        {
            Assert.Contains($"\"{wanted}\" \"1\"", config);
        }

        Assert.Contains("\"heartbeat\" \"5.0\"", config);
        Assert.Contains("\"output\"", config);
        Assert.Contains("\t\"uri\"", config);
    }

    [Fact]
    public void CfgFromExecutable_WalksUpFromTheWin64Folder()
    {
        var gameRoot = Path.Combine(_root, "Counter-Strike Global Offensive");
        var cfg = Path.Combine(gameRoot, "game", "csgo", "cfg");
        Directory.CreateDirectory(cfg);
        var exe = Path.Combine(gameRoot, "game", "bin", "win64", "cs2.exe");
        Directory.CreateDirectory(Path.GetDirectoryName(exe)!);
        File.WriteAllText(exe, "");

        Assert.Equal(cfg, Cs2IntegrationService.CfgFromExecutable(exe));
    }

    [Fact]
    public void FindConfigDirectory_AcceptsTheLegacyCsgoLayout()
    {
        var library = Path.Combine(_root, "Legacy");
        var cfg = Path.Combine(library, "steamapps", "common", "Counter-Strike Global Offensive", "csgo", "cfg");
        Directory.CreateDirectory(cfg);

        Assert.Equal(cfg, Cs2IntegrationService.FindConfigDirectory([library]));
    }

    [Fact]
    public void FindConfigDirectory_ReadsInstallDirFromTheAppManifest()
    {
        var library = Path.Combine(_root, "ManifestLib");
        var cfg = Path.Combine(library, "steamapps", "common", "CS2 Custom", "game", "csgo", "cfg");
        Directory.CreateDirectory(cfg);
        Directory.CreateDirectory(Path.Combine(library, "steamapps"));
        File.WriteAllText(
            Path.Combine(library, "steamapps", "appmanifest_730.acf"),
            """
            "AppState"
            {
            	"appid"		"730"
            	"installdir"		"CS2 Custom"
            }
            """);

        Assert.Equal(cfg, Cs2IntegrationService.FindConfigDirectory([library]));
    }

    [Fact]
    public void ParseInstallDir_ReadsTheQuotedFolderName() =>
        Assert.Equal("Counter-Strike Global Offensive", Cs2IntegrationService.ParseInstallDir("\"installdir\"\t\t\"Counter-Strike Global Offensive\""));

    [Fact]
    public void LocalGsiUri_IsTheShortLoopbackPathCs2CanPostTo() =>
        Assert.Equal("http://127.0.0.1:17820/gsi", Cs2IntegrationService.LocalGsiUri(17820));

    private string MakeCfgDirectory(string library)
    {
        var cfg = Path.Combine(library, "steamapps", "common", "Counter-Strike Global Offensive", "game", "csgo", "cfg");
        Directory.CreateDirectory(cfg);
        return cfg;
    }

    private static Cs2IntegrationService Create(string configuredDirectory) =>
        new(new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?> { ["StreamPanel:Cs2CfgDirectory"] = configuredDirectory })
            .Build());
}
