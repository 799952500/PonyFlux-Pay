#!/usr/bin/env python3
"""手动应用 admin V6 迁移（当 Flyway 尚未自动执行时）。"""

from pathlib import Path

import mysql.connector

from run_mysql_sql import is_comment_only, split_sql

ROOT = Path(__file__).resolve().parents[1]
SQL_PATH = ROOT / "payflow-admin-server/src/main/resources/db/migration/admin/V6__dashboard_and_routing.sql"


def main() -> None:
    text = SQL_PATH.read_text(encoding="utf-8")
    stmts = split_sql(text)
    conn = mysql.connector.connect(
        host="127.0.0.1",
        port=3306,
        user="root",
        password="root",
        database="payflow_admin",
        autocommit=True,
        use_pure=True,
    )
    cur = conn.cursor()
    try:
        for st in stmts:
            if not st or is_comment_only(st):
                continue
            cur.execute(st)
            if getattr(cur, "with_rows", False):
                cur.fetchall()

        cur.execute(
            "SELECT COUNT(*) FROM flyway_schema_history WHERE version = %s",
            ("6",),
        )
        exists = cur.fetchone()[0] > 0
        if not exists:
            cur.execute(
                """
                INSERT INTO flyway_schema_history
                (installed_rank, version, description, type, script,
                 checksum, installed_by, installed_on, execution_time, success)
                SELECT COALESCE(MAX(installed_rank), 0) + 1,
                       '6', 'dashboard and routing', 'SQL',
                       'V6__dashboard_and_routing.sql',
                       NULL, 'manual', NOW(), 0, 1
                FROM flyway_schema_history
                """
            )
        print("V6 migration applied to payflow_admin")
    finally:
        cur.close()
        conn.close()


if __name__ == "__main__":
    main()
