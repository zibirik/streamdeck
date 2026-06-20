using System.Runtime.InteropServices;

namespace StreamPanel.Server.Windows;

public interface IInputSimulator
{
    void SendText(string text);
    void SendHotkey(string hotkey);
    void SendMediaKey(string mediaKey);
    void MoveMouse(int deltaX, int deltaY);
    void LeftClick();
    void RightClick();
    void Scroll(int delta);
}

public sealed class InputSimulator : IInputSimulator
{
    public void SendText(string text)
    {
        foreach (var character in text)
        {
            SendUnicode(character, keyUp: false);
            SendUnicode(character, keyUp: true);
        }
    }

    public void SendMediaKey(string mediaKey)
    {
        var virtualKey = mediaKey.ToLowerInvariant() switch
        {
            "play_pause" or "play" or "pause" => 0xB3,
            "next" => 0xB0,
            "previous" or "prev" => 0xB1,
            "stop" => 0xB2,
            "mute" => 0xAD,
            "volume_up" => 0xAF,
            "volume_down" => 0xAE,
            _ => throw new InvalidOperationException($"Unsupported media key '{mediaKey}'.")
        };

        SendVirtualKey((ushort)virtualKey, keyUp: false);
        SendVirtualKey((ushort)virtualKey, keyUp: true);
    }

    public void SendHotkey(string hotkey)
    {
        var keys = hotkey
            .Split('+', StringSplitOptions.TrimEntries | StringSplitOptions.RemoveEmptyEntries)
            .Select(ParseVirtualKey)
            .ToList();

        if (keys.Count == 0)
        {
            throw new InvalidOperationException("Hotkey cannot be empty.");
        }

        foreach (var key in keys)
        {
            SendVirtualKey(key, keyUp: false);
        }

        foreach (var key in keys.AsEnumerable().Reverse())
        {
            SendVirtualKey(key, keyUp: true);
        }
    }

    public void MoveMouse(int deltaX, int deltaY)
    {
        SendMouse(MOUSEEVENTF_MOVE, deltaX, deltaY, 0);
    }

    public void LeftClick()
    {
        SendMouse(MOUSEEVENTF_LEFTDOWN, 0, 0, 0);
        SendMouse(MOUSEEVENTF_LEFTUP, 0, 0, 0);
    }

    public void RightClick()
    {
        SendMouse(MOUSEEVENTF_RIGHTDOWN, 0, 0, 0);
        SendMouse(MOUSEEVENTF_RIGHTUP, 0, 0, 0);
    }

    public void Scroll(int delta)
    {
        SendMouse(MOUSEEVENTF_WHEEL, 0, 0, delta);
    }

    private static ushort ParseVirtualKey(string key) =>
        key.ToUpperInvariant() switch
        {
            "CTRL" or "CONTROL" => 0x11,
            "SHIFT" => 0x10,
            "ALT" => 0x12,
            "WIN" or "META" => 0x5B,
            "ENTER" => 0x0D,
            "ESC" or "ESCAPE" => 0x1B,
            "TAB" => 0x09,
            "SPACE" => 0x20,
            "`" or "BACKTICK" or "OEM_3" => 0xC0,
            "BACKSPACE" => 0x08,
            "DELETE" or "DEL" => 0x2E,
            "UP" => 0x26,
            "DOWN" => 0x28,
            "LEFT" => 0x25,
            "RIGHT" => 0x27,
            { Length: 1 } value when char.IsLetterOrDigit(value[0]) => (ushort)char.ToUpperInvariant(value[0]),
            var value when value.StartsWith('F') && int.TryParse(value[1..], out var n) && n is >= 1 and <= 24 => (ushort)(0x70 + n - 1),
            _ => throw new InvalidOperationException($"Unsupported hotkey token '{key}'.")
        };

    private static void SendUnicode(char character, bool keyUp)
    {
        var input = new INPUT
        {
            type = INPUT_KEYBOARD,
            U = new InputUnion
            {
                ki = new KEYBDINPUT
                {
                    wScan = (ushort)character,
                    dwFlags = KEYEVENTF_UNICODE | (keyUp ? KEYEVENTF_KEYUP : 0)
                }
            }
        };
        SendInputChecked(input);
    }

    private static void SendVirtualKey(ushort virtualKey, bool keyUp)
    {
        var input = new INPUT
        {
            type = INPUT_KEYBOARD,
            U = new InputUnion
            {
                ki = new KEYBDINPUT
                {
                    wVk = virtualKey,
                    dwFlags = keyUp ? KEYEVENTF_KEYUP : 0
                }
            }
        };
        SendInputChecked(input);
    }

    private static void SendMouse(uint flags, int dx, int dy, int mouseData)
    {
        var input = new INPUT
        {
            type = INPUT_MOUSE,
            U = new InputUnion
            {
                mi = new MOUSEINPUT
                {
                    dx = dx,
                    dy = dy,
                    mouseData = mouseData,
                    dwFlags = flags
                }
            }
        };
        SendInputChecked(input);
    }

    private static void SendInputChecked(INPUT input)
    {
        var sent = SendInput(1, [input], Marshal.SizeOf<INPUT>());
        if (sent != 1)
        {
            throw new InvalidOperationException($"SendInput failed with Win32 error {Marshal.GetLastWin32Error()}.");
        }
    }

    private const int INPUT_KEYBOARD = 1;
    private const int INPUT_MOUSE = 0;
    private const uint KEYEVENTF_KEYUP = 0x0002;
    private const uint KEYEVENTF_UNICODE = 0x0004;
    private const uint MOUSEEVENTF_MOVE = 0x0001;
    private const uint MOUSEEVENTF_LEFTDOWN = 0x0002;
    private const uint MOUSEEVENTF_LEFTUP = 0x0004;
    private const uint MOUSEEVENTF_RIGHTDOWN = 0x0008;
    private const uint MOUSEEVENTF_RIGHTUP = 0x0010;
    private const uint MOUSEEVENTF_WHEEL = 0x0800;

    [DllImport("user32.dll", SetLastError = true)]
    private static extern uint SendInput(uint nInputs, INPUT[] pInputs, int cbSize);

    [StructLayout(LayoutKind.Sequential)]
    private struct INPUT
    {
        public int type;
        public InputUnion U;
    }

    [StructLayout(LayoutKind.Explicit)]
    private struct InputUnion
    {
        [FieldOffset(0)]
        public KEYBDINPUT ki;

        [FieldOffset(0)]
        public MOUSEINPUT mi;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct KEYBDINPUT
    {
        public ushort wVk;
        public ushort wScan;
        public uint dwFlags;
        public uint time;
        public IntPtr dwExtraInfo;
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct MOUSEINPUT
    {
        public int dx;
        public int dy;
        public int mouseData;
        public uint dwFlags;
        public uint time;
        public IntPtr dwExtraInfo;
    }
}
