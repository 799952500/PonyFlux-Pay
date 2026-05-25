#!/usr/bin/env python3
"""校验 payflow_admin / payflow_cashier 表名前缀是否符合规范。"""

from __future__ import annotations

import argparse
import sys

import mysql.connector

ADMIN_PATTERN = r"^(admin_|recon_)"
CASHIER_PREFIX = "cashier_"
IGNORED_TABLES = frozenset({"flyway_schema_history"})


def fetch_violations(cur, schema: str, pattern_sql: str, pattern_args: tuple) -> list[str]:
    cur.execute(
        """
        SELECT TABLE_NAME FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = %s AND TABLE_TYPE = 'BASE TABLE'
        """
        + pattern_sql,
        (schema,) + pattern_args,
    )
    return [row[0] for row in cur.fetchall() if row[0] not in IGNORED_TABLES]


def main() -> int:
    p = argparse.ArgumentParser(description="校验数据库表前缀")
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
        use_pure=True,
    )
    cur = conn.cursor()
    try:
        admin_bad = fetch_violations(
            cur,
            "payflow_admin",
            " AND TABLE_NAME NOT REGEXP %s",
            (ADMIN_PATTERN,),
        )
        cashier_bad = fetch_violations(
            cur,
            "payflow_cashier",
            " AND TABLE_NAME NOT LIKE %s",
            (CASHIER_PREFIX + "%",),
        )
    finally:
        cur.close()
        conn.close()

    ok = True
    if admin_bad:
        ok = False
        print("payflow_admin 违规表:", ", ".join(admin_bad))
    if cashier_bad:
        ok = False
        print("payflow_cashier 违规表:", ", ".join(cashier_bad))
    if ok:
        print("表前缀校验通过。")
        return 0
    return 1


if __name__ == "__main__":
    sys.exit(main())
