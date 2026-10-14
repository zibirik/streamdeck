using System.Text.Json;
using StreamPanel.Server.Actions;
using StreamPanel.Server.CommandProtocol;

namespace StreamPanel.Server.Infrastructure;

/// <summary>
/// One WebSocket connection. Owns the per-connection authentication state and decides whether a
/// command may reach the <see cref="ICommandHandler"/>.
/// </summary>
public sealed class ClientSession
{
    private const string UnknownId = "unknown";

    private readonly PinAuthService _pinAuth;
    private readonly ICommandHandler _commandHandler;
    private readonly JsonSerializerOptions _jsonOptions;
    private readonly string _clientKey;

    public ClientSession(
        PinAuthService pinAuth,
        ICommandHandler commandHandler,
        JsonSerializerOptions jsonOptions,
        string clientKey,
        bool isLocal)
    {
        _pinAuth = pinAuth;
        _commandHandler = commandHandler;
        _jsonOptions = jsonOptions;
        _clientKey = clientKey;
        // Processes on the PC itself are already trusted; only remote clients must present the PIN.
        IsAuthenticated = isLocal || !pinAuth.IsRequired;
    }

    public bool IsAuthenticated { get; private set; }

    public async Task<(PcCommand? Command, PcCommandResponse Response)> ProcessAsync(
        string text,
        CancellationToken cancellationToken)
    {
        PcCommand? parsed;
        try
        {
            parsed = JsonSerializer.Deserialize<PcCommand>(text, _jsonOptions);
        }
        catch (JsonException)
        {
            return (null, PcCommandResponse.Failure(UnknownId, "Invalid JSON command."));
        }

        if (parsed is null)
        {
            return (null, PcCommandResponse.Failure(UnknownId, "Command body is empty."));
        }

        var command = Normalize(parsed);

        if (IsAction(command, "auth"))
        {
            return Authenticate(command);
        }

        if (!IsAuthenticated && !IsAction(command, "ping"))
        {
            return (command, PcCommandResponse.Failure(command.Id, "Authentication required. Enter the server PIN on the tablet."));
        }

        var response = await _commandHandler.HandleAsync(command, cancellationToken);
        return (command, response);
    }

    private (PcCommand? Command, PcCommandResponse Response) Authenticate(PcCommand command)
    {
        // Never keep the PIN in logs or in the recent-commands list exposed by /status.
        var redacted = command with { Payload = new Dictionary<string, string> { ["action"] = "auth" } };

        command.Payload.TryGetValue("pin", out var pin);
        var result = _pinAuth.Authenticate(pin, _clientKey);
        switch (result)
        {
            case PinAuthResult.Success:
                IsAuthenticated = true;
                return (redacted, PcCommandResponse.Success(command.Id, "Authenticated."));
            case PinAuthResult.LockedOut:
                var seconds = (int)Math.Ceiling(_pinAuth.RemainingLockout(_clientKey).TotalSeconds);
                return (redacted, PcCommandResponse.Failure(command.Id, $"Too many wrong PINs. Try again in {seconds}s."));
            default:
                return (redacted, PcCommandResponse.Failure(command.Id, "Invalid PIN."));
        }
    }

    private static PcCommand Normalize(PcCommand command) => command with
    {
        Id = string.IsNullOrWhiteSpace(command.Id) ? UnknownId : command.Id,
        Payload = command.Payload ?? new Dictionary<string, string>()
    };

    private static bool IsAction(PcCommand command, string action) =>
        command.Type == PcCommandType.CUSTOM &&
        command.Payload.TryGetValue("action", out var value) &&
        string.Equals(value, action, StringComparison.OrdinalIgnoreCase);
}
