using StreamPanel.Server.Infrastructure;

namespace StreamPanel.Server.Tests;

public sealed class PinBootstrapTests : IDisposable
{
    private readonly string _store = Path.Combine(Path.GetTempPath(), "streampanel-pin-" + Guid.NewGuid().ToString("N"));

    public void Dispose()
    {
        if (Directory.Exists(_store)) Directory.Delete(_store, recursive: true);
    }

    [Fact]
    public void ConfiguredPin_IsUsedAsIs_AndNothingIsWritten()
    {
        var result = PinBootstrap.Resolve("  4321 ", _store);

        Assert.Equal("4321", result.Pin);
        Assert.Equal(PinSource.Configured, result.Source);
        Assert.False(Directory.Exists(_store));
    }

    [Theory]
    [InlineData("off")]
    [InlineData("OFF")]
    [InlineData("none")]
    public void OffMeansNoPin_OnPurpose(string value)
    {
        var result = PinBootstrap.Resolve(value, _store);

        Assert.Null(result.Pin);
        Assert.Equal(PinSource.Disabled, result.Source);
    }

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("   ")]
    public void NoPinChosen_MakesASixDigitOne(string? configured)
    {
        var result = PinBootstrap.Resolve(configured, _store);

        Assert.Equal(PinSource.Generated, result.Source);
        Assert.NotNull(result.Pin);
        Assert.Equal(PinBootstrap.Length, result.Pin!.Length);
        Assert.All(result.Pin, c => Assert.InRange(c, '0', '9'));
    }

    [Fact]
    public void GeneratedPin_IsRememberedBetweenRuns()
    {
        var first = PinBootstrap.Resolve("", _store);
        var second = PinBootstrap.Resolve("", _store);

        Assert.Equal(first.Pin, second.Pin);
        Assert.Equal(first.Pin, File.ReadAllText(Path.Combine(_store, PinBootstrap.FileName)).Trim());
    }

    [Fact]
    public void ConfiguredPin_BeatsTheGeneratedOne()
    {
        var generated = PinBootstrap.Resolve("", _store);

        var configured = PinBootstrap.Resolve("7777", _store);

        Assert.Equal("7777", configured.Pin);
        Assert.NotEqual("7777", generated.Pin);
    }

    [Fact]
    public void Generate_ProducesDifferentPins()
    {
        var pins = Enumerable.Range(0, 20).Select(_ => PinBootstrap.Generate()).ToHashSet();

        Assert.True(pins.Count > 10);
    }
}
