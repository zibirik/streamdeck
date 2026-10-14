using StreamPanel.Server.Actions;

namespace StreamPanel.Server.Tests;

public class UrlGuardTests
{
    private readonly UrlGuard _guard = new(UrlGuard.DefaultSchemes);

    [Theory]
    [InlineData("https://www.youtube.com")]
    [InlineData("http://127.0.0.1:17820")]
    [InlineData("steam://open/main")]
    [InlineData("  https://example.com/a?b=c  ")]
    [InlineData("HTTPS://EXAMPLE.COM")]
    public void AllowsWebAndAppLinks(string value)
    {
        Assert.True(_guard.TryValidate(value, out var url, out _));
        Assert.Equal(value.Trim(), url);
    }

    [Theory]
    [InlineData(@"C:\Windows\System32\cmd.exe")]
    [InlineData("file:///C:/Windows/System32/cmd.exe")]
    [InlineData("javascript:alert(1)")]
    [InlineData(@"\\evil\share\payload.exe")]
    [InlineData("calc.exe")]
    [InlineData("")]
    [InlineData("   ")]
    [InlineData(null)]
    public void RejectsEverythingElse(string? value)
    {
        Assert.False(_guard.TryValidate(value, out var url, out var error));
        Assert.Equal(string.Empty, url);
        Assert.NotEmpty(error);
    }

    [Fact]
    public void HonoursConfiguredSchemes()
    {
        var strict = new UrlGuard(["https"]);

        Assert.True(strict.TryValidate("https://a.com", out _, out _));
        Assert.False(strict.TryValidate("http://a.com", out _, out _));
        Assert.False(strict.TryValidate("steam://open/main", out _, out _));
    }
}
