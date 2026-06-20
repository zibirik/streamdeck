using System.Net.NetworkInformation;

namespace StreamPanel.Server.Windows;

public interface INetworkStatsService
{
    double? DownloadMbps { get; }
    double? UploadMbps { get; }
    string? ActiveInterface { get; }
    void Refresh();
}

public sealed class NetworkStatsService : INetworkStatsService
{
    private long _lastBytesReceived;
    private long _lastBytesSent;
    private DateTime _lastSample = DateTime.UtcNow;
    private double? _downloadMbps;
    private double? _uploadMbps;
    private string? _activeInterface;

    public double? DownloadMbps => _downloadMbps;
    public double? UploadMbps => _uploadMbps;
    public string? ActiveInterface => _activeInterface;

    public void Refresh()
    {
        try
        {
            var now = DateTime.UtcNow;
            var elapsed = (now - _lastSample).TotalSeconds;
            if (elapsed < 0.5) return;

            var primary = NetworkInterface.GetAllNetworkInterfaces()
                .Where(IsUsableInterface)
                .OrderByDescending(nic => nic.NetworkInterfaceType is NetworkInterfaceType.Wireless80211 or NetworkInterfaceType.Ethernet)
                .ThenBy(nic => nic.Name.Contains("virtual", StringComparison.OrdinalIgnoreCase))
                .ThenBy(nic => nic.Name.Contains("vpn", StringComparison.OrdinalIgnoreCase))
                .FirstOrDefault();

            if (primary == null)
            {
                _downloadMbps = null;
                _uploadMbps = null;
                _activeInterface = null;
                return;
            }

            var stats = primary.GetIPv4Statistics();
            var received = stats.BytesReceived;
            var sent = stats.BytesSent;
            var previousInterface = _activeInterface;
            _activeInterface = primary.Name;

            if (!string.Equals(previousInterface, primary.Name, StringComparison.Ordinal))
            {
                _lastBytesReceived = received;
                _lastBytesSent = sent;
                _lastSample = now;
                _downloadMbps = 0;
                _uploadMbps = 0;
                return;
            }

            if (_lastSample != default && elapsed > 0)
            {
                var downDelta = Math.Max(0, received - _lastBytesReceived);
                var upDelta = Math.Max(0, sent - _lastBytesSent);
                _downloadMbps = Math.Round(downDelta * 8 / elapsed / 1_000_000, 2);
                _uploadMbps = Math.Round(upDelta * 8 / elapsed / 1_000_000, 2);
            }

            _lastBytesReceived = received;
            _lastBytesSent = sent;
            _lastSample = now;
        }
        catch
        {
            _downloadMbps = null;
            _uploadMbps = null;
        }
    }

    private static bool IsUsableInterface(NetworkInterface nic)
    {
        if (nic.OperationalStatus != OperationalStatus.Up) return false;
        if (nic.NetworkInterfaceType is NetworkInterfaceType.Loopback or NetworkInterfaceType.Tunnel) return false;
        if (nic.Name.Contains("happ", StringComparison.OrdinalIgnoreCase)) return false;
        if (nic.Description.Contains("happ", StringComparison.OrdinalIgnoreCase)) return false;
        return nic.GetIPProperties().GatewayAddresses.Any();
    }
}
