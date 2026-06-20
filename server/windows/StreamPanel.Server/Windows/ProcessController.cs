using System.Diagnostics;
using System.Runtime.InteropServices;

namespace StreamPanel.Server.Windows;

public interface IProcessController
{
    void KillForegroundProcess();
}

public sealed class ProcessController : IProcessController
{
    public void KillForegroundProcess()
    {
        var hwnd = GetForegroundWindow();
        if (hwnd == IntPtr.Zero)
        {
            throw new InvalidOperationException("No active window found.");
        }

        GetWindowThreadProcessId(hwnd, out var pid);
        if (pid == 0)
        {
            throw new InvalidOperationException("Could not resolve foreground process.");
        }

        using var process = Process.GetProcessById((int)pid);
        process.Kill(entireProcessTree: true);
    }

    [DllImport("user32.dll")]
    private static extern IntPtr GetForegroundWindow();

    [DllImport("user32.dll")]
    private static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint processId);
}
