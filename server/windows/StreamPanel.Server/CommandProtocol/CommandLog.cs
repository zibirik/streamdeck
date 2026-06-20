using System.Collections.Concurrent;

namespace StreamPanel.Server.CommandProtocol;

public sealed class CommandLog
{
    private readonly ConcurrentQueue<CommandLogEntry> entries = new();

    public void Add(PcCommand command, PcCommandResponse response)
    {
        entries.Enqueue(
            new CommandLogEntry(
                command.Id,
                command.Type.ToString(),
                response.Ok,
                response.Message,
                DateTimeOffset.UtcNow
            )
        );

        while (entries.Count > 100 && entries.TryDequeue(out _))
        {
        }
    }

    public IReadOnlyList<CommandLogEntry> Recent() => entries.Reverse().Take(25).ToList();
}

public sealed record CommandLogEntry(
    string Id,
    string Type,
    bool Ok,
    string Message,
    DateTimeOffset Timestamp
);
