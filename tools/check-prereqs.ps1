param(
    [switch]$Quiet
)

$ErrorActionPreference = "Stop"

function Test-Tool {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$InstallHint
    )

    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if ($null -eq $command) {
        if (-not $Quiet) {
            Write-Host "[missing] $Name" -ForegroundColor Red
            Write-Host "          $InstallHint" -ForegroundColor DarkGray
        }
        return $false
    }

    if (-not $Quiet) {
        Write-Host "[ok]      $Name -> $($command.Source)" -ForegroundColor Green
    }
    return $true
}

function Test-AndroidSdk {
    $sdkPath = $env:ANDROID_HOME
    if ([string]::IsNullOrWhiteSpace($sdkPath)) {
        $sdkPath = $env:ANDROID_SDK_ROOT
    }

    if ([string]::IsNullOrWhiteSpace($sdkPath) -or -not (Test-Path $sdkPath)) {
        if (-not $Quiet) {
            Write-Host "[missing] Android SDK" -ForegroundColor Red
            Write-Host "          Install Android Studio, open SDK Manager, install Android SDK Platform 35 and set ANDROID_HOME." -ForegroundColor DarkGray
        }
        return $false
    }

    if (-not $Quiet) {
        Write-Host "[ok]      Android SDK -> $sdkPath" -ForegroundColor Green
    }
    return $true
}

Write-Host "StreamPanel prerequisite check" -ForegroundColor Cyan
Write-Host ""

$ok = $true
$ok = (Test-Tool "dotnet" "Install .NET 8 SDK: winget install Microsoft.DotNet.SDK.8") -and $ok
$ok = (Test-Tool "java" "Install JDK 17: winget install EclipseAdoptium.Temurin.17.JDK") -and $ok
$gradleWrapper = Join-Path $PSScriptRoot "..\android\gradlew.bat" | Resolve-Path -ErrorAction SilentlyContinue
if ($gradleWrapper -and (Test-Path $gradleWrapper)) {
    if (-not $Quiet) {
        Write-Host "[ok]      Gradle Wrapper -> $gradleWrapper" -ForegroundColor Green
    }
} else {
    $ok = (Test-Tool "gradle" "Install Gradle manually or use Android Studio build") -and $ok
}
$ok = (Test-AndroidSdk) -and $ok

Write-Host ""
if ($ok) {
    Write-Host "All required tools are available." -ForegroundColor Green
    exit 0
}

Write-Host "Some tools are missing. Run tools\install-prereqs-windows.ps1 or install them manually." -ForegroundColor Yellow
exit 1
