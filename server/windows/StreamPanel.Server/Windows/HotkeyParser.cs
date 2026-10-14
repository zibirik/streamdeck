namespace StreamPanel.Server.Windows;

/// <summary>One key to press: its Windows virtual-key code, and whether Windows treats it as an "extended" key.</summary>
public readonly record struct KeyStroke(ushort VirtualKey, bool Extended);

/// <summary>
/// Reads shortcuts the way people write them: <c>CTRL+SHIFT+ESC</c>, <c>WIN+LEFT</c>, <c>ALT+F4</c>, <c>CTRL+`</c>.
/// Kept apart from the code that presses the keys so it can be tested without touching the keyboard.
/// </summary>
public static class HotkeyParser
{
    public static IReadOnlyList<KeyStroke> Parse(string hotkey)
    {
        var tokens = hotkey.Split('+', StringSplitOptions.TrimEntries | StringSplitOptions.RemoveEmptyEntries);
        if (tokens.Length == 0)
        {
            throw new InvalidOperationException("Hotkey cannot be empty.");
        }

        return tokens.Select(ParseToken).ToList();
    }

    private static KeyStroke ParseToken(string token)
    {
        var upper = token.ToUpperInvariant();

        if (Named.TryGetValue(upper, out var named))
        {
            return named;
        }

        if (upper.Length == 1)
        {
            var c = upper[0];
            if (c is >= 'A' and <= 'Z' or >= '0' and <= '9')
            {
                return new KeyStroke(c, false);
            }

            if (Punctuation.TryGetValue(c, out var punctuation))
            {
                return new KeyStroke(punctuation, false);
            }
        }

        // F1..F24
        if (upper.Length >= 2 && upper[0] == 'F' && int.TryParse(upper[1..], out var function) && function is >= 1 and <= 24)
        {
            return new KeyStroke((ushort)(0x70 + function - 1), false);
        }

        // NUMPAD0..NUMPAD9
        if (upper.StartsWith("NUMPAD", StringComparison.Ordinal) && int.TryParse(upper["NUMPAD".Length..], out var digit) && digit is >= 0 and <= 9)
        {
            return new KeyStroke((ushort)(0x60 + digit), false);
        }

        throw new InvalidOperationException($"Unsupported hotkey token '{token}'.");
    }

    // Keys that move the cursor or edit text are "extended": without that flag Windows mistakes them
    // for the number pad and, with NumLock off, an arrow key would type a digit.
    private static readonly Dictionary<string, KeyStroke> Named = new(StringComparer.Ordinal)
    {
        ["CTRL"] = new(0x11, false),
        ["CONTROL"] = new(0x11, false),
        ["SHIFT"] = new(0x10, false),
        ["ALT"] = new(0x12, false),
        ["WIN"] = new(0x5B, true),
        ["META"] = new(0x5B, true),
        ["ENTER"] = new(0x0D, false),
        ["RETURN"] = new(0x0D, false),
        ["ESC"] = new(0x1B, false),
        ["ESCAPE"] = new(0x1B, false),
        ["TAB"] = new(0x09, false),
        ["SPACE"] = new(0x20, false),
        ["BACKSPACE"] = new(0x08, false),
        ["CAPSLOCK"] = new(0x14, false),
        ["PRINTSCREEN"] = new(0x2C, true),
        ["PRTSC"] = new(0x2C, true),
        ["`"] = new(0xC0, false),
        ["BACKTICK"] = new(0xC0, false),
        ["OEM_3"] = new(0xC0, false),
        ["PLUS"] = new(0xBB, false),
        ["MINUS"] = new(0xBD, false),
        ["DELETE"] = new(0x2E, true),
        ["DEL"] = new(0x2E, true),
        ["INSERT"] = new(0x2D, true),
        ["INS"] = new(0x2D, true),
        ["HOME"] = new(0x24, true),
        ["END"] = new(0x23, true),
        ["PAGEUP"] = new(0x21, true),
        ["PGUP"] = new(0x21, true),
        ["PAGEDOWN"] = new(0x22, true),
        ["PGDN"] = new(0x22, true),
        ["LEFT"] = new(0x25, true),
        ["UP"] = new(0x26, true),
        ["RIGHT"] = new(0x27, true),
        ["DOWN"] = new(0x28, true),
    };

    private static readonly Dictionary<char, ushort> Punctuation = new()
    {
        ['-'] = 0xBD,
        ['='] = 0xBB,
        ['['] = 0xDB,
        [']'] = 0xDD,
        [';'] = 0xBA,
        ['\''] = 0xDE,
        [','] = 0xBC,
        ['.'] = 0xBE,
        ['/'] = 0xBF,
        ['\\'] = 0xDC,
        ['`'] = 0xC0,
    };
}
