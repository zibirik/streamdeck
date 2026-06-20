param(
    [int]$Port = 17820
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$env:StreamPanel__Port = $Port

$dotnet = Get-Command dotnet -ErrorAction SilentlyContinue
if (-not $dotnet) {
    $fallback = "C:\Program Files\dotnet\dotnet.exe"
    if (Test-Path $fallback) {
        $dotnet = Get-Command $fallback
    } else {
        throw "dotnet SDK not found. Install .NET 8 SDK first."
    }
}

Write-Host "Building StreamPanel server..." -ForegroundColor Cyan
& $dotnet.Source build "$repoRoot\server\windows\StreamPanel.Server\StreamPanel.Server.csproj" -c Debug --nologo -v q
if ($LASTEXITCODE -ne 0) {
    throw "Server build failed."
}

Write-Host ""
Write-Host "Starting StreamPanel server as Administrator (UAC prompt)..." -ForegroundColor Cyan
$runArgs = "run --project `"$repoRoot\server\windows\StreamPanel.Server\StreamPanel.Server.csproj`" --no-build"
Start-Process -FilePath $dotnet.Source -ArgumentList $runArgs -Verb RunAs -WorkingDirectory $repoRoot
