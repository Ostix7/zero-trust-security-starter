param(
    [string]$Secret = "01234567890123456789012345678901",
    [string]$Issuer = "https://issuer.diploma.local",
    [string]$Audience = "diploma-services",
    [string]$Subject = "demo-user",
    [string[]]$Roles = @("USER"),
    [string]$TenantId,
    [string]$Department,
    [long]$IssuedAt = 1735689600,
    [long]$ExpiresAt = 4102444800
)

function ConvertTo-Base64Url([byte[]]$bytes) {
    [Convert]::ToBase64String($bytes).TrimEnd("=").Replace("+", "-").Replace("/", "_")
}

$headerJson = '{"alg":"HS256","typ":"JWT"}'
$payload = [ordered]@{
    iss = $Issuer
    sub = $Subject
    aud = $Audience
    roles = $Roles
    iat = $IssuedAt
    nbf = $IssuedAt
    exp = $ExpiresAt
}

if ($TenantId) {
    $payload.tenant_id = $TenantId
}
if ($Department) {
    $payload.department = $Department
}

$headerEncoded = ConvertTo-Base64Url([Text.Encoding]::UTF8.GetBytes($headerJson))
$payloadEncoded = ConvertTo-Base64Url([Text.Encoding]::UTF8.GetBytes(($payload | ConvertTo-Json -Compress)))
$unsignedToken = "$headerEncoded.$payloadEncoded"
$hmac = [System.Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes($Secret))
$signature = ConvertTo-Base64Url($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($unsignedToken)))

Write-Output "$unsignedToken.$signature"
