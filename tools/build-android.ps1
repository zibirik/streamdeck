$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location "$repoRoot\android"

if (Test-Path ".\gradlew.bat") {
    & ".\gradlew.bat" assembleDebug
} else {
    gradle assembleDebug
}
