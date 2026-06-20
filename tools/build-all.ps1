$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

Write-Host "Checking prerequisites..." -ForegroundColor Cyan
& "$PSScriptRoot\check-prereqs.ps1"

Write-Host ""
Write-Host "Building Windows companion server..." -ForegroundColor Cyan
dotnet build "$repoRoot\server\windows\StreamPanel.Server\StreamPanel.Server.csproj" -c Release

Write-Host ""
Write-Host "Building Android app..." -ForegroundColor Cyan
Set-Location "$repoRoot\android"
if (Test-Path ".\gradlew.bat") {
    & ".\gradlew.bat" assembleDebug
} else {
    gradle assembleDebug
}

Write-Host ""
Write-Host "Packaging friend bundle (APK + Windows EXE)..." -ForegroundColor Cyan
& "$PSScriptRoot\package-friend-bundle.ps1"

Write-Host ""
Write-Host "Build completed." -ForegroundColor Green
