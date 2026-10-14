using Windows.Media.Control;

namespace StreamPanel.Server.Windows;

public interface IMediaInfoService
{
    (string? Title, string? Artist) GetNowPlaying();
    bool? IsMediaPlaying();
}

/// <summary>
/// What is playing on this PC right now, from Windows' own media session list (the same one the volume flyout uses):
/// Spotify, YouTube in a browser, VLC, the Windows media player and so on all show up here.
/// </summary>
public sealed class MediaInfoService : IMediaInfoService
{
    private static readonly TimeSpan CacheFor = TimeSpan.FromMilliseconds(800);

    private readonly object _gate = new();
    private GlobalSystemMediaTransportControlsSessionManager? _manager;
    private Snapshot _cached = new(null, null, null, DateTimeOffset.MinValue);

    public (string? Title, string? Artist) GetNowPlaying()
    {
        var snapshot = Read();
        return (snapshot.Title, snapshot.Artist);
    }

    public bool? IsMediaPlaying() => Read().Playing;

    private Snapshot Read()
    {
        lock (_gate)
        {
            // The tablet asks for both the title and the state in one status poll; look only once.
            if (DateTimeOffset.UtcNow - _cached.At < CacheFor)
            {
                return _cached;
            }

            _cached = Capture();
            return _cached;
        }
    }

    private Snapshot Capture()
    {
        var now = DateTimeOffset.UtcNow;
        try
        {
            var session = PickSession();
            if (session is null)
            {
                return new Snapshot(null, null, null, now);
            }

            var playing = session.GetPlaybackInfo().PlaybackStatus ==
                          GlobalSystemMediaTransportControlsSessionPlaybackStatus.Playing;
            var properties = session.TryGetMediaPropertiesAsync().AsTask().GetAwaiter().GetResult();
            return new Snapshot(Clean(properties.Title), Clean(properties.Artist), playing, now);
        }
        catch (Exception exception) when (exception is InvalidOperationException or System.Runtime.InteropServices.COMException or UnauthorizedAccessException)
        {
            // Media sessions come and go; "nothing playing" is the right answer when Windows cannot say.
            _manager = null;
            return new Snapshot(null, null, null, now);
        }
    }

    /// <summary>A session that is actually playing beats the one Windows calls "current".</summary>
    private GlobalSystemMediaTransportControlsSession? PickSession()
    {
        _manager ??= GlobalSystemMediaTransportControlsSessionManager.RequestAsync().AsTask().GetAwaiter().GetResult();

        var sessions = _manager.GetSessions();
        foreach (var candidate in sessions)
        {
            if (candidate.GetPlaybackInfo().PlaybackStatus == GlobalSystemMediaTransportControlsSessionPlaybackStatus.Playing)
            {
                return candidate;
            }
        }

        return _manager.GetCurrentSession() ?? sessions.FirstOrDefault();
    }

    private static string? Clean(string? value) => string.IsNullOrWhiteSpace(value) ? null : value.Trim();

    private sealed record Snapshot(string? Title, string? Artist, bool? Playing, DateTimeOffset At);
}
