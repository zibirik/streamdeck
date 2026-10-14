using System.Collections.Concurrent;
using System.Security.Cryptography;
using System.Text;

namespace StreamPanel.Server.Infrastructure;

public enum PinAuthResult
{
    Success,
    InvalidPin,
    LockedOut
}

/// <summary>
/// Validates the optional server PIN. Comparison is constant-time and repeated failures from
/// the same client are rate limited to make brute-forcing a short PIN impractical.
/// Authentication state is NOT stored here: it lives on the connection (see <see cref="ClientSession"/>)
/// or is supplied per HTTP request.
/// </summary>
public sealed class PinAuthService
{
    public const int MaxFailedAttempts = 5;
    public static readonly TimeSpan LockoutDuration = TimeSpan.FromSeconds(60);
    private const int MaxTrackedClients = 1024;

    private readonly byte[]? _pinHash;
    private readonly TimeProvider _timeProvider;
    private readonly ConcurrentDictionary<string, FailureState> _failures = new(StringComparer.Ordinal);

    private readonly string? _pin;

    public PinAuthService(IConfiguration configuration, TimeProvider timeProvider)
    {
        _timeProvider = timeProvider;
        var pin = configuration.GetValue("StreamPanel:Pin", "") ?? "";
        _pin = string.IsNullOrWhiteSpace(pin) ? null : pin;
        _pinHash = _pin is null ? null : Hash(_pin);
    }

    public bool IsRequired => _pinHash is not null;

    /// <summary>
    /// Only for generating client configs served to already authorised callers
    /// (the CS2 GSI file has to carry the PIN because CS2 cannot send custom headers).
    /// </summary>
    public string? PinForGeneratedConfig => _pin;

    /// <summary>Checks the PIN for the given client (usually its IP address).</summary>
    public PinAuthResult Authenticate(string? pin, string clientKey)
    {
        if (_pinHash is not { } expected) return PinAuthResult.Success;

        var now = _timeProvider.GetUtcNow();
        if (_failures.TryGetValue(clientKey, out var existing) && existing.LockedUntil > now)
        {
            return PinAuthResult.LockedOut;
        }

        if (!string.IsNullOrEmpty(pin) && CryptographicOperations.FixedTimeEquals(expected, Hash(pin)))
        {
            _failures.TryRemove(clientKey, out _);
            return PinAuthResult.Success;
        }

        RegisterFailure(clientKey, now);
        return _failures.TryGetValue(clientKey, out var updated) && updated.LockedUntil > now
            ? PinAuthResult.LockedOut
            : PinAuthResult.InvalidPin;
    }

    public TimeSpan RemainingLockout(string clientKey)
    {
        if (!_failures.TryGetValue(clientKey, out var state)) return TimeSpan.Zero;
        var remaining = state.LockedUntil - _timeProvider.GetUtcNow();
        return remaining > TimeSpan.Zero ? remaining : TimeSpan.Zero;
    }

    private void RegisterFailure(string clientKey, DateTimeOffset now)
    {
        if (_failures.Count >= MaxTrackedClients)
        {
            foreach (var pair in _failures)
            {
                if (pair.Value.LockedUntil <= now) _failures.TryRemove(pair.Key, out _);
            }
        }

        _failures.AddOrUpdate(
            clientKey,
            _ => new FailureState(1, DateTimeOffset.MinValue),
            (_, current) =>
            {
                // A lockout that already expired starts a fresh series of attempts.
                var count = current.LockedUntil != DateTimeOffset.MinValue && current.LockedUntil <= now
                    ? 1
                    : current.Count + 1;
                return count >= MaxFailedAttempts
                    ? new FailureState(0, now + LockoutDuration)
                    : new FailureState(count, DateTimeOffset.MinValue);
            });
    }

    private static byte[] Hash(string value) => SHA256.HashData(Encoding.UTF8.GetBytes(value));

    private readonly record struct FailureState(int Count, DateTimeOffset LockedUntil);
}
