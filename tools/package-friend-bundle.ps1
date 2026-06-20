$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$bundleRoot = Join-Path $repoRoot "dist\StreamPanel-FriendBundle"
$zipPath = Join-Path $repoRoot "dist\StreamPanel-FriendBundle.zip"
$serverOut = Join-Path $bundleRoot "WindowsServer"

Write-Host "Building friend bundle in $bundleRoot" -ForegroundColor Cyan

if (Test-Path $bundleRoot) {
    Remove-Item $bundleRoot -Recurse -Force
}
if (Test-Path $zipPath) {
    Remove-Item $zipPath -Force
}
New-Item -ItemType Directory -Force -Path $bundleRoot | Out-Null
New-Item -ItemType Directory -Force -Path $serverOut | Out-Null

Write-Host "1/3 Android APK..." -ForegroundColor Yellow
Set-Location "$repoRoot\android"
if (Test-Path ".\gradlew.bat") {
    & ".\gradlew.bat" assembleDebug
} else {
    gradle assembleDebug
}
$apkSource = "$repoRoot\android\app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apkSource)) {
    throw "APK not found at $apkSource"
}
Copy-Item $apkSource (Join-Path $bundleRoot "StreamPanel.apk") -Force

Write-Host "2/3 Windows server (self-contained EXE)..." -ForegroundColor Yellow
$dotnet = Get-Command dotnet -ErrorAction SilentlyContinue
if ($null -eq $dotnet) {
    Write-Host "dotnet SDK not found. Installing via winget..." -ForegroundColor Yellow
    & "$PSScriptRoot\install-prereqs-windows.ps1"
    $dotnet = Get-Command dotnet -ErrorAction SilentlyContinue
    if ($null -eq $dotnet) {
        throw "dotnet is still missing. Restart PowerShell and run this script again."
    }
}

dotnet publish "$repoRoot\server\windows\StreamPanel.Server\StreamPanel.Server.csproj" `
    -c Release `
    -r win-x64 `
    --self-contained true `
    /p:PublishSingleFile=true `
    /p:IncludeNativeLibrariesForSelfExtract=true `
    /p:EnableCompressionInSingleFile=true `
    -o $serverOut

Write-Host "3/3 Launchers + README..." -ForegroundColor Yellow
Copy-Item "$PSScriptRoot\run-server.ps1" $bundleRoot -Force
Copy-Item "$PSScriptRoot\stop-server.ps1" $bundleRoot -Force
Copy-Item "$PSScriptRoot\allow-firewall.ps1" $bundleRoot -Force

$readme = @"
StreamPanel — быстрый старт
===========================

НА ТЕЛЕФОНЕ
1. Установите StreamPanel.apk (разрешите установку из неизвестных источников).
2. Откройте приложение → Настройки → укажите IP вашего ПК и порт 17820.
3. Нажмите Подключить.

НА ПК (Windows)
1. Запустите START-SERVER.bat (или run-server.ps1).
2. Разрешите доступ в брандмауэре, если Windows спросит.
3. Сервер слушает порт 17820. Иконка в трее — StreamPanel.
4. Если телефон не подключается, запустите ALLOW-FIREWALL.bat от имени администратора.

ВАЖНО
- Телефон и ПК должны быть в одной Wi‑Fi сети.
- IP ПК можно узнать: Win+R → cmd → ipconfig
- Остановка сервера: STOP-SERVER.bat

Состав папки
- StreamPanel.apk          — приложение для Android
- WindowsServer\           — сервер (всё внутри, .NET ставить не нужно)
- START-SERVER.bat         — запуск сервера
- STOP-SERVER.bat          — остановка
- ALLOW-FIREWALL.bat       — открыть порт 17820 в брандмауэре
"@
Set-Content -Path (Join-Path $bundleRoot "README_RU.txt") -Value $readme -Encoding UTF8

$startBat = @"
@echo off
cd /d "%~dp0WindowsServer"
start "" "StreamPanel.Server.exe"
echo StreamPanel server started on port 17820
pause
"@
Set-Content -Path (Join-Path $bundleRoot "START-SERVER.bat") -Value $startBat -Encoding ASCII

$stopBat = @"
@echo off
powershell -ExecutionPolicy Bypass -File "%~dp0stop-server.ps1"
pause
"@
Set-Content -Path (Join-Path $bundleRoot "STOP-SERVER.bat") -Value $stopBat -Encoding ASCII

$firewallBat = @"
@echo off
powershell -ExecutionPolicy Bypass -File "%~dp0allow-firewall.ps1"
pause
"@
Set-Content -Path (Join-Path $bundleRoot "ALLOW-FIREWALL.bat") -Value $firewallBat -Encoding ASCII

Compress-Archive -Path $bundleRoot -DestinationPath $zipPath -Force

Write-Host ""
Write-Host "Done! Share this folder with your friend:" -ForegroundColor Green
Write-Host $bundleRoot
Write-Host "ZIP:" -ForegroundColor Green
Write-Host $zipPath
