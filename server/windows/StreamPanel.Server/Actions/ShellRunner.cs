using System.Diagnostics;
using System.Globalization;
using System.Text;

namespace StreamPanel.Server.Actions;

public sealed record ShellResult(bool Started, int? ExitCode, bool TimedOut, string Output)
{
    public bool Succeeded => Started && !TimedOut && ExitCode == 0;
}

/// <summary>
/// Runs a program on the PC and hands back what it printed, so the tablet can show the answer of
/// <c>git status</c> or <c>docker ps</c> instead of a cheerful "sent" that hides every error.
/// </summary>
public static class ShellRunner
{
    /// <summary>The tablet shows this on a small screen; the end of the output is what matters.</summary>
    public const int MaxOutputChars = 3000;

    public static ShellResult Run(string fileName, string arguments, string? workingDirectory, TimeSpan timeout)
    {
        var startInfo = new ProcessStartInfo(fileName, arguments)
        {
            UseShellExecute = false,
            CreateNoWindow = true,
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            RedirectStandardInput = true,
        };
        if (!string.IsNullOrWhiteSpace(workingDirectory))
        {
            startInfo.WorkingDirectory = workingDirectory;
        }

        Process process;
        try
        {
            process = Process.Start(startInfo) ?? throw new InvalidOperationException($"Could not start {fileName}.");
        }
        catch (Exception exception) when (exception is System.ComponentModel.Win32Exception or InvalidOperationException or DirectoryNotFoundException)
        {
            var hint = exception is System.ComponentModel.Win32Exception
                ? $"{fileName} was not found on this PC (is it installed and on PATH?)."
                : exception.Message;
            return new ShellResult(false, null, false, hint);
        }

        using (process)
        {
            // Nobody can answer a prompt here: close input so tools never wait for it.
            try { process.StandardInput.Close(); } catch (IOException) { /* already closed */ }

            var output = ReadAll(process.StandardOutput.BaseStream);
            var error = ReadAll(process.StandardError.BaseStream);

            var finished = process.WaitForExit(timeout);
            if (!finished)
            {
                try { process.Kill(entireProcessTree: true); } catch (InvalidOperationException) { /* just ended */ }
            }

            // Let the pipes drain, but never hang on a stray child process that keeps them open.
            Task.WaitAll([output, error], TimeSpan.FromSeconds(3));
            var text = Decode(Collect(output), Collect(error));
            return finished
                ? new ShellResult(true, process.ExitCode, false, Tidy(text))
                : new ShellResult(true, null, true, Tidy(text));
        }
    }

    private static Task<byte[]> ReadAll(Stream stream) => Task.Run(() =>
    {
        using var buffer = new MemoryStream();
        try { stream.CopyTo(buffer); } catch (IOException) { /* the process was killed */ }
        return buffer.ToArray();
    });

    private static byte[] Collect(Task<byte[]> read) => read.IsCompletedSuccessfully ? read.Result : [];

    private static string Decode(byte[] output, byte[] error)
    {
        var text = DecodeBytes(output);
        var errorText = DecodeBytes(error);
        return string.IsNullOrWhiteSpace(errorText) ? text : string.IsNullOrWhiteSpace(text) ? errorText : $"{text}\n{errorText}";
    }

    /// <summary>Modern tools print UTF-8; old Windows tools (ipconfig) print the console code page. Try UTF-8 first.</summary>
    private static string DecodeBytes(byte[] bytes)
    {
        if (bytes.Length == 0) return string.Empty;
        try
        {
            return new UTF8Encoding(false, throwOnInvalidBytes: true).GetString(bytes);
        }
        catch (DecoderFallbackException)
        {
            Encoding.RegisterProvider(CodePagesEncodingProvider.Instance);
            try
            {
                return Encoding.GetEncoding(CultureInfo.CurrentCulture.TextInfo.OEMCodePage).GetString(bytes);
            }
            catch (ArgumentException)
            {
                return Encoding.Latin1.GetString(bytes);
            }
        }
    }

    public static string Tidy(string text)
    {
        var cleaned = text.Replace("\r\n", "\n").Trim();
        return cleaned.Length <= MaxOutputChars ? cleaned : "…" + cleaned[^MaxOutputChars..];
    }
}
