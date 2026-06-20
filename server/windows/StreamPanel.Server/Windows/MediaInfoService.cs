namespace StreamPanel.Server.Windows;

public interface IMediaInfoService
{
    (string? Title, string? Artist) GetNowPlaying();
    bool? IsMediaPlaying();
}

public sealed class MediaInfoService : IMediaInfoService
{
    public (string? Title, string? Artist) GetNowPlaying()
    {
        try
        {
            var managerType = Type.GetType("Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows, ContentType=WindowsRuntime");
            if (managerType is null) return (null, null);

            var requestAsync = managerType.GetMethod("RequestAsync");
            if (requestAsync is null) return (null, null);

            var task = requestAsync.Invoke(null, null);
            if (task is null) return (null, null);

            task.GetType().GetMethod("GetAwaiter")?.Invoke(task, null);
            var resultProperty = task.GetType().GetProperty("Result");
            var manager = resultProperty?.GetValue(task);
            if (manager is null) return (null, null);

            var getSession = manager.GetType().GetMethod("GetCurrentSession");
            var session = getSession?.Invoke(manager, null);
            if (session is null) return (null, null);

            var mediaPropertiesAsync = session.GetType().GetMethod("TryGetMediaPropertiesAsync");
            var mediaTask = mediaPropertiesAsync?.Invoke(session, null);
            if (mediaTask is null) return (null, null);

            mediaTask.GetType().GetMethod("GetAwaiter")?.Invoke(mediaTask, null);
            var mediaResult = mediaTask.GetType().GetProperty("Result")?.GetValue(mediaTask);
            if (mediaResult is null) return (null, null);

            var title = mediaResult.GetType().GetProperty("Title")?.GetValue(mediaResult)?.ToString();
            var artist = mediaResult.GetType().GetProperty("Artist")?.GetValue(mediaResult)?.ToString();
            return (title, artist);
        }
        catch
        {
            return (null, null);
        }
    }

    public bool? IsMediaPlaying()
    {
        try
        {
            var managerType = Type.GetType("Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows, ContentType=WindowsRuntime");
            if (managerType is null) return null;

            var requestAsync = managerType.GetMethod("RequestAsync");
            var task = requestAsync?.Invoke(null, null);
            if (task is null) return null;

            task.GetType().GetMethod("GetAwaiter")?.Invoke(task, null);
            var manager = task.GetType().GetProperty("Result")?.GetValue(task);
            if (manager is null) return null;

            var getSession = manager.GetType().GetMethod("GetCurrentSession");
            var session = getSession?.Invoke(manager, null);
            if (session is null) return null;

            var getPlaybackInfo = session.GetType().GetMethod("GetPlaybackInfo");
            var playbackInfo = getPlaybackInfo?.Invoke(session, null);
            if (playbackInfo is null) return null;

            var status = playbackInfo.GetType().GetProperty("PlaybackStatus")?.GetValue(playbackInfo);
            if (status is null) return null;

            // Windows.Media.PlaybackStatus: Playing = 4
            return Convert.ToInt32(status) == 4;
        }
        catch
        {
            return null;
        }
    }
}
