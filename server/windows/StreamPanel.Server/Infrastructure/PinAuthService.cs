namespace StreamPanel.Server.Infrastructure;

public sealed class PinAuthService(IConfiguration configuration)
{
    private readonly HashSet<string> _authenticated = new(StringComparer.Ordinal);
    private readonly string _pin = configuration.GetValue("StreamPanel:Pin", "") ?? "";

    public bool IsRequired => !string.IsNullOrWhiteSpace(_pin);

    public bool TryAuthenticate(string? pin)
    {
        if (!IsRequired) return true;
        if (string.IsNullOrWhiteSpace(pin)) return false;
        if (!string.Equals(_pin, pin, StringComparison.Ordinal)) return false;
        _authenticated.Add(pin);
        return true;
    }

    public bool IsAuthenticated(string? pin) =>
        !IsRequired || (!string.IsNullOrWhiteSpace(pin) && _authenticated.Contains(pin));
}
