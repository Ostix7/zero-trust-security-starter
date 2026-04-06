$ErrorActionPreference = "Stop"

Write-Host "Stopping Zero Trust diploma demo..." -ForegroundColor Cyan
docker compose down --remove-orphans
