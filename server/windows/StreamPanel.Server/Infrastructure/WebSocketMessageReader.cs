using System.Net.WebSockets;
using System.Text;

namespace StreamPanel.Server.Infrastructure;

public static class WebSocketMessageReader
{
    /// <summary>
    /// Reads one complete text message, joining fragments. Returns <c>null</c> when the client closed the
    /// connection. Throws <see cref="InvalidDataException"/> when the message exceeds <paramref name="maxBytes"/>.
    /// </summary>
    public static async Task<string?> ReadTextAsync(WebSocket socket, int maxBytes, CancellationToken cancellationToken)
    {
        var buffer = new byte[8 * 1024];
        using var message = new MemoryStream();

        while (true)
        {
            var result = await socket.ReceiveAsync(new ArraySegment<byte>(buffer), cancellationToken);
            if (result.MessageType == WebSocketMessageType.Close)
            {
                return null;
            }

            if (message.Length + result.Count > maxBytes)
            {
                throw new InvalidDataException($"Message is larger than {maxBytes} bytes.");
            }

            message.Write(buffer, 0, result.Count);

            if (result.EndOfMessage)
            {
                return result.MessageType == WebSocketMessageType.Text
                    ? Encoding.UTF8.GetString(message.GetBuffer(), 0, (int)message.Length)
                    : string.Empty;
            }
        }
    }
}
