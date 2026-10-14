using System.Net;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.DependencyInjection;
using StreamPanel.Server.Infrastructure;

namespace StreamPanel.Server.Tests;

public class RequestPinMiddlewareTests
{
    private static readonly IPAddress Remote = IPAddress.Parse("192.168.1.50");
    private static readonly IPAddress ThisPc = IPAddress.Parse("192.168.1.10");

    [Theory]
    [InlineData("/status")]
    [InlineData("/api/configurator/draft")]
    [InlineData("/integrations/cs2/config")]
    [InlineData("/gsi")]
    [InlineData("/STATUS")]
    public async Task RemoteWithoutPin_IsRejected(string path)
    {
        var result = await Send("1234", path, remote: Remote);

        Assert.Equal(StatusCodes.Status401Unauthorized, result.Status);
        Assert.False(result.ReachedEndpoint);
    }

    [Fact]
    public async Task RemoteWithWrongPin_IsRejected()
    {
        var result = await Send("1234", "/status", remote: Remote, header: "9999");

        Assert.Equal(StatusCodes.Status401Unauthorized, result.Status);
        Assert.False(result.ReachedEndpoint);
    }

    [Fact]
    public async Task RemoteWithPinHeader_IsAllowed()
    {
        var result = await Send("1234", "/status", remote: Remote, header: "1234");

        Assert.True(result.ReachedEndpoint);
    }

    [Fact]
    public async Task RemoteWithPinQuery_IsAllowed()
    {
        var result = await Send("1234", "/integrations/cs2/gsi", remote: Remote, query: "?pin=1234");

        Assert.True(result.ReachedEndpoint);
    }

    [Fact]
    public async Task RepeatedWrongPins_AreRateLimited()
    {
        var services = BuildServices("1234");
        for (var i = 0; i < PinAuthService.MaxFailedAttempts; i++)
        {
            await Send(services, "/status", Remote, header: "bad");
        }

        var result = await Send(services, "/status", Remote, header: "1234");

        Assert.Equal(StatusCodes.Status429TooManyRequests, result.Status);
        Assert.False(result.ReachedEndpoint);
    }

    [Theory]
    [InlineData("/")]
    [InlineData("/api")]
    [InlineData("/index.html")]
    [InlineData("/ws")]
    public async Task PublicPaths_DoNotNeedPin(string path)
    {
        var result = await Send("1234", path, remote: Remote);

        Assert.True(result.ReachedEndpoint);
    }

    [Fact]
    public async Task LoopbackAndOwnAddress_SkipThePin()
    {
        Assert.True((await Send("1234", "/status", remote: IPAddress.Loopback)).ReachedEndpoint);
        Assert.True((await Send("1234", "/status", remote: IPAddress.IPv6Loopback)).ReachedEndpoint);
        Assert.True((await Send("1234", "/status", remote: ThisPc)).ReachedEndpoint);
    }

    [Fact]
    public async Task NoPinConfigured_EverythingIsOpen()
    {
        var result = await Send("", "/status", remote: Remote);

        Assert.True(result.ReachedEndpoint);
    }

    [Theory]
    [InlineData("/gsi")]
    [InlineData("/integrations/cs2/gsi")]
    public async Task CounterStrikePost_DoesNotNeedAPin(string path)
    {
        var result = await Send("1234", path, remote: Remote, method: "POST");

        Assert.True(result.ReachedEndpoint);
    }

    [Fact]
    public async Task CounterStrikeGet_StillNeedsAPinFromTheLan()
    {
        var result = await Send("1234", "/integrations/cs2/gsi", remote: Remote, method: "GET");

        Assert.Equal(StatusCodes.Status401Unauthorized, result.Status);
        Assert.False(result.ReachedEndpoint);
    }

    private static Task<(int Status, bool ReachedEndpoint)> Send(
        string pin, string path, IPAddress remote, string? header = null, string query = "", string method = "GET") =>
        Send(BuildServices(pin), path, remote, header, query, method);

    private static async Task<(int Status, bool ReachedEndpoint)> Send(
        IServiceProvider services, string path, IPAddress remote, string? header = null, string query = "", string method = "GET")
    {
        var reached = false;
        var pipeline = new ApplicationBuilder(services);
        pipeline.UseRequestPin();
        pipeline.Run(_ =>
        {
            reached = true;
            return Task.CompletedTask;
        });

        var context = new DefaultHttpContext { RequestServices = services };
        context.Request.Method = method;
        context.Request.Path = path;
        context.Request.QueryString = new QueryString(query);
        context.Connection.RemoteIpAddress = remote;
        context.Connection.LocalIpAddress = ThisPc;
        context.Response.Body = new MemoryStream();
        if (header is not null) context.Request.Headers[RequestPinMiddleware.HeaderName] = header;

        await pipeline.Build()(context);
        return (context.Response.StatusCode, reached);
    }

    private static IServiceProvider BuildServices(string pin) =>
        new ServiceCollection()
            .AddSingleton(PinAuthServiceTests.Create(pin))
            .BuildServiceProvider();
}
