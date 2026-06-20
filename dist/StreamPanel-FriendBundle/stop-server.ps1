param(
    [int]$Port = 17820
)

$ErrorActionPreference = "Stop"

$listeners = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
if (-not $listeners) {
    Write-Host "No process is listening on port $Port." -ForegroundColor Yellow
    exit 0
}

$pids = $listeners | Select-Object -ExpandProperty OwningProcess -Unique
foreach ($procId in $pids) {
    $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
    $name = if ($proc) { $proc.ProcessName } else { "unknown" }
    Write-Host "Stopping $name (PID $procId) on port $Port..." -ForegroundColor Cyan
    Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
}

Write-Host "Port $Port is free." -ForegroundColor Green
