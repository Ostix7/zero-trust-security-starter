param(
    [int]$TimeoutSeconds = 120
)

$ErrorActionPreference = "Stop"

function Wait-ForHealth {
    param(
        [string]$Name,
        [string]$Url,
        [int]$TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-RestMethod -Method Get -Uri $Url -TimeoutSec 5
            if ($response.status -eq "UP") {
                Write-Host "[ready] $Name -> $Url" -ForegroundColor Green
                return
            }
        } catch {
            Start-Sleep -Seconds 2
            continue
        }
        Start-Sleep -Seconds 2
    }

    throw "Timed out waiting for $Name health endpoint: $Url"
}

function Wait-ForHttp200 {
    param(
        [string]$Name,
        [string]$Url,
        [int]$TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Method Get -Uri $Url -TimeoutSec 5
            if ($response.StatusCode -eq 200) {
                Write-Host "[ready] $Name -> $Url" -ForegroundColor Green
                return
            }
        } catch {
            Start-Sleep -Seconds 2
            continue
        }
        Start-Sleep -Seconds 2
    }

    throw "Timed out waiting for $Name endpoint: $Url"
}

Write-Host "Starting Zero Trust diploma demo with Docker Compose..." -ForegroundColor Cyan
docker compose up --build -d

Wait-ForHealth -Name "policy-service" -Url "http://localhost:8082/actuator/health" -TimeoutSeconds $TimeoutSeconds
Wait-ForHealth -Name "resource-service" -Url "http://localhost:8081/actuator/health" -TimeoutSeconds $TimeoutSeconds
Wait-ForHealth -Name "gateway-service" -Url "http://localhost:8080/actuator/health" -TimeoutSeconds $TimeoutSeconds
Wait-ForHttp200 -Name "prometheus" -Url "http://localhost:9090/-/ready" -TimeoutSeconds $TimeoutSeconds
Wait-ForHttp200 -Name "grafana" -Url "http://localhost:3000/api/health" -TimeoutSeconds $TimeoutSeconds

Write-Host ""
Write-Host "Demo platform is ready." -ForegroundColor Green
Write-Host "Gateway:  http://localhost:8080"
Write-Host "Resource: http://localhost:8081"
Write-Host "Policy:   http://localhost:8082"
Write-Host "Prometheus: http://localhost:9090"
Write-Host "Grafana:    http://localhost:3000 (admin/admin)"
