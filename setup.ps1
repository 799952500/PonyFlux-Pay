# PonyFlux Pay 一键初始化（Windows）
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

if (-not (Get-Command python -ErrorAction SilentlyContinue)) {
    Write-Error "未找到 python，请先安装 Python 3.9+"
}

python -m pip install -q -r scripts/requirements.txt
python scripts/setup.py @args
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
