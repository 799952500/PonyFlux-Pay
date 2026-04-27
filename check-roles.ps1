# Check if backend is running
$resp = Invoke-RestMethod -Uri 'http://localhost:3003/api/v1/admin/auth/login' -Method POST -ContentType 'application/json' -Body '{"username":"admin","password":"admin123"}' -UseBasicParsing
Write-Host "Login response:"
$resp | ConvertTo-Json -Depth 5
$token = $resp.data.token
Write-Host "Token: $token"

# Get roles
$roles = Invoke-RestMethod -Uri 'http://localhost:3003/api/v1/admin/roles' -Headers @{Authorization="Bearer $token"} -UseBasicParsing
Write-Host "`nRoles response:"
$roles | ConvertTo-Json -Depth 5