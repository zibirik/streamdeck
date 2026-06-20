using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Text;

namespace StreamPanel.Server.Windows;

public interface IForegroundWindowTracker
{
    string? GetProcessName();
    string? GetWindowTitle();
}

public sealed class ForegroundWindowTracker : IForegroundWindowTracker
{
    public string? GetProcessName()
    {
        var hwnd = GetForegroundWindow();
        if (hwnd == IntPtr.Zero) return null;

        GetWindowThreadProcessId(hwnd, out var pid);
        if (pid == 0) return null;

        try
        {
            using var process = Process.GetProcessById((int)pid);
            return process.ProcessName;
        }
        catch
        {
            return null;
        }
    }

    public string? GetWindowTitle()
    {
        var hwnd = GetForegroundWindow();
        if (hwnd == IntPtr.Zero) return null;

        var builder = new StringBuilder(512);
        return GetWindowText(hwnd, builder, builder.Capacity) > 0
            ? builder.ToString()
            : null;
    }

    [DllImport("user32.dll")]
    private static extern IntPtr GetForegroundWindow();

    [DllImport("user32.dll")]
    private static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint processId);

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    private static extern int GetWindowText(IntPtr hWnd, StringBuilder text, int count);
}
