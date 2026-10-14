using StreamPanel.Server.Windows;

namespace StreamPanel.Server.Tests;

public class AudioAndMediaTests(Xunit.Abstractions.ITestOutputHelper output)
{
    private static readonly IReadOnlyList<AudioOutputDevice> Devices =
    [
        new("1", "Speakers (Realtek High Definition Audio)", true),
        new("2", "Headphones (WH-1000XM4 Stereo)", false),
        new("3", "LG ULTRAGEAR (NVIDIA High Definition Audio)", false),
    ];

    [Theory]
    [InlineData("headphones", "2")]
    [InlineData("Headphone", "2")]
    [InlineData("headset", "2")]
    [InlineData("speakers", "1")]
    [InlineData("speaker", "1")]
    [InlineData("lg ultragear", "3")]
    [InlineData("WH-1000", "2")]
    public void Choose_FindsTheRightDevice(string target, string expectedId) =>
        Assert.Equal(expectedId, AudioOutputSwitcher.Choose(Devices, target)?.Id);

    [Fact]
    public void Choose_Speakers_FallsBackToAnyNonHeadphoneOutput()
    {
        var onlyMonitorAndHeadphones = Devices.Skip(1).ToList();

        Assert.Equal("3", AudioOutputSwitcher.Choose(onlyMonitorAndHeadphones, "speakers")?.Id);
    }

    [Fact]
    public void Choose_RussianNames()
    {
        var russian = new List<AudioOutputDevice> { new("a", "Динамики (Realtek)", true), new("b", "Наушники (USB)", false) };

        Assert.Equal("a", AudioOutputSwitcher.Choose(russian, "speakers")?.Id);
        Assert.Equal("b", AudioOutputSwitcher.Choose(russian, "headphones")?.Id);
    }

    [Theory]
    [InlineData("")]
    [InlineData("nonexistent device")]
    public void Choose_ReturnsNull_WhenNothingMatches(string target) =>
        Assert.Null(AudioOutputSwitcher.Choose(Devices, target));

    [Fact]
    public void Choose_Headphones_WhenThereAreNone_IsNull() =>
        Assert.Null(AudioOutputSwitcher.Choose(Devices.Take(1).ToList(), "headphones"));

    [Fact]
    public void ListOutputs_TalksToWindowsWithoutCrashing()
    {
        // The COM layout is easy to get wrong (it once crashed here). On a PC without sound this is simply empty.
        var outputs = new AudioOutputSwitcher().ListOutputs();
        foreach (var device in outputs) output.WriteLine($"{(device.IsDefault ? "*" : " ")} {device.Name}");

        Assert.All(outputs, device =>
        {
            Assert.False(string.IsNullOrWhiteSpace(device.Id));
            Assert.False(string.IsNullOrWhiteSpace(device.Name));
        });
        Assert.True(outputs.Count(device => device.IsDefault) <= 1);
    }

    [Fact]
    public void MediaInfo_NeverThrows_EvenWhenNothingIsPlaying()
    {
        var media = new MediaInfoService();

        var exception = Record.Exception(() =>
        {
            var (title, artist) = media.GetNowPlaying();
            output.WriteLine($"now playing: {title} / {artist}, playing={media.IsMediaPlaying()}");
        });

        Assert.Null(exception);
    }
}
