$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

Write-Host "Checking prerequisites..." -ForegroundColor Cyan
& "$PSScriptRoot\check-prereqs.ps1"

Write-Host ""
Write-Host "Building Windows companion server..." -ForegroundColor Cyan
dotnet build "$repoRoot\server\windows\StreamPanel.Server\StreamPanel.Server.csproj" -c Release
if ($LASTEXITCODE -ne 0) { throw "Server build failed." }

Write-Host ""
Write-Host "Running server tests..." -ForegroundColor Cyan
& "$PSScriptRoot\test-server.ps1"

Write-Host ""
Write-Host "Building Android app..." -ForegroundColor Cyan
Set-Location "$repoRoot\android"
if (Test-Path ".\gradlew.bat") {
    & ".\gradlew.bat" assembleDebug
} else {
    gradle assembleDebug
}

if ($LASTEXITCODE -ne 0) { throw "Android build failed." }

Write-Host ""
Write-Host "Build completed. To create release files run: .\tools\package-release.ps1 -Version X.Y.Z" -ForegroundColor Green
