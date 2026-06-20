namespace StreamPanel.Server.Windows;

public static class PathExpander
{
    public static string Expand(string value) =>
        Environment.ExpandEnvironmentVariables(value.Trim().Trim('"'));
}
