#!/usr/bin/env bash
# PonyFlux Pay 一键初始化（Linux / macOS）
set -euo pipefail
cd "$(dirname "$0")"

PYTHON="${PYTHON:-python3}"
if ! command -v "$PYTHON" >/dev/null 2>&1; then
  echo "未找到 python3，请先安装 Python 3.9+" >&2
  exit 1
fi

"$PYTHON" -m pip install -q -r scripts/requirements.txt
"$PYTHON" scripts/setup.py "$@"
