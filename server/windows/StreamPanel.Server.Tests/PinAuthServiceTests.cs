using Microsoft.Extensions.Configuration;
using StreamPanel.Server.Infrastructure;

namespace StreamPanel.Server.Tests;

public class PinAuthServiceTests
{
    [Fact]
    public void NoPinConfigured_AlwaysSucceedsAndIsNotRequired()
    {
        var service = Create("");

        Assert.False(service.IsRequired);
        Assert.Equal(PinAuthResult.Success, service.Authenticate(null, "a"));
        Assert.Equal(PinAuthResult.Success, service.Authenticate("anything", "a"));
    }

    [Fact]
    public void CorrectPin_Succeeds()
    {
        var service = Create("1234");

        Assert.True(service.IsRequired);
        Assert.Equal(PinAuthResult.Success, service.Authenticate("1234", "a"));
    }

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("12345")]
    [InlineData("123")]
    [InlineData("0000")]
    public void WrongPin_Fails(string? pin)
    {
        var service = Create("1234");

        Assert.Equal(PinAuthResult.InvalidPin, service.Authenticate(pin, "a"));
    }

    [Fact]
    public void TooManyFailures_LocksOutEvenTheCorrectPin()
    {
        var service = Create("1234");

        for (var i = 0; i < PinAuthService.MaxFailedAttempts - 1; i++)
        {
            Assert.Equal(PinAuthResult.InvalidPin, service.Authenticate("bad", "a"));
        }

        Assert.Equal(PinAuthResult.LockedOut, service.Authenticate("bad", "a"));
        Assert.Equal(PinAuthResult.LockedOut, service.Authenticate("1234", "a"));
        Assert.True(service.RemainingLockout("a") > TimeSpan.Zero);
    }

    [Fact]
    public void LockoutIsPerClient()
    {
        var service = Create("1234");
        for (var i = 0; i < PinAuthService.MaxFailedAttempts; i++) service.Authenticate("bad", "attacker");

        Assert.Equal(PinAuthResult.Success, service.Authenticate("1234", "tablet"));
    }

    [Fact]
    public void LockoutExpires()
    {
        var clock = new FakeClock();
        var service = Create("1234", clock);
        for (var i = 0; i < PinAuthService.MaxFailedAttempts; i++) service.Authenticate("bad", "a");

        clock.Advance(PinAuthService.LockoutDuration + TimeSpan.FromSeconds(1));

        Assert.Equal(PinAuthResult.Success, service.Authenticate("1234", "a"));
    }

    [Fact]
    public void SuccessResetsFailureCounter()
    {
        var service = Create("1234");
        for (var i = 0; i < PinAuthService.MaxFailedAttempts - 1; i++) service.Authenticate("bad", "a");

        Assert.Equal(PinAuthResult.Success, service.Authenticate("1234", "a"));

        for (var i = 0; i < PinAuthService.MaxFailedAttempts - 1; i++)
        {
            Assert.Equal(PinAuthResult.InvalidPin, service.Authenticate("bad", "a"));
        }
    }

    internal static PinAuthService Create(string pin, TimeProvider? clock = null)
    {
        var configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?> { ["StreamPanel:Pin"] = pin })
            .Build();
        return new PinAuthService(configuration, clock ?? new FakeClock());
    }
}

internal sealed class FakeClock : TimeProvider
{
    private DateTimeOffset _now = new(2026, 1, 1, 0, 0, 0, TimeSpan.Zero);

    public override DateTimeOffset GetUtcNow() => _now;

    public void Advance(TimeSpan by) => _now += by;
}
