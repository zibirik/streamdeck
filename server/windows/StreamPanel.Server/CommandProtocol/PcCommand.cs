namespace StreamPanel.Server.CommandProtocol;

public sealed record PcCommand(
    string Id,
    int ProtocolVersion,
    PcCommandType Type,
    Dictionary<string, string> Payload,
    long CreatedAtEpochMs
);

public enum PcCommandType
{
    OPEN_URL,
    LAUNCH_PROCESS,
    SEND_TEXT,
    HOTKEY,
    MOUSE_COMMAND,
    MEDIA_COMMAND,
    VOLUME_COMMAND,
    WINDOW_COMMAND,
    OPEN_FOLDER,
    OPEN_URLS,
    SYSTEM_COMMAND,
    CUSTOM
}

public sealed record PcCommandResponse(
    string Id,
    bool Ok,
    string Message,
    long CompletedAtEpochMs
)
{
    public static PcCommandResponse Success(string id, string message) =>
        new(id, true, message, DateTimeOffset.UtcNow.ToUnixTimeMilliseconds());

    public static PcCommandResponse Failure(string id, string message) =>
        new(id, false, message, DateTimeOffset.UtcNow.ToUnixTimeMilliseconds());
}
