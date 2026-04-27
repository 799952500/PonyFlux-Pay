# Fix PonyFlux-Pay sys_roles Chinese garbled text
$baseUri = "http://localhost:3003/api/v1/admin"

# Login
$loginResp = Invoke-RestMethod -Uri "$baseUri/auth/login" -Method POST -ContentType "application/json" -Body '{"username":"admin","password":"admin123"}' -UseBasicParsing
$token = $loginResp.data.token
$headers = @{Authorization="Bearer $token"}

# Fix Role 1 - SUPER_ADMIN
$body1 = @{
    roleCode = "SUPER_ADMIN"
    roleName = "超级管理员"
    description = "拥有系统所有权限"
    status = "ACTIVE"
} | ConvertTo-Json -Compress

$r1 = Invoke-RestMethod -Uri "$baseUri/roles/1" -Method PUT -ContentType "application/json" -Headers $headers -Body $body1 -UseBasicParsing
Write-Host "Role 1 update: $($r1 | ConvertTo-Json -Compress)"

# Fix Role 2 - ADMIN
$body2 = @{
    roleCode = "ADMIN"
    roleName = "管理员"
    description = "拥有大部分管理权限"
    status = "ACTIVE"
} | ConvertTo-Json -Compress

$r2 = Invoke-RestMethod -Uri "$baseUri/roles/2" -Method PUT -ContentType "application/json" -Headers $headers -Body $body2 -UseBasicParsing
Write-Host "Role 2 update: $($r2 | ConvertTo-Json -Compress)"

# Fix Role 3 - FINANCE (need to keep the original code)
$body3 = @{
    roleCode = "FINANCE"
    roleName = "财务人员"
    description = "负责财务相关操作"
    status = "ACTIVE"
} | ConvertTo-Json -Compress

$r3 = Invoke-RestMethod -Uri "$baseUri/roles/3" -Method PUT -ContentType "application/json" -Headers $headers -Body $body3 -UseBasicParsing
Write-Host "Role 3 update: $($r3 | ConvertTo-Json -Compress)"

# Fix Role 4 - RISK
$body4 = @{
    roleCode = "RISK"
    roleName = "风控人员"
    description = "负责风控管理操作"
    status = "ACTIVE"
} | ConvertTo-Json -Compress

$r4 = Invoke-RestMethod -Uri "$baseUri/roles/4" -Method PUT -ContentType "application/json" -Headers $headers -Body $body4 -UseBasicParsing
Write-Host "Role 4 update: $($r4 | ConvertTo-Json -Compress)"

# Verify - Get all roles again
Write-Host "`n=== Verification ==="
$verify = Invoke-RestMethod -Uri "$baseUri/roles" -Headers $headers -UseBasicParsing
$verify.data | ConvertTo-Json -Depth 3