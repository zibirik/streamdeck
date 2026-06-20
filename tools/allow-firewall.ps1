param(
    [int]$Port = 17820
)

$ErrorActionPreference = "Stop"

$ruleName = "StreamPanel Server $Port"
$existing = Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue
if ($existing) {
    Write-Host "Firewall rule already exists: $ruleName" -ForegroundColor Yellow
    exit 0
}

New-NetFirewallRule `
    -DisplayName $ruleName `
    -Direction Inbound `
    -Action Allow `
    -Protocol TCP `
    -LocalPort $Port `
    -Profile Private `
    | Out-Null

Write-Host "Firewall opened for TCP port $Port (Private networks)." -ForegroundColor Green
