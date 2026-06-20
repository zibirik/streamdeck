# StreamPanel Plugin SDK (preview)

Plugins extend the Windows server with custom command handlers.

## Folder layout

```
plugins/
  my-plugin/
    plugin.json
    MyPlugin.dll
```

## plugin.json

```json
{
  "id": "my-plugin",
  "name": "My Plugin",
  "version": "1.0.0",
  "entry": "MyPlugin.dll"
}
```

## IStreamPanelPlugin (C#)

```csharp
public interface IStreamPanelPlugin
{
    string Id { get; }
    Task<PluginResult> HandleAsync(string action, IReadOnlyDictionary<string, string> payload);
}
```

Send from Android macro: `Custom` action with `action=my-plugin:doThing`.

Future: JavaScript plugin host via Node sidecar.
