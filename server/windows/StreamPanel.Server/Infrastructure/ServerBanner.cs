using System.Net;
using System.Net.Sockets;

namespace StreamPanel.Server.Infrastructure;

public static class ServerBanner
{
    public static void Print(int port, bool pinRequired)
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
        if (pinRequired)
        {
            Console.WriteLine("  Security: PIN is required for remote clients.");
        }
        else
        {
            Console.WriteLine("  WARNING : no PIN is set - anyone on this network can control this PC.");
            Console.WriteLine("            Set StreamPanel:Pin in appsettings.json and the same PIN on the tablet.");
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
