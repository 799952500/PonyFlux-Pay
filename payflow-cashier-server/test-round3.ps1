# ============================================================
# 收银台后端第三轮测试脚本
# 必须在中文路径目录内运行，确保 UTF-8 编码
# ============================================================
$ErrorActionPreference = "Continue"

$ProjectDir = "D:\个人\pay\PonyFlux-Pay\payflow-cashier-server"
$PomFile = $ProjectDir + "\pom.xml"
$LogFile = $ProjectDir + "\test-round3.log"

# 确保 JAVA_TOOL_OPTIONS 设置在当前 PowerShell 进程
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"
Write-Host "JAVA_TOOL_OPTIONS=$env:JAVA_TOOL_OPTIONS"

# 预创建日志文件（避免 Add-Content 找不到目录）
"=== Test Round 3 - $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') ===" | Out-File $LogFile -Encoding UTF8

function Log($msg) {
    $ts = Get-Date -Format "HH:mm:ss"
    $line = "[$ts] $msg"
    Write-Host $line
    $line | Out-File $LogFile -Append -Encoding UTF8
}

# ============================================================
# Step 1: 编译（确保 JAVA_TOOL_OPTIONS 传递给 mvn）
# ============================================================
Log "=== STEP 1: Maven Clean Compile ==="
# 在同一会话直接运行，继承 $env:JAVA_TOOL_OPTIONS
$compileOut = & mvn clean compile -f $PomFile 2>&1
$compileOut | Out-File $LogFile -Append -Encoding UTF8
if ($LASTEXITCODE -ne 0) {
    Log "COMPILE FAILED"
    Write-Host $compileOut
    exit 1
}
if ($compileOut -match "BUILD SUCCESS") {
    Log "COMPILE SUCCESS"
} else {
    Log "COMPILE unclear (exit=$LASTEXITCODE)"
}

# ============================================================
# Step 2: 启动 Spring Boot
# ============================================================
Log "=== STEP 2: Start Spring Boot (profile=test, MQ disabled) ==="

# 使用 Start-Process，它会 fork 继承当前进程环境变量
$svc = Start-Process -FilePath "mvn" `
    -ArgumentList "spring-boot:run","-Dspring-boot.run.profiles=test","-f",$PomFile `
    -WorkingDirectory $ProjectDir `
    -PassThru `
    -NoNewWindow `
    -RedirectStandardOutput "$ProjectDir\svc-stdout.log" `
    -RedirectStandardError "$ProjectDir\svc-stderr.log"

Log "Service PID: $($svc.Id)"

# 轮询等待服务启动
$maxWait = 120; $waited = 0
while ($waited -lt $maxWait) {
    Start-Sleep -Seconds 10; $waited += 10
    $portCheck = netstat -ano | Select-String ":3002\s+LISTENING"
    if ($portCheck) {
        Log "SERVICE STARTED in ${waited}s"
        break
    }
    Log "  ... waiting (${waited}s)"
}

if (-not $portCheck) {
    Log "SERVICE NOT started after ${maxWait}s"
    # 读 stderr 找错误
    $err = Get-Content "$ProjectDir\svc-stderr.log" -Raw -ErrorAction SilentlyContinue
    if ($err) { Log "[STDERR] $err" }
    Stop-Process $svc.Id -Force -ErrorAction SilentlyContinue
    exit 1
}

# 读启动日志
Start-Sleep -Seconds 3
$stdout = Get-Content "$ProjectDir\svc-stdout.log" -Raw -ErrorAction SilentlyContinue
if ($stdout) { Log "[STDOUT] $stdout" }

# ============================================================
# Step 3: API 测试
# ============================================================
$BaseUrl = "http://localhost:3002"
$nowTs = [Math]::Floor((Get-Date -UFormat %s)).ToString()
$merchantId = "M2024040001"
$appSecret = "4f3c2b1a0e9d8c7b6a5f4e3d2c1b0a9f"

function ComputeSig($m, $p, $qs, $ts, $sec) {
    $text = "$m`n$p`n$qs`n$ts"
    $hmac = [System.Security.Cryptography.HMAC]::new(
        [System.Security.Cryptography.HMACAlgorithm]::SHA256,
        [Text.Encoding]::UTF8.GetBytes($sec))
    $bytes = $hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($text))
    $hmac.Dispose()
    return [BitConverter]::ToString($bytes).Replace("-","").ToLower()
}

function InvokeAndLog($name, $method, $url, $hdrs=@{}) {
    Log "--- $name ---"
    Log "  URL: $method $url"
    try {
        $r = Invoke-WebRequest -Uri $url -Method $method -Headers $hdrs -UseBasicParsing -TimeoutSec 10
        Log "  HTTP $($r.StatusCode) | $($r.Content)"
    } catch {
        $ex = $_.Exception
        if ($ex.Response) {
            $body = try { [System.IO.StreamReader]::new($ex.Response.GetResponseStream()).ReadToEnd() } catch { "" }
            Log "  HTTP $([int]$ex.Response.StatusCode) | $body"
        } else {
            Log "  ERROR: $($ex.Message)"
        }
    }
}

