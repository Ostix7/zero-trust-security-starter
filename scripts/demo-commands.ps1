Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass

function Demo-Request {
    param(
        [string]$Label,
        [string]$Url,
        [string]$Token,
        [hashtable]$ExtraHeaders = @{}
    )
    Write-Host ""
    Write-Host "=== $Label ===" -ForegroundColor Yellow
    $headers = @{}
    $ExtraHeaders.GetEnumerator() | ForEach-Object { $headers[$_.Key] = $_.Value }
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    try {
        $response = Invoke-WebRequest -Method GET -Uri $Url -Headers $headers -UseBasicParsing
        Write-Host "  Status: $($response.StatusCode)" -ForegroundColor Green
        Write-Host "  $($response.Content)"
    } catch {
        $ex = $_.Exception
        if ($ex.Response) {
            $statusCode = [int]$ex.Response.StatusCode
            $reader = [System.IO.StreamReader]::new($ex.Response.GetResponseStream())
            $body = $reader.ReadToEnd()
            $reader.Close()
            Write-Host "  Status: $statusCode" -ForegroundColor Red
            Write-Host "  $body"
        } else {
            Write-Host "  Error: $($ex.Message)" -ForegroundColor Red
        }
    }
}

# === Генерація токенів ===
Write-Host "Generating tokens..." -ForegroundColor Cyan
$admin   = & .\scripts\generate-demo-jwt.ps1 -Subject security-admin -Roles ADMIN
$user    = & .\scripts\generate-demo-jwt.ps1 -Subject alice -Roles USER
$fin     = & .\scripts\generate-demo-jwt.ps1 -Subject fin-user -Roles USER -Department finance -TenantId acme
$sales   = & .\scripts\generate-demo-jwt.ps1 -Subject sales-user -Roles USER -Department sales -TenantId acme
$tenant  = & .\scripts\generate-demo-jwt.ps1 -Subject tenant-user -Roles USER -TenantId acme
Write-Host "Done." -ForegroundColor Green

Demo-Request -Label "1. Public endpoint - no token needed (200)" `
    -Url "http://localhost:8081/public/hello"

Demo-Request -Label "2. Deny-by-default - no token = 401" `
    -Url "http://localhost:8081/api/me"

Demo-Request -Label "3. RBAC - USER on admin endpoint = 403" `
    -Url "http://localhost:8081/admin/report" -Token $user

Demo-Request -Label "4. RBAC - ADMIN on admin endpoint = 200" `
    -Url "http://localhost:8081/admin/report" -Token $admin

Demo-Request -Label "5. Token propagation - gateway proxies user JWT to resource" `
    -Url "http://localhost:8080/api/proxy/me" -Token $fin

Demo-Request -Label "5b. Token propagation via WebClient - same starter, different client" `
    -Url "http://localhost:8080/api/proxy-webclient/me" -Token $fin

Demo-Request -Label "6. Service token - gateway uses Vault secret for non-human call" `
    -Url "http://localhost:8080/public/service-ping"

Demo-Request -Label "7. Tenant ABAC - matching tenant = 200" `
    -Url "http://localhost:8080/api/proxy/tenant-orders" -Token $tenant -ExtraHeaders @{ "X-Tenant-Id" = "acme" }

Demo-Request -Label "8. Tenant ABAC - mismatched tenant = 403" `
    -Url "http://localhost:8080/api/proxy/tenant-orders" -Token $tenant -ExtraHeaders @{ "X-Tenant-Id" = "other" }

Demo-Request -Label "9. External policy - finance department = 200" `
    -Url "http://localhost:8080/api/proxy/finance-report" -Token $fin

Demo-Request -Label "10. External policy - sales department = 403" `
    -Url "http://localhost:8080/api/proxy/finance-report" -Token $sales

Demo-Request -Label "11. Audit events - admin reads security log" `
    -Url "http://localhost:8080/api/proxy/audit-events" -Token $admin

Write-Host ""
Write-Host "=== 12. Rate limiting test - 50 rapid requests (limit: 20/s, burst: 40) ===" -ForegroundColor Yellow
$rejected = 0
$accepted = 0
for ($i = 1; $i -le 50; $i++) {
    try {
        $null = Invoke-WebRequest -Method GET -Uri "http://localhost:8081/api/me" -Headers @{ Authorization = "Bearer $user" } -UseBasicParsing
        $accepted++
    } catch {
        if ($_.Exception.Response) {
            $code = [int]$_.Exception.Response.StatusCode
            if ($code -eq 429) { $rejected++ } else { $accepted++ }
        }
    }
}
Write-Host "  Accepted: $accepted | Rejected (429): $rejected" -ForegroundColor Cyan
if ($rejected -gt 0) {
    Write-Host "  Rate limiting is working!" -ForegroundColor Green
} else {
    Write-Host "  (burst capacity absorbed all 50 requests)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Done ===" -ForegroundColor Green
Write-Host ""
Write-Host "Next: run tests with 'mvn test -q'"
Write-Host "Grafana:    http://localhost:3000"
Write-Host "Prometheus: http://localhost:9090"
