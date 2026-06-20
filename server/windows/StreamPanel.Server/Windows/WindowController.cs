using System.Runtime.InteropServices;

namespace StreamPanel.Server.Windows;

public interface IWindowController
{
    int GetMonitorCount();
    void MinimizeAll();
    void MinimizeActive();
    void MaximizeActive();
    void SnapLeft();
    void SnapRight();
    void CloseActive();
    void AltTab();
    void FullscreenToggle();
    void MoveActiveToMonitor(int monitorIndex);
}

public sealed class WindowController(IInputSimulator inputSimulator) : IWindowController
{
    public int GetMonitorCount() => EnumerateMonitorsSorted().Count;

    public void MinimizeAll() => inputSimulator.SendHotkey("WIN+D");

    public void MinimizeActive() => inputSimulator.SendHotkey("WIN+DOWN");

    public void MaximizeActive() => inputSimulator.SendHotkey("WIN+UP");

    public void SnapLeft() => inputSimulator.SendHotkey("WIN+LEFT");

    public void SnapRight() => inputSimulator.SendHotkey("WIN+RIGHT");

    public void CloseActive() => inputSimulator.SendHotkey("ALT+F4");

    public void AltTab() => inputSimulator.SendHotkey("ALT+TAB");

    public void FullscreenToggle() => inputSimulator.SendHotkey("F11");

    public void MoveActiveToMonitor(int monitorIndex)
    {
        var hwnd = GetForegroundWindow();
        if (hwnd == IntPtr.Zero)
        {
            throw new InvalidOperationException("No active window found.");
        }

        var monitors = EnumerateMonitorsSorted();
        if (monitors.Count == 0)
        {
            throw new InvalidOperationException("No monitors detected.");
        }

        var targetIndex = Math.Clamp(monitorIndex - 1, 0, monitors.Count - 1);
        var target = monitors[targetIndex];

        if (!GetWindowRect(hwnd, out var windowRect))
        {
            throw new InvalidOperationException("Could not read active window bounds.");
        }

        var width = windowRect.Right - windowRect.Left;
        var height = windowRect.Bottom - windowRect.Top;
        var monitorWidth = target.Right - target.Left;
        var monitorHeight = target.Bottom - target.Top;

        // Keep window size but clamp to monitor work area.
        width = Math.Min(width, monitorWidth);
        height = Math.Min(height, monitorHeight);

        var x = target.Left + Math.Max(0, (monitorWidth - width) / 2);
        var y = target.Top + Math.Max(0, (monitorHeight - height) / 2);

        const uint flags = 0x0040; // SWP_SHOWWINDOW

        if (!SetWindowPos(hwnd, IntPtr.Zero, x, y, width, height, flags))
        {
            throw new InvalidOperationException($"Failed to move window to monitor {monitorIndex}.");
        }
    }

    private static List<RECT> EnumerateMonitorsSorted()
    {
        var monitors = new List<RECT>();
        EnumDisplayMonitors(IntPtr.Zero, IntPtr.Zero,
            (IntPtr _, IntPtr _, ref RECT rect, IntPtr _) =>
            {
                monitors.Add(rect);
                return true;
            },
            IntPtr.Zero);
        return monitors
            .OrderBy(m => m.Left)
            .ThenBy(m => m.Top)
            .ToList();
    }

    private delegate bool MonitorEnumProc(IntPtr hMonitor, IntPtr hdcMonitor, ref RECT lprcMonitor, IntPtr dwData);

    [DllImport("user32.dll")]
    private static extern IntPtr GetForegroundWindow();

    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool SetWindowPos(IntPtr hWnd, IntPtr hWndInsertAfter, int x, int y, int cx, int cy, uint uFlags);

    [DllImport("user32.dll", SetLastError = true)]
    private static extern bool GetWindowRect(IntPtr hWnd, out RECT lpRect);

    [DllImport("user32.dll")]
    private static extern bool EnumDisplayMonitors(IntPtr hdc, IntPtr lprcClip, MonitorEnumProc lpfnEnum, IntPtr dwData);

    [StructLayout(LayoutKind.Sequential)]
    private struct RECT
    {
        public int Left;
        public int Top;
        public int Right;
        public int Bottom;
    }
}
