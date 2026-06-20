using System.Diagnostics;

namespace StreamPanel.Server.Infrastructure;

public static class TrayHost
{
    public static void StartIfEnabled(IConfiguration configuration, int port)
    {
        if (!configuration.GetValue("StreamPanel:TrayIcon", true)) return;

        try
        {
            var thread = new Thread(() =>
            {
                using var icon = new System.Windows.Forms.NotifyIcon
                {
                    Icon = System.Drawing.SystemIcons.Application,
                    Visible = true,
                    Text = $"StreamPanel :{port}",
                };
                icon.ContextMenuStrip = new System.Windows.Forms.ContextMenuStrip();
                icon.ContextMenuStrip.Items.Add($"StreamPanel on port {port}", null, (_, _) => { });
                icon.ContextMenuStrip.Items.Add("Open status", null, (_, _) =>
                {
                    Process.Start(new ProcessStartInfo($"http://localhost:{port}/status") { UseShellExecute = true });
                });
                icon.ContextMenuStrip.Items.Add("Exit", null, (_, _) => Environment.Exit(0));
                System.Windows.Forms.Application.Run();
            })
            {
                IsBackground = true,
            };
            thread.SetApartmentState(ApartmentState.STA);
            thread.Start();
        }
        catch
        {
            // Tray is optional; server still runs headless.
        }
    }
}
