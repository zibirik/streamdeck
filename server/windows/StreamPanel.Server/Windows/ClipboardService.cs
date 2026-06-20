using System.Runtime.InteropServices;
using System.Text;

namespace StreamPanel.Server.Windows;

public interface IClipboardService
{
    string? GetText();
    void SetText(string text);
}

public sealed class ClipboardService : IClipboardService
{
    public string? GetText()
    {
        if (!OpenClipboard(IntPtr.Zero)) return null;
        try
        {
            var handle = GetClipboardData(CF_UNICODETEXT);
            if (handle == IntPtr.Zero) return null;

            var pointer = GlobalLock(handle);
            if (pointer == IntPtr.Zero) return null;

            try
            {
                return Marshal.PtrToStringUni(pointer);
            }
            finally
            {
                GlobalUnlock(handle);
            }
        }
        finally
        {
            CloseClipboard();
        }
    }

    public void SetText(string text)
    {
        if (!OpenClipboard(IntPtr.Zero))
        {
            throw new InvalidOperationException("Could not open clipboard.");
        }

        try
        {
            EmptyClipboard();
            var bytes = (text.Length + 1) * 2;
            var hGlobal = GlobalAlloc(GMEM_MOVEABLE, (UIntPtr)bytes);
            if (hGlobal == IntPtr.Zero)
            {
                throw new InvalidOperationException("Could not allocate clipboard memory.");
            }

            var target = GlobalLock(hGlobal);
            if (target == IntPtr.Zero)
            {
                throw new InvalidOperationException("Could not lock clipboard memory.");
            }

            try
            {
                Marshal.Copy(Encoding.Unicode.GetBytes(text + '\0'), 0, target, bytes);
            }
            finally
            {
                GlobalUnlock(hGlobal);
            }

            if (SetClipboardData(CF_UNICODETEXT, hGlobal) == IntPtr.Zero)
            {
                throw new InvalidOperationException("Could not set clipboard data.");
            }
        }
        finally
        {
            CloseClipboard();
        }
    }

    private const uint CF_UNICODETEXT = 13;
    private const uint GMEM_MOVEABLE = 0x0002;

    [DllImport("user32.dll")]
    private static extern bool OpenClipboard(IntPtr hWndNewOwner);

    [DllImport("user32.dll")]
    private static extern bool CloseClipboard();

    [DllImport("user32.dll")]
    private static extern bool EmptyClipboard();

    [DllImport("user32.dll")]
    private static extern IntPtr GetClipboardData(uint uFormat);

    [DllImport("user32.dll")]
    private static extern IntPtr SetClipboardData(uint uFormat, IntPtr hMem);

    [DllImport("kernel32.dll")]
    private static extern IntPtr GlobalLock(IntPtr hMem);

    [DllImport("kernel32.dll")]
    private static extern bool GlobalUnlock(IntPtr hMem);

    [DllImport("kernel32.dll")]
    private static extern IntPtr GlobalAlloc(uint uFlags, UIntPtr dwBytes);
}
