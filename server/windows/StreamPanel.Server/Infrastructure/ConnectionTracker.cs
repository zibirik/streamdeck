namespace StreamPanel.Server.Infrastructure;

public sealed class ConnectionTracker
{
    private int _activeConnections;

    public int ActiveConnections => Volatile.Read(ref _activeConnections);

    public void Connected() => Interlocked.Increment(ref _activeConnections);

    public void Disconnected()
    {
        while (true)
        {
            var current = Volatile.Read(ref _activeConnections);
            if (current <= 0) return;
            if (Interlocked.CompareExchange(ref _activeConnections, current - 1, current) == current) return;
        }
    }
}
