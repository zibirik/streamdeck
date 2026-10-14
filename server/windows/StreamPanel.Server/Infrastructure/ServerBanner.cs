using System.Net;
using System.Net.Sockets;

namespace StreamPanel.Server.Infrastructure;

public static class ServerBanner
{
    public static void Print(int port, PinBootstrapResult pin)
    {
        Console.WriteLine();
        Console.WriteLine("  ╔══════════════════════════════════════╗");
        Console.WriteLine("  ║         StreamPanel Server           ║");
        Console.WriteLine("  ╚══════════════════════════════════════╝");
        Console.WriteLine();
        Console.WriteLine($"  Version : {AppVersion.Current}");
        Console.WriteLine($"  Machine : {Environment.MachineName}");
        Console.WriteLine($"  Port    : {port}");
        Console.WriteLine();
        Console.WriteLine("  Connect tablet to:");

        var printed = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        foreach (var address in GetLocalIPv4())
        {
            if (!printed.Add(address)) continue;
            Console.WriteLine($"    ws://{address}:{port}/ws");
        }

        Console.WriteLine($"    ws://127.0.0.1:{port}/ws  (this PC only)");
        Console.WriteLine();
        Console.WriteLine($"  Status  : http://localhost:{port}/status");
        Console.WriteLine();
        switch (pin.Source)
        {
            case PinSource.Generated:
                Console.WriteLine($"  PIN     : {pin.Pin}   <- type this into the app (Settings > My PC)");
                Console.WriteLine("            It was made for you on first start and is remembered.");
                Console.WriteLine($"            To choose your own, write it in {UserData.SettingsFilePath}");
                break;
            case PinSource.Configured:
                Console.WriteLine("  Security: your own PIN is required for remote clients.");
                break;
            default:
                Console.WriteLine("  WARNING : the PIN is switched off - anyone on this network can control this PC.");
                Console.WriteLine("            Remove the line with Pin = off from your settings to get a PIN again.");
                break;
        }

        Console.WriteLine("  Press Ctrl+C to stop.");
        Console.WriteLine();
    }

    private static IEnumerable<string> GetLocalIPv4()
    {
        foreach (var networkInterface in System.Net.NetworkInformation.NetworkInterface.GetAllNetworkInterfaces())
        {
            if (networkInterface.OperationalStatus != System.Net.NetworkInformation.OperationalStatus.Up) continue;
            if (networkInterface.NetworkInterfaceType == System.Net.NetworkInformation.NetworkInterfaceType.Loopback) continue;

            foreach (var address in networkInterface.GetIPProperties().UnicastAddresses)
            {
                if (address.Address.AddressFamily != AddressFamily.InterNetwork) continue;
                var ip = address.Address.ToString();
                if (IPAddress.IsLoopback(address.Address)) continue;
                yield return ip;
            }
        }
    }
}
