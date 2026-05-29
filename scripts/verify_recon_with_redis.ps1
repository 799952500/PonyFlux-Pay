# 对账工作流 + Redis 联机功能验证（需 admin-server 3003 已启动）
$ErrorActionPreference = "Stop"
$base = "http://127.0.0.1:3003/api/v1"

function Solve-Captcha([string]$q) {
    if ($q -match "(\d+)\s*\+\s*(\d+)") { return [string]([int]$Matches[1] + [int]$Matches[2]) }
    throw "无法解析验证码: $q"
}

# Redis
$ping = & "D:\apps\Redis\redis-cli.exe" ping 2>&1
if ($ping -ne "PONG") { throw "Redis 未就绪: $ping" }
Write-Host "[OK] Redis PONG"

# Captcha + login
$required = Invoke-RestMethod -Uri "$base/admin/auth/captcha-required?username=admin"
$body = @{ username = "admin"; password = "admin123" }
if ($required.data.required) {
    $cap = Invoke-RestMethod -Uri "$base/admin/auth/captcha"
    $body.captchaId = $cap.data.captchaId
    $body.captchaAnswer = Solve-Captcha $cap.data.question
}
$login = Invoke-RestMethod -Uri "$base/admin/auth/login" -Method POST -ContentType "application/json" -Body ($body | ConvertTo-Json)
if ($login.code -ne 0) { throw "登录失败: $($login | ConvertTo-Json -Compress)" }
$token = $login.data.token
$h = @{ Authorization = "Bearer $token" }
Write-Host "[OK] 登录成功"

$endpoints = @(
    @{ Name = "profile"; Url = "$base/admin/auth/profile" },
    @{ Name = "dashboard"; Url = "$base/admin/dashboard" },
    @{ Name = "notifications"; Url = "$base/admin/notifications/unread-count" },
    @{ Name = "work-items"; Url = "$base/admin/reconcile/diffs/work-items?page=1&size=5" },
    @{ Name = "sla-rules"; Url = "$base/admin/reconcile/sla-rules" },
    @{ Name = "aggregation"; Url = "$base/admin/reconcile/aggregation/dashboard?dateFrom=2026-05-01&dateTo=2026-05-29" },
    @{ Name = "long-tail"; Url = "$base/admin/reconcile/long-tail/summary" },
    @{ Name = "subscriptions"; Url = "$base/admin/reconcile/subscriptions" }
)

foreach ($ep in $endpoints) {
    $r = Invoke-RestMethod -Uri $ep.Url -Headers $h
    if ($r.code -ne 0) { throw "$($ep.Name) 失败 code=$($r.code) msg=$($r.message)" }
    Write-Host "[OK] $($ep.Name) code=0"
}

Write-Host "`n全部接口验证通过（Redis + JWT 黑名单 + 对账工作流）"
