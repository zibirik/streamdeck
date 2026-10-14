namespace StreamPanel.Server.Actions;

/// <summary>
/// OPEN_URL is handed to the shell, so anything that is not a plain web/app-deep-link must be refused
/// (otherwise "C:\Windows\System32\cmd.exe" or "file:///..." would run through the URL action).
/// </summary>
public sealed class UrlGuard(IEnumerable<string> allowedSchemes)
{
    public static readonly string[] DefaultSchemes =
    [
        "http", "https", "mailto", "steam", "spotify", "discord", "ms-settings", "tg", "slack", "zoommtg"
    ];

    private readonly HashSet<string> _allowed = new(allowedSchemes, StringComparer.OrdinalIgnoreCase);

    public static UrlGuard FromConfiguration(IConfiguration configuration)
    {
        var configured = configuration.GetSection("StreamPanel:AllowedUrlSchemes").Get<string[]>();
        return new UrlGuard(configured is { Length: > 0 } ? configured : DefaultSchemes);
    }

    public bool TryValidate(string? value, out string url, out string error)
    {
        url = string.Empty;
        error = string.Empty;

        if (string.IsNullOrWhiteSpace(value))
        {
            error = "URL is empty.";
            return false;
        }

        var trimmed = value.Trim();
        if (!Uri.TryCreate(trimmed, UriKind.Absolute, out var uri))
        {
            error = $"'{trimmed}' is not a valid absolute URL.";
            return false;
        }

        if (!_allowed.Contains(uri.Scheme))
        {
            error = $"URL scheme '{uri.Scheme}' is not allowed.";
            return false;
        }

        url = trimmed;
        return true;
    }
}
