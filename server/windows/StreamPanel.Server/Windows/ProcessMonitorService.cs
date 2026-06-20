using System.Diagnostics;

namespace StreamPanel.Server.Windows;

public interface IProcessMonitorService
{
    IReadOnlyList<ProcessSnapshot> GetTopProcesses(int count = 5);
}

public sealed record ProcessSnapshot(string Name, int Pid, long MemoryMb);

public sealed class ProcessMonitorService : IProcessMonitorService
{
    public IReadOnlyList<ProcessSnapshot> GetTopProcesses(int count = 5)
    {
        try
        {
            return Process.GetProcesses()
                .Select(p =>
                {
                    try
                    {
                        return new ProcessSnapshot(
                            p.ProcessName,
                            p.Id,
                            p.WorkingSet64 / (1024 * 1024));
                    }
                    catch
                    {
                        return null;
                    }
                    finally
                    {
                        p.Dispose();
                    }
                })
                .Where(p => p is not null)
                .Cast<ProcessSnapshot>()
                .OrderByDescending(p => p.MemoryMb)
                .Take(count)
                .ToList();
        }
        catch
        {
            return Array.Empty<ProcessSnapshot>();
        }
    }
}
