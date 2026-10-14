using StreamPanel.Server.Actions;

namespace StreamPanel.Server.Tests;

public class ShellRunnerTests
{
    private static readonly TimeSpan Plenty = TimeSpan.FromSeconds(20);

    [Fact]
    public void ReportsWhatTheToolPrinted()
    {
        var result = ShellRunner.Run("cmd.exe", "/c echo hello from the pc", null, Plenty);

        Assert.True(result.Succeeded);
        Assert.Equal("hello from the pc", result.Output);
    }

    [Fact]
    public void NonZeroExitCode_IsAFailure_WithTheErrorText()
    {
        var result = ShellRunner.Run("cmd.exe", "/c (echo something went wrong 1>&2) & exit /b 3", null, Plenty);

        Assert.True(result.Started);
        Assert.False(result.Succeeded);
        Assert.Equal(3, result.ExitCode);
        Assert.Contains("something went wrong", result.Output);
    }

    [Fact]
    public void MissingProgram_SaysItWasNotFound_InsteadOfPretendingToWork()
    {
        var result = ShellRunner.Run("definitely-not-a-real-program.exe", "", null, Plenty);

        Assert.False(result.Started);
        Assert.False(result.Succeeded);
        Assert.Contains("not found", result.Output, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public void SlowProgram_IsStoppedAfterTheTimeout()
    {
        var result = ShellRunner.Run("cmd.exe", "/c ping -n 30 127.0.0.1 >nul", null, TimeSpan.FromMilliseconds(700));

        Assert.True(result.TimedOut);
        Assert.False(result.Succeeded);
    }

    [Fact]
    public void WorkingDirectory_IsHonoured()
    {
        var directory = Path.GetTempPath().TrimEnd(Path.DirectorySeparatorChar);

        var result = ShellRunner.Run("cmd.exe", "/c cd", directory, Plenty);

        Assert.True(result.Succeeded);
        Assert.Equal(directory, result.Output, ignoreCase: true);
    }

    [Fact]
    public void ProgramWaitingForInput_DoesNotHang()
    {
        // `more` reads stdin; with input closed it ends at once instead of waiting for a person.
        var result = ShellRunner.Run("cmd.exe", "/c more", null, Plenty);

        Assert.False(result.TimedOut);
    }

    [Fact]
    public void LongOutput_KeepsTheEnd()
    {
        var text = string.Concat(Enumerable.Range(0, 2000).Select(i => $"line {i}\n"));

        var tidy = ShellRunner.Tidy(text);

        Assert.True(tidy.Length <= ShellRunner.MaxOutputChars + 1);
        Assert.EndsWith("line 1999", tidy);
        Assert.StartsWith("…", tidy);
    }
}
