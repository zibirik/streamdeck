using StreamPanel.Server.Infrastructure;

namespace StreamPanel.Server.Tests;

public class AppVersionTests
{
    [Fact]
    public void CurrentIsASemanticVersionWithoutCommitSuffix()
    {
        Assert.Matches(@"^\d+\.\d+\.\d+(-[0-9A-Za-z.-]+)?$", AppVersion.Current);
        Assert.DoesNotContain("+", AppVersion.Current);
    }
}
