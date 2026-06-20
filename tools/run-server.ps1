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



$portInUse = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($portInUse) {
    $ownerPid = $portInUse.OwningProcess
    $owner = Get-Process -Id $ownerPid -ErrorAction SilentlyContinue
    $ownerName = if ($owner) { $owner.ProcessName } else { "PID $ownerPid" }
    Write-Host "Port $Port is already in use by $ownerName (PID $ownerPid)." -ForegroundColor Yellow
    Write-Host "Server is already running. Status: http://localhost:$Port/status" -ForegroundColor Green
    Write-Host ""
    Write-Host "To restart: powershell -ExecutionPolicy Bypass -File .\tools\stop-server.ps1" -ForegroundColor DarkGray
    Write-Host "            then run-server.ps1 again" -ForegroundColor DarkGray
    exit 0
}

Write-Host "Building StreamPanel server..." -ForegroundColor Cyan

& $dotnet.Source build "$repoRoot\server\windows\StreamPanel.Server\StreamPanel.Server.csproj" -c Debug --nologo -v q

if ($LASTEXITCODE -ne 0) {

    throw "Server build failed."

}

Write-Host ""

Write-Host "Starting StreamPanel server..." -ForegroundColor Cyan

& $dotnet.Source run --project "$repoRoot\server\windows\StreamPanel.Server\StreamPanel.Server.csproj" --no-build

