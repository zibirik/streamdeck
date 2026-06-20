param(
    [switch]$IncludeAndroidStudio
)

$ErrorActionPreference = "Stop"

function Require-Winget {
    if ($null -eq (Get-Command winget -ErrorAction SilentlyContinue)) {
        throw "winget is not available. Install App Installer from Microsoft Store or install tools manually."
    }
}

function Install-PackageIfMissing {
    param(
        [Parameter(Mandatory = $true)][string]$Command,
        [Parameter(Mandatory = $true)][string]$WingetId
    )

    if ($null -ne (Get-Command $Command -ErrorAction SilentlyContinue)) {
        Write-Host "[ok] $Command already installed" -ForegroundColor Green
        return
    }

    Write-Host "[install] $WingetId" -ForegroundColor Cyan
    winget install --id $WingetId --exact --source winget --accept-package-agreements --accept-source-agreements
}

Require-Winget

Install-PackageIfMissing -Command "dotnet" -WingetId "Microsoft.DotNet.SDK.8"
Install-PackageIfMissing -Command "java" -WingetId "EclipseAdoptium.Temurin.17.JDK"
if ($IncludeAndroidStudio) {
    Write-Host "[install] Google.AndroidStudio" -ForegroundColor Cyan
    winget install --id Google.AndroidStudio --exact --source winget --accept-package-agreements --accept-source-agreements
}

Write-Host ""
Write-Host "Done. Restart PowerShell so PATH updates are visible, then run tools\check-prereqs.ps1." -ForegroundColor Green
