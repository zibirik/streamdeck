$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
dotnet build "$repoRoot\server\windows\StreamPanel.Server\StreamPanel.Server.csproj" -c Release
