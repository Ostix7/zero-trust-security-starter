$ErrorActionPreference = "Stop"

Write-Host "Stopping Zero Trust diploma demo..." -ForegroundColor Cyan
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    docker compose down --remove-orphans
} finally {
    Pop-Location
}
