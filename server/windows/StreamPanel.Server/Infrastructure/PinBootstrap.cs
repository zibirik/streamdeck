using System.Security.Cryptography;

namespace StreamPanel.Server.Infrastructure;

/// <summary>Where the server keeps what belongs to this PC (the generated PIN, the user's own settings file).</summary>
public static class UserData
{
    public const string SettingsFileName = "appsettings.user.json";

    public static string Directory { get; } = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "StreamPanel");

    public static string SettingsFilePath => Path.Combine(Directory, SettingsFileName);
}

public sealed record PinBootstrapResult(string? Pin, PinSource Source);

public enum PinSource
{
    /// <summary>The user wrote a PIN in a settings file.</summary>
    Configured,

    /// <summary>The server made one up the first time it ran and remembered it.</summary>
    Generated,

    /// <summary>The user explicitly asked for no PIN (<c>"Pin": "off"</c>).</summary>
    Disabled,
}

/// <summary>
/// Makes the server safe by default. Without a PIN anybody on the Wi-Fi could control the PC, so when the
/// user has not chosen one we generate a random 6-digit PIN once, remember it and show it on the PC.
/// Writing <c>"Pin": "off"</c> in the settings turns the PIN off on purpose.
/// </summary>
public static class PinBootstrap
{
    public const string FileName = "pin.txt";
    public const int Length = 6;

    public static PinBootstrapResult Resolve(string? configuredPin, string storeDirectory)
    {
        var configured = configuredPin?.Trim();
        if (string.Equals(configured, "off", StringComparison.OrdinalIgnoreCase) ||
            string.Equals(configured, "none", StringComparison.OrdinalIgnoreCase))
        {
            return new PinBootstrapResult(null, PinSource.Disabled);
        }

        if (!string.IsNullOrEmpty(configured))
        {
            return new PinBootstrapResult(configured, PinSource.Configured);
        }

        var path = Path.Combine(storeDirectory, FileName);
        try
        {
            if (File.Exists(path))
            {
                var saved = File.ReadAllText(path).Trim();
                if (saved.Length > 0)
                {
                    return new PinBootstrapResult(saved, PinSource.Generated);
                }
            }

            var pin = Generate();
            System.IO.Directory.CreateDirectory(storeDirectory);
            File.WriteAllText(path, pin);
            return new PinBootstrapResult(pin, PinSource.Generated);
        }
        catch (Exception exception) when (exception is IOException or UnauthorizedAccessException)
        {
            // Cannot remember it: a PIN that lasts until the server stops is still far better than none.
            return new PinBootstrapResult(Generate(), PinSource.Generated);
        }
    }

    public static string Generate()
    {
        var digits = new char[Length];
        for (var i = 0; i < digits.Length; i++)
        {
            digits[i] = (char)('0' + RandomNumberGenerator.GetInt32(10));
        }

        return new string(digits);
    }
}
