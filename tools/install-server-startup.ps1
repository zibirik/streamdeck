$serverExe = Join-Path $PSScriptRoot "..\server\windows\StreamPanel.Server\bin\Release\net8.0-windows\StreamPanel.Server.exe"
$shortcutPath = Join-Path $env:APPDATA "Microsoft\Windows\Start Menu\Programs\Startup\StreamPanel Server.lnk"

if (-not (Test-Path $serverExe)) {
    Write-Host "Build the server first: dotnet publish server/windows/StreamPanel.Server -c Release"
    exit 1
}

$shell = New-Object -ComObject WScript.Shell
$shortcut = $shell.CreateShortcut($shortcutPath)
$shortcut.TargetPath = $serverExe
$shortcut.WorkingDirectory = Split-Path $serverExe
$shortcut.Save()
Write-Host "Added startup shortcut: $shortcutPath"
