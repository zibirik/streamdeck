using System.Diagnostics;
using System.Net.WebSockets;
using System.Text;
using System.Text.Json;
using StreamPanel.Server.Actions;
using StreamPanel.Server.CommandProtocol;
using StreamPanel.Server.Infrastructure;

namespace StreamPanel.Server.Endpoints;

public static class WebSocketEndpoint
{
    private const int DefaultMaxMessageBytes = 1024 * 1024;

    public static void MapCommandWebSocket(this WebApplication app)
    {
        var maxMessageBytes = app.Configuration.GetValue("StreamPanel:MaxMessageBytes", DefaultMaxMessageBytes);

        app.Map("/ws", async (
            HttpContext context,
            PinAuthService pinAuth,
            ICommandHandler commandHandler,
            JsonSerializerOptions jsonOptions,
            CommandLog commandLog,
            ConnectionTracker connectionTracker,
            ILoggerFactory loggerFactory
        ) =>
        {
            if (!context.WebSockets.IsWebSocketRequest)
            {
                context.Response.StatusCode = StatusCodes.Status400BadRequest;
                return;
            }

            var logger = loggerFactory.CreateLogger("WebSocket");
            var clientKey = ClientAddress.Key(context);
            var session = new ClientSession(pinAuth, commandHandler, jsonOptions, clientKey, ClientAddress.IsLocal(context));

            using var socket = await context.WebSockets.AcceptWebSocketAsync();
            connectionTracker.Connected();
            logger.LogInformation("Tablet connected from {RemoteIp} ({Active} active)", clientKey, connectionTracker.ActiveConnections);

            try
            {
                while (socket.State == WebSocketState.Open && !context.RequestAborted.IsCancellationRequested)
                {
                    var text = await WebSocketMessageReader.ReadTextAsync(socket, maxMessageBytes, context.RequestAborted);
                    if (text is null)
                    {
                        await socket.CloseAsync(WebSocketCloseStatus.NormalClosure, "Closed by client", CancellationToken.None);
                        break;
                    }

                    var stopwatch = Stopwatch.StartNew();
                    var (command, response) = await session.ProcessAsync(text, context.RequestAborted);
                    stopwatch.Stop();

                    if (command is not null)
                    {
                        commandLog.Add(command, response);
                        logger.LogInformation(
                            "Command {Type} {Id} => {Ok} ({ElapsedMs}ms): {Message}",
                            command.Type,
                            command.Id,
                            response.Ok,
                            stopwatch.ElapsedMilliseconds,
                            response.Message);
                    }

                    var bytes = Encoding.UTF8.GetBytes(JsonSerializer.Serialize(response, jsonOptions));
                    await socket.SendAsync(new ArraySegment<byte>(bytes), WebSocketMessageType.Text, true, context.RequestAborted);
                }
            }
            catch (InvalidDataException exception)
            {
                logger.LogWarning("Dropping {RemoteIp}: {Reason}", clientKey, exception.Message);
                await TryCloseAsync(socket, WebSocketCloseStatus.MessageTooBig, "Message too large");
            }
            catch (Exception exception) when (exception is WebSocketException or OperationCanceledException)
            {
                logger.LogDebug(exception, "WebSocket for {RemoteIp} ended", clientKey);
            }
            finally
            {
                connectionTracker.Disconnected();
                logger.LogInformation("Tablet disconnected ({Active} active)", connectionTracker.ActiveConnections);
            }
        });
    }

    private static async Task TryCloseAsync(WebSocket socket, WebSocketCloseStatus status, string reason)
    {
        try
        {
            if (socket.State is WebSocketState.Open or WebSocketState.CloseReceived)
            {
                await socket.CloseAsync(status, reason, CancellationToken.None);
            }
        }
        catch (WebSocketException)
        {
            // Peer already gone.
        }
    }
}
