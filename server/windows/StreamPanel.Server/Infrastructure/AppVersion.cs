using System.Reflection;

namespace StreamPanel.Server.Infrastructure;

public static class AppVersion
{
    /// <summary>Semantic version of this build (set with <c>-p:Version=</c> when publishing), without the commit suffix.</summary>
    public static string Current { get; } = Resolve();

    private static string Resolve()
    {
        var assembly = typeof(AppVersion).Assembly;
        var informational = assembly.GetCustomAttribute<AssemblyInformationalVersionAttribute>()?.InformationalVersion;
        if (string.IsNullOrWhiteSpace(informational))
        {
            return assembly.GetName().Version?.ToString(3) ?? "0.0.0";
        }

        var plus = informational.IndexOf('+');
        return plus >= 0 ? informational[..plus] : informational;
    }
}
