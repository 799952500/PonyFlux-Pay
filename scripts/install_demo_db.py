#!/usr/bin/env python3
"""
一键安装演示库：建库 → 建表 → 灌入演示数据。

用法:
  python scripts/install_demo_db.py
  python scripts/install_demo_db.py --host 127.0.0.1 --user root --password root
"""

from __future__ import annotations

import argparse
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

from run_mysql_sql import is_comment_only, split_sql  # noqa: E402

import mysql.connector

SQL_FILES = [
    ROOT / "sql/schema/00_create_databases.sql",
    ROOT / "sql/schema/payflow_admin.sql",
    ROOT / "sql/schema/payflow_cashier.sql",
    ROOT / "sql/seed/payflow_cashier_seed.sql",
    ROOT / "sql/seed/payflow_admin_seed.sql",
]


def execute_file(cur, path: pathlib.Path) -> int:
    text = path.read_text(encoding="utf-8")
    count = 0
    for stmt in split_sql(text):
        if not stmt or is_comment_only(stmt):
            continue
        cur.execute(stmt)
        if getattr(cur, "with_rows", False):
            cur.fetchall()
        count += 1
    return count


def main() -> None:
    p = argparse.ArgumentParser(description="安装 PonyFlux Pay 演示数据库")
    p.add_argument("--host", default="127.0.0.1")
    p.add_argument("--port", type=int, default=3306)
    p.add_argument("--user", default="root")
    p.add_argument("--password", default="root")
    args = p.parse_args()

    conn = mysql.connector.connect(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        autocommit=True,
        use_pure=True,
    )
    cur = conn.cursor()
    try:
        total = 0
        for path in SQL_FILES:
            if not path.is_file():
                raise FileNotFoundError(path)
            n = execute_file(cur, path)
            total += n
            print(f"OK  {path.relative_to(ROOT)}  ({n} statements)")
        print(f"\n演示库安装完成，共执行 {total} 条语句。")
        print("管理后台登录: admin / admin123")
        print("验证: SELECT COUNT(*) FROM payflow_cashier.cashier_orders;")
    finally:
        cur.close()
        conn.close()


if __name__ == "__main__":
    main()
