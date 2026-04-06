param(
    [string]$GatewayBaseUrl = "http://localhost:8080",
    [string]$ResourceBaseUrl = "http://localhost:8081",
    [switch]$StartCompose
)

$ErrorActionPreference = "Stop"

if ($StartCompose) {
    & "$PSScriptRoot\start-demo-compose.ps1"
}

function New-DemoToken {
    param(
        [string]$Subject,
        [string[]]$Roles,
        [string]$TenantId,
        [string]$Department
    )

    $params = @{
        Subject = $Subject
        Roles = $Roles
    }
    if ($TenantId) {
        $params.TenantId = $TenantId
    }
    if ($Department) {
        $params.Department = $Department
    }
    & "$PSScriptRoot\generate-demo-jwt.ps1" @params
}

function Invoke-DemoRequest {
    param(
        [string]$Label,
        [string]$Method,
        [string]$Url,
        [string]$Token,
        [hashtable]$Headers = @{}
    )

    $requestHeaders = @{}
    $Headers.GetEnumerator() | ForEach-Object { $requestHeaders[$_.Key] = $_.Value }
    if ($Token) {
        $requestHeaders["Authorization"] = "Bearer $Token"
    }

    Write-Host ""
    Write-Host "=== $Label ===" -ForegroundColor Yellow
    Write-Host "$Method $Url"
    if ($requestHeaders.Count -gt 0) {
        Write-Host ("headers: " + (($requestHeaders.GetEnumerator() | ForEach-Object { "$($_.Key)=$($_.Value)" }) -join "; "))
    }

    try {
        $response = Invoke-WebRequest -Method $Method -Uri $Url -Headers $requestHeaders
        Write-Host "status: $($response.StatusCode)" -ForegroundColor Green
        if ($response.Content) {
            Write-Host $response.Content
        }
    } catch {
        $exceptionResponse = $_.Exception.Response
        if (-not $exceptionResponse) {
            if ($_.ErrorDetails -and $_.ErrorDetails.Message) {
                Write-Host $_.ErrorDetails.Message
                return
            }
            throw
        }
        $statusCode = if ($exceptionResponse.StatusCode.value__) {
            $exceptionResponse.StatusCode.value__
        } else {
            [int]$exceptionResponse.StatusCode
        }
        Write-Host "status: $statusCode" -ForegroundColor Red

        $body = $null
        if ($_.ErrorDetails -and $_.ErrorDetails.Message) {
            $body = $_.ErrorDetails.Message
        } elseif ($exceptionResponse -is [System.Net.Http.HttpResponseMessage]) {
            $body = $exceptionResponse.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        } elseif ($exceptionResponse.GetResponseStream) {
            $reader = New-Object System.IO.StreamReader($exceptionResponse.GetResponseStream())
            $body = $reader.ReadToEnd()
        }
        if ($body) {
            Write-Host $body
        }
    }
}

$adminToken = New-DemoToken -Subject "security-admin" -Roles @("ADMIN")
$financeToken = New-DemoToken -Subject "fin-user" -Roles @("USER") -Department "finance" -TenantId "acme"
$salesToken = New-DemoToken -Subject "sales-user" -Roles @("USER") -Department "sales" -TenantId "acme"
$tenantToken = New-DemoToken -Subject "tenant-user" -Roles @("USER") -TenantId "acme"

Write-Host "Zero Trust diploma scenario" -ForegroundColor Cyan
Write-Host "Gateway:  $GatewayBaseUrl"
Write-Host "Resource: $ResourceBaseUrl"

Invoke-DemoRequest -Label "Public endpoint is open" `
    -Method "GET" `
    -Url "$ResourceBaseUrl/public/hello"

Invoke-DemoRequest -Label "Protected endpoint without token returns 401" `
    -Method "GET" `
    -Url "$ResourceBaseUrl/api/me"

Invoke-DemoRequest -Label "Gateway propagates user token and correlation id" `
    -Method "GET" `
    -Url "$GatewayBaseUrl/api/proxy/me" `
    -Token $financeToken

Invoke-DemoRequest -Label "Tenant ABAC allows matching tenant" `
    -Method "GET" `
    -Url "$GatewayBaseUrl/api/proxy/tenant-orders" `
    -Token $tenantToken `
    -Headers @{ "X-Tenant-Id" = "acme" }

Invoke-DemoRequest -Label "Tenant ABAC rejects mismatched tenant" `
    -Method "GET" `
    -Url "$GatewayBaseUrl/api/proxy/tenant-orders" `
    -Token $tenantToken `
    -Headers @{ "X-Tenant-Id" = "other" }

Invoke-DemoRequest -Label "OPA-like policy allows finance access" `
    -Method "GET" `
    -Url "$GatewayBaseUrl/api/proxy/finance-report" `
    -Token $financeToken

Invoke-DemoRequest -Label "OPA-like policy denies non-finance access" `
    -Method "GET" `
    -Url "$GatewayBaseUrl/api/proxy/finance-report" `
    -Token $salesToken

Invoke-DemoRequest -Label "Gateway uses Vault-style service token for non-human call" `
    -Method "GET" `
    -Url "$GatewayBaseUrl/public/service-ping"

Invoke-DemoRequest -Label "Admin reads accumulated security audit events" `
    -Method "GET" `
    -Url "$GatewayBaseUrl/api/proxy/audit-events" `
    -Token $adminToken

Write-Host ""
Write-Host "Observability endpoints:" -ForegroundColor Cyan
Write-Host "Prometheus: http://localhost:9090"
Write-Host "Grafana:    http://localhost:3000  (admin/admin)"
