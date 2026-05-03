# 本地联调依赖探测（Windows PowerShell）
# 用法: .\scripts\check-local-env.ps1

$ErrorActionPreference = 'Continue'
$hosts = @(
  @{ Name = 'MySQL'; Host = '127.0.0.1'; Port = 3306 },
  @{ Name = 'Redis'; Host = '127.0.0.1'; Port = 6379 },
  @{ Name = 'Cashier'; Host = '127.0.0.1'; Port = 3002 },
  @{ Name = 'Admin'; Host = '127.0.0.1'; Port = 3003 }
)

Write-Host '=== PayFlow 本地依赖检测 ===' -ForegroundColor Cyan
foreach ($h in $hosts) {
  try {
    $r = Test-NetConnection -ComputerName $h.Host -Port $h.Port -WarningAction SilentlyContinue
    if ($r.TcpTestSucceeded) {
      Write-Host "[OK] $($h.Name) $($h.Host):$($h.Port)" -ForegroundColor Green
    } else {
      Write-Host "[--] $($h.Name) $($h.Host):$($h.Port) 不可达" -ForegroundColor Yellow
    }
  } catch {
    Write-Host "[!!] $($h.Name) 检测异常: $_" -ForegroundColor Red
  }
}
Write-Host '完成。MySQL/Redis 未启动时请先启动服务再跑收银台与管理端。' -ForegroundColor Gray
