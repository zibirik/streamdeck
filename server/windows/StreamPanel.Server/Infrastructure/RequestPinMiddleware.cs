using System.Net;

namespace StreamPanel.Server.Infrastructure;

public static class ClientAddress
{
    /// <summary>True when the request comes from this PC itself (loopback or the PC's own LAN address).</summary>
    public static bool IsLocal(HttpContext context)
    {
        var remote = context.Connection.RemoteIpAddress;
        if (remote is null) return false;
        if (remote.IsIPv4MappedToIPv6) remote = remote.MapToIPv4();
        if (IPAddress.IsLoopback(remote)) return true;

        var local = context.Connection.LocalIpAddress;
        if (local is null) return false;
        if (local.IsIPv4MappedToIPv6) local = local.MapToIPv4();
        return remote.Equals(local);
    }

    public static string Key(HttpContext context)
    {
        var remote = context.Connection.RemoteIpAddress;
        if (remote is null) return "unknown";
        if (remote.IsIPv4MappedToIPv6) remote = remote.MapToIPv4();
        return remote.ToString();
    }
}

/// <summary>
/// Protects the HTTP API (<c>/status</c>, <c>/api/configurator/*</c>, <c>/integrations/*</c>) with the server PIN.
/// The PIN is read from the <c>X-StreamPanel-Pin</c> header or the <c>pin</c> query parameter.
/// Requests from the PC itself are always allowed. When no PIN is configured everything is open.
/// </summary>
public static class RequestPinMiddleware
{
    public const string HeaderName = "X-StreamPanel-Pin";

    private static readonly string[] ProtectedPrefixes = ["/status", "/api/configurator", "/integrations", "/gsi"];

    public static IApplicationBuilder UseRequestPin(this IApplicationBuilder app) =>
        app.Use(async (context, next) =>
        {
            // Counter-Strike posts game state and cannot send headers or a query PIN. The payload is HUD
            // data only — it cannot run commands — so those two POST paths stay open.
            if (IsGsiPost(context) || !IsProtected(context.Request.Path))
            {
                await next();
                return;
            }

            var pinAuth = context.RequestServices.GetRequiredService<PinAuthService>();
            if (!pinAuth.IsRequired || ClientAddress.IsLocal(context))
            {
                await next();
                return;
            }

            var pin = context.Request.Headers[HeaderName].FirstOrDefault()
                      ?? context.Request.Query["pin"].FirstOrDefault();
            var clientKey = ClientAddress.Key(context);

            switch (pinAuth.Authenticate(pin, clientKey))
            {
                case PinAuthResult.Success:
                    await next();
                    break;
                case PinAuthResult.LockedOut:
                    context.Response.StatusCode = StatusCodes.Status429TooManyRequests;
                    context.Response.Headers.RetryAfter =
                        ((int)Math.Ceiling(pinAuth.RemainingLockout(clientKey).TotalSeconds)).ToString();
                    await context.Response.WriteAsJsonAsync(new { ok = false, error = "locked_out" });
                    break;
                default:
                    context.Response.StatusCode = StatusCodes.Status401Unauthorized;
                    await context.Response.WriteAsJsonAsync(new { ok = false, error = "pin_required" });
                    break;
            }
        });

    private static bool IsProtected(PathString path) =>
        ProtectedPrefixes.Any(prefix => path.StartsWithSegments(prefix, StringComparison.OrdinalIgnoreCase));

    public static bool IsGsiPost(HttpContext context) =>
        HttpMethods.IsPost(context.Request.Method) && (
            context.Request.Path.Equals("/gsi", StringComparison.OrdinalIgnoreCase) ||
            context.Request.Path.Equals("/integrations/cs2/gsi", StringComparison.OrdinalIgnoreCase));
}
