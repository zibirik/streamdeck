$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
dotnet test "$repoRoot\server\windows\StreamPanel.sln" -c Release
if ($LASTEXITCODE -ne 0) { throw "Server tests failed." }