Log "=== STEP 3: API Tests ==="

InvokeAndLog "Health" -method "GET" -url "$BaseUrl/actuator/health"
InvokeAndLog "GET /cashier/ORD20260420002 (PAID, should include successUrl/failUrl)" -method "GET" -url "$BaseUrl/api/v1/cashier/ORD20260420002"
InvokeAndLog "GET /cashier/ORD20260420003 (EXPIRED, should include successUrl/failUrl)" -method "GET" -url "$BaseUrl/api/v1/cashier/ORD20260420003"
InvokeAndLog "GET /payments/status/PAY20260420002A" -method "GET" -url "$BaseUrl/api/v1/payments/status/PAY20260420002A"
InvokeAndLog "GET /merchant/orders (no auth)" -method "GET" -url "$BaseUrl/api/v1/merchant/orders?page=1&size=10"

# Scenario 1: Valid Signature
$path = "/api/v1/merchant/orders"
$params = @("merchantId=$merchantId","page=1","size=10","timestamp=$nowTs")
$qs = ($params | Sort-Object) -join "&"
$sign = ComputeSig -m "GET" -p $path -qs $qs -ts $nowTs -sec $appSecret
$hdrs = @{ "X-Merchant-Id"=$merchantId; "X-Timestamp"=$nowTs; "X-Sign"=$sign }
Log "Valid Sig: qs=$qs | sign=$sign"
InvokeAndLog "Scenario 1: Valid Signature" -method "GET" -url "$BaseUrl$path`?$qs" -hdrs $hdrs

# Scenario 2: Wrong Signature
$hdrs2 = @{ "X-Merchant-Id"=$merchantId; "X-Timestamp"=$nowTs; "X-Sign"="deadbeef0000" }
InvokeAndLog "Scenario 2: Wrong Signature" -method "GET" -url "$BaseUrl$path`?$qs" -hdrs $hdrs2

# Scenario 3: Missing X-Sign
$hdrs3 = @{ "X-Merchant-Id"=$merchantId; "X-Timestamp"=$nowTs }
InvokeAndLog "Scenario 3: Missing X-Sign" -method "GET" -url "$BaseUrl$path`?$qs" -hdrs $hdrs3

# Scenario 4: Missing X-Merchant-Id
$hdrs4 = @{ "X-Timestamp"=$nowTs; "X-Sign"=$sign }
InvokeAndLog "Scenario 4: Missing X-Merchant-Id" -method "GET" -url "$BaseUrl$path`?$qs" -hdrs $hdrs4

# Scenario 5: Expired timestamp
$oldTs = [Math]::Floor((Get-Date -Date "2025-04-14T00:00:00").ToUniversalTime().Subtract([DateTime]::Parse("1970-01-01")).TotalSeconds).ToString()
$oldQs = "merchantId=$merchantId&page=1&size=10&timestamp=$oldTs"
$oldSign = ComputeSig -m "GET" -p $path -qs $oldQs -ts $oldTs -sec $appSecret
$hdrs5 = @{ "X-Merchant-Id"=$merchantId; "X-Timestamp"=$oldTs; "X-Sign"=$oldSign }
InvokeAndLog "Scenario 5: Expired Timestamp" -method "GET" -url "$BaseUrl$path`?$oldQs" -hdrs $hdrs5

# Scenario 6: Non-existent merchant
$fakeQs = "merchantId=M99999&page=1&size=10&timestamp=$nowTs"
$fakeSign = ComputeSig -m "GET" -p $path -qs $fakeQs -ts $nowTs -sec $appSecret
$hdrs6 = @{ "X-Merchant-Id"="M99999"; "X-Timestamp"=$nowTs; "X-Sign"=$fakeSign }
InvokeAndLog "Scenario 6: Non-existent Merchant" -method "GET" -url "$BaseUrl$path`?$fakeQs" -hdrs $hdrs6

# ============================================================
# Step 4: 清理
# ============================================================
Log "=== STEP 4: Cleanup ==="
if (-not $svc.HasExited) {
    Log "Stopping service PID=$($svc.Id)..."
    Stop-Process $svc.Id -Force -ErrorAction SilentlyContinue
}
Remove-Item "$ProjectDir\svc-stdout.log" -ErrorAction SilentlyContinue
Remove-Item "$ProjectDir\svc-stderr.log" -ErrorAction SilentlyContinue
Log "TEST COMPLETE"
Write-Host ""
Write-Host "=== DONE ==="
Write-Host "Log: $LogFile"
