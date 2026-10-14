using System.Text.Json;
using System.Text.Json.Serialization;
using StreamPanel.Server.Actions;
using StreamPanel.Server.CommandProtocol;
using StreamPanel.Server.Infrastructure;

namespace StreamPanel.Server.Tests;

public class ClientSessionTests
{
    private static readonly JsonSerializerOptions Json = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        PropertyNameCaseInsensitive = true,
        Converters = { new JsonStringEnumConverter() }
    };

    [Fact]
    public async Task WithoutPin_CommandsReachHandler()
    {
        var (session, handler) = Create(pin: "");

        var (command, response) = await session.ProcessAsync(Message("OPEN_URL"), default);

        Assert.True(response.Ok);
        Assert.NotNull(command);
        Assert.Equal(1, handler.Calls);
    }

    [Fact]
    public async Task WithPin_UnauthenticatedCommandIsRejectedAndNeverHandled()
    {
        var (session, handler) = Create(pin: "1234");

        var (_, response) = await session.ProcessAsync(Message("LAUNCH_PROCESS"), default);

        Assert.False(response.Ok);
        Assert.Contains("Authentication required", response.Message);
        Assert.Equal(0, handler.Calls);
    }

    [Fact]
    public async Task WithPin_PingIsAllowedBeforeAuth()
    {
        var (session, handler) = Create(pin: "1234");

        var (_, response) = await session.ProcessAsync(Message("CUSTOM", new { action = "ping" }), default);

        Assert.True(response.Ok);
        Assert.Equal(1, handler.Calls);
    }

    [Fact]
    public async Task CorrectPin_AuthenticatesTheConnection()
    {
        var (session, handler) = Create(pin: "1234");

        var (_, auth) = await session.ProcessAsync(Message("CUSTOM", new { action = "auth", pin = "1234" }), default);
        var (_, after) = await session.ProcessAsync(Message("HOTKEY"), default);

        Assert.True(auth.Ok);
        Assert.True(session.IsAuthenticated);
        Assert.True(after.Ok);
        Assert.Equal(1, handler.Calls);
    }

    [Fact]
    public async Task WrongPin_DoesNotAuthenticate()
    {
        var (session, handler) = Create(pin: "1234");

        var (_, auth) = await session.ProcessAsync(Message("CUSTOM", new { action = "auth", pin = "nope" }), default);
        var (_, after) = await session.ProcessAsync(Message("HOTKEY"), default);

        Assert.False(auth.Ok);
        Assert.False(session.IsAuthenticated);
        Assert.False(after.Ok);
        Assert.Equal(0, handler.Calls);
    }

    [Fact]
    public async Task AuthenticationDoesNotLeakToOtherConnections()
    {
        var pinAuth = PinAuthServiceTests.Create("1234");
        var handler = new CountingHandler();
        var first = new ClientSession(pinAuth, handler, Json, "tablet", isLocal: false);
        var second = new ClientSession(pinAuth, handler, Json, "intruder", isLocal: false);

        await first.ProcessAsync(Message("CUSTOM", new { action = "auth", pin = "1234" }), default);
        var (_, response) = await second.ProcessAsync(Message("HOTKEY"), default);

        Assert.False(response.Ok);
        Assert.Equal(0, handler.Calls);
    }

    [Fact]
    public async Task AuthCommandIsRedactedBeforeLogging()
    {
        var (session, _) = Create(pin: "1234");

        var (command, _) = await session.ProcessAsync(Message("CUSTOM", new { action = "auth", pin = "1234" }), default);

        Assert.NotNull(command);
        Assert.DoesNotContain("pin", command!.Payload.Keys);
        Assert.DoesNotContain("1234", command.Payload.Values);
    }

    [Fact]
    public async Task LocalClientsSkipThePin()
    {
        var handler = new CountingHandler();
        var session = new ClientSession(PinAuthServiceTests.Create("1234"), handler, Json, "local", isLocal: true);

        var (_, response) = await session.ProcessAsync(Message("HOTKEY"), default);

        Assert.True(response.Ok);
    }

    [Theory]
    [InlineData("not json")]
    [InlineData("")]
    [InlineData("{\"type\":\"DOES_NOT_EXIST\"}")]
    public async Task GarbageIsRejectedWithoutThrowing(string text)
    {
        var (session, handler) = Create(pin: "");

        var (_, response) = await session.ProcessAsync(text, default);

        Assert.False(response.Ok);
        Assert.Equal(0, handler.Calls);
    }

    [Fact]
    public async Task MissingPayloadIsTreatedAsEmpty()
    {
        var (session, handler) = Create(pin: "");

        var (command, response) = await session.ProcessAsync("{\"id\":\"x\",\"protocolVersion\":1,\"type\":\"HOTKEY\"}", default);

        Assert.True(response.Ok);
        Assert.NotNull(command!.Payload);
        Assert.Equal(1, handler.Calls);
    }

    private static (ClientSession Session, CountingHandler Handler) Create(string pin)
    {
        var handler = new CountingHandler();
        return (new ClientSession(PinAuthServiceTests.Create(pin), handler, Json, "client", isLocal: false), handler);
    }

    private static string Message(string type, object? payload = null) =>
        JsonSerializer.Serialize(new
        {
            id = Guid.NewGuid().ToString(),
            protocolVersion = 1,
            type,
            payload = payload ?? new { },
            createdAtEpochMs = 0
        }, Json);

    private sealed class CountingHandler : ICommandHandler
    {
        public int Calls { get; private set; }

        public Task<PcCommandResponse> HandleAsync(PcCommand command, CancellationToken cancellationToken)
        {
            Calls++;
            return Task.FromResult(PcCommandResponse.Success(command.Id, "ok"));
        }
    }
}
