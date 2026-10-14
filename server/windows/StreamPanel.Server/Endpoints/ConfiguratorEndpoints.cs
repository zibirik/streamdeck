using System.Text;
using System.Text.Json;

namespace StreamPanel.Server.Endpoints;

public static class ConfiguratorEndpoints
{
    private const string DefaultDraft = """
        {
          "version": 1,
          "buttons": [],
          "macros": [],
          "windowPresets": [],
          "layout": {
            "phoneTabs": ["Deck", "Tools", "Menu"],
            "panels": ["Hardware", "StreamChat", "GameStatus", "Media", "QuickActions"]
          }
        }
        """;

    public static void MapConfiguratorEndpoints(this WebApplication app)
    {
        var draftPath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "StreamPanel",
            "web-configurator-draft.json");

        app.MapGet("/", () => Results.Content(ReadHtml(app.Environment), "text/html; charset=utf-8"));
        app.MapGet("/configurator", () => Results.Content(ReadHtml(app.Environment), "text/html; charset=utf-8"));

        app.MapGet("/api/configurator/draft", () => Results.Text(
            File.Exists(draftPath) ? File.ReadAllText(draftPath) : DefaultDraft,
            "application/json; charset=utf-8"));

        app.MapPost("/api/configurator/draft", async (HttpContext context) =>
        {
            // Kestrel already caps the request body (see Program.cs), so this read is bounded.
            using var reader = new StreamReader(context.Request.Body, Encoding.UTF8);
            var raw = await reader.ReadToEndAsync(context.RequestAborted);

            try
            {
                using var _ = JsonDocument.Parse(raw);
            }
            catch (JsonException)
            {
                return Results.BadRequest(new { ok = false, error = "invalid_json" });
            }

            Directory.CreateDirectory(Path.GetDirectoryName(draftPath)!);
            var tempPath = draftPath + ".tmp";
            await File.WriteAllTextAsync(tempPath, raw, Encoding.UTF8, context.RequestAborted);
            File.Move(tempPath, draftPath, overwrite: true);
            return Results.Ok(new { ok = true });
        });
    }

    private static string ReadHtml(IWebHostEnvironment environment)
    {
        var path = Path.Combine(environment.WebRootPath ?? Path.Combine(AppContext.BaseDirectory, "wwwroot"), "index.html");
        return File.Exists(path)
            ? File.ReadAllText(path)
            : """
              <!doctype html>
              <html><head><title>StreamPanel</title></head>
              <body style="font-family:system-ui;background:#050508;color:white">
              <h1>StreamPanel Configurator</h1>
              <p>index.html was not found in wwwroot.</p>
              </body></html>
              """;
    }
}
