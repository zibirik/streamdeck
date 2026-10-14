<#
.SYNOPSIS
  Builds the files that are attached to a GitHub release.

.DESCRIPTION
  Produces in dist\release\:
    StreamPanel-v<Version>.apk                    Android app
    StreamPanel-Server-v<Version>-win-x64.zip     Windows server (self-contained, no .NET needed)
    SHA256SUMS.txt                                checksums of both files

  Without -ApkPath the release APK is built with Gradle first (needs JDK 17 + Android SDK).
#>
param(
    [Parameter(Mandatory = $true)][string]$Version,
    [string]$ApkPath = "",
    [string]$OutDir = ""
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if (-not $OutDir) { $OutDir = Join-Path $repoRoot "dist\release" }
$staging = Join-Path $repoRoot "dist\staging\StreamPanel-Server"

if ($Version -notmatch '^\d+\.\d+\.\d+(-[0-9A-Za-z.-]+)?$') {
    throw "Version '$Version' is not semantic (expected e.g. 1.2.3 or 1.2.3-beta.1)."
}

if (Test-Path $OutDir) { Remove-Item $OutDir -Recurse -Force }
if (Test-Path (Split-Path $staging)) { Remove-Item (Split-Path $staging) -Recurse -Force }
New-Item -ItemType Directory -Force -Path $OutDir, $staging | Out-Null

# --- Android -----------------------------------------------------------------------------------------------
if (-not $ApkPath) {
    Write-Host "Building release APK..." -ForegroundColor Cyan
    Push-Location (Join-Path $repoRoot "android")
    try {
        & ".\gradlew.bat" assembleRelease "-PappVersionName=$Version" --console=plain
        if ($LASTEXITCODE -ne 0) { throw "Gradle build failed." }
    } finally { Pop-Location }
    $ApkPath = Join-Path $repoRoot "android\app\build\outputs\apk\release\app-release.apk"
}
if (-not (Test-Path $ApkPath)) { throw "APK not found: $ApkPath" }
$apkOut = Join-Path $OutDir "StreamPanel-v$Version.apk"
Copy-Item $ApkPath $apkOut -Force

# --- Windows server ----------------------------------------------------------------------------------------
Write-Host "Publishing Windows server..." -ForegroundColor Cyan
dotnet publish (Join-Path $repoRoot "server\windows\StreamPanel.Server\StreamPanel.Server.csproj") `
    -c Release -r win-x64 --self-contained true `
    "-p:Version=$Version" `
    -p:PublishSingleFile=true `
    -p:IncludeNativeLibrariesForSelfExtract=true `
    -p:EnableCompressionInSingleFile=true `
    -p:DebugType=None -p:DebugSymbols=false `
    -o $staging
if ($LASTEXITCODE -ne 0) { throw "dotnet publish failed." }

Copy-Item (Join-Path $PSScriptRoot "stop-server.ps1") $staging -Force
Copy-Item (Join-Path $PSScriptRoot "allow-firewall.ps1") $staging -Force
foreach ($file in "LICENSE", "COMMERCIAL.md", "NOTICE.md") {
    Copy-Item (Join-Path $repoRoot $file) $staging -Force
}

Set-Content -Path (Join-Path $staging "START-SERVER.bat") -Encoding ASCII -Value @'
@echo off
cd /d "%~dp0"
start "" "StreamPanel.Server.exe"
echo StreamPanel server started on port 17820. See README.txt for the PIN setup.
pause
'@
Set-Content -Path (Join-Path $staging "STOP-SERVER.bat") -Encoding ASCII -Value @'
@echo off
powershell -ExecutionPolicy Bypass -File "%~dp0stop-server.ps1"
pause
'@
Set-Content -Path (Join-Path $staging "ALLOW-FIREWALL.bat") -Encoding ASCII -Value @'
@echo off
echo Run this file as Administrator (right click - Run as administrator).
powershell -ExecutionPolicy Bypass -File "%~dp0allow-firewall.ps1"
pause
'@

Set-Content -Path (Join-Path $staging "README.txt") -Encoding UTF8 -Value @"
StreamPanel Server $Version
===========================
https://github.com/zibirik/streamdeck

QUICK START
1. Run START-SERVER.bat (allow the Windows firewall prompt, or run ALLOW-FIREWALL.bat as administrator).
2. Set a PIN (strongly recommended): open appsettings.json, set  "Pin": "your-pin"  and restart the server.
   Without a PIN anyone on your Wi-Fi can control this PC.
3. Install StreamPanel-v$Version.apk on your Android tablet.
4. In the app: Settings -> enter this PC's IP address (ipconfig -> IPv4), port 17820 and the same PIN.
Both devices must be on the same Wi-Fi network. Stop the server with STOP-SERVER.bat.

БЫСТРЫЙ СТАРТ
1. Запустите START-SERVER.bat (разрешите доступ в брандмауэре или запустите ALLOW-FIREWALL.bat от администратора).
2. Задайте PIN (настоятельно рекомендуется): в appsettings.json укажите  "Pin": "ваш-пин"  и перезапустите сервер.
   Без PIN любой в вашей Wi-Fi сети может управлять этим ПК.
3. Установите StreamPanel-v$Version.apk на Android-планшет.
4. В приложении: Настройки -> IP этого ПК (ipconfig -> IPv4), порт 17820 и тот же PIN.
Устройства должны быть в одной Wi-Fi сети. Остановка: STOP-SERVER.bat.

LICENSE
Free for noncommercial use (PolyForm Noncommercial 1.0.0, see LICENSE). Commercial use needs a license: see COMMERCIAL.md.
"@

$zipOut = Join-Path $OutDir "StreamPanel-Server-v$Version-win-x64.zip"
Compress-Archive -Path $staging -DestinationPath $zipOut -Force

# --- Checksums ---------------------------------------------------------------------------------------------
$sums = Get-ChildItem $OutDir -File | Where-Object { $_.Name -ne "SHA256SUMS.txt" } | ForEach-Object {
    "{0}  {1}" -f (Get-FileHash $_.FullName -Algorithm SHA256).Hash.ToLower(), $_.Name
}
Set-Content -Path (Join-Path $OutDir "SHA256SUMS.txt") -Value $sums -Encoding ASCII

Write-Host ""
Write-Host "Release files in $OutDir" -ForegroundColor Green
Get-ChildItem $OutDir | ForEach-Object { "  {0}  ({1:N1} MB)" -f $_.Name, ($_.Length / 1MB) }
