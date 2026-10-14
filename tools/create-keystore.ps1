<#
.SYNOPSIS
  Creates the Android release signing key and prints the GitHub secrets needed by the release workflow.

.DESCRIPTION
  Run this ONCE. Android only lets users update an app if every release is signed with the SAME key,
  so the generated .jks file (and its password) must be backed up somewhere safe and never committed.
#>
param(
    [string]$Out = (Join-Path $env:USERPROFILE "streampanel-release.jks"),
    [string]$Alias = "streampanel",
    [string]$DName = "CN=StreamPanel, O=zibirik"
)

$ErrorActionPreference = "Stop"

$keytool = Get-Command keytool -ErrorAction SilentlyContinue
if (-not $keytool -and $env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME "bin\keytool.exe"
    if (Test-Path $candidate) { $keytool = Get-Command $candidate }
}
if (-not $keytool) { throw "keytool not found. Install JDK 17 and set JAVA_HOME (see tools\check-prereqs.ps1)." }

if (Test-Path $Out) { throw "$Out already exists. Refusing to overwrite an existing signing key." }

# Random 32-char alphanumeric password.
$chars = [char[]]((97..122) + (65..90) + (48..57))
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$bytes = New-Object byte[] 32
$rng.GetBytes($bytes)
$password = -join ($bytes | ForEach-Object { $chars[$_ % $chars.Length] })

& $keytool.Source -genkeypair -v -keystore $Out -alias $Alias -keyalg RSA -keysize 4096 -validity 10000 `
    -storetype PKCS12 -storepass $password -keypass $password -dname $DName
if ($LASTEXITCODE -ne 0) { throw "keytool failed." }

$base64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($Out))
Set-Clipboard -Value $base64

Write-Host ""
Write-Host "Keystore created: $Out" -ForegroundColor Green
Write-Host ""
Write-Host "1) BACK UP this file and the password below (password manager / offline). If you lose them you can never" -ForegroundColor Yellow
Write-Host "   publish an update that installs over the existing app." -ForegroundColor Yellow
Write-Host ""
Write-Host "2) Add these repository secrets: GitHub -> Settings -> Secrets and variables -> Actions -> New repository secret" -ForegroundColor Cyan
Write-Host "   ANDROID_KEYSTORE_BASE64    = (already copied to your clipboard - just paste)"
Write-Host "   ANDROID_KEYSTORE_PASSWORD  = $password"
Write-Host "   ANDROID_KEY_PASSWORD       = $password"
Write-Host "   ANDROID_KEY_ALIAS          = $Alias"
Write-Host ""
Write-Host "3) Clear your clipboard afterwards and do not commit the .jks (it is in .gitignore)." -ForegroundColor DarkGray
