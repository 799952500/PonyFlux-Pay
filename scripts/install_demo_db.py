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
    ROOT / "sql/seed/payflow_cashier_merchant_notify_demo.sql",
    ROOT / "sql/seed/payflow_admin_seed.sql",
]

FLYWAY_ADMIN_MIGRATIONS = [
    ("1", "<< Flyway Baseline >>", "BASELINE", "<< Flyway Baseline >>"),
    ("2", "rename admin tables", "SQL", "V2__rename_admin_tables.sql"),
    ("3", "union channel seed", "SQL", "V3__union_channel_seed.sql"),
    ("4", "plan extensions", "SQL", "V4__plan_extensions.sql"),
    ("5", "admin feature menus", "SQL", "V5__admin_feature_menus.sql"),
    ("6", "dashboard and routing", "SQL", "V6__dashboard_and_routing.sql"),
    ("7", "merchant data isolation governance", "SQL", "V7__merchant_data_isolation_governance.sql"),
    ("8", "button permissions", "SQL", "V8__button_permissions.sql"),
    ("9", "notification center", "SQL", "V9__notification_center.sql"),
    ("10", "recon diff workflow", "SQL", "V10__recon_diff_workflow.sql"),
    ("11", "recon workflow menus", "SQL", "V11__recon_workflow_menus.sql"),
]

FLYWAY_CASHIER_MIGRATIONS = [
    ("1", "<< Flyway Baseline >>", "BASELINE", "<< Flyway Baseline >>"),
    ("2", "rename tables", "SQL", "V2__rename_tables.sql"),
    ("3", "payments account code", "SQL", "V3__payments_account_code.sql"),
    ("4", "cashier security audit", "SQL", "V4__cashier_security_audit.sql"),
    ("5", "webhook tables", "SQL", "V5__webhook_tables.sql"),
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


def seed_flyway_history(cur, database: str, migrations: list[tuple[str, str, str, str]]) -> None:
    """将 Flyway 历史标记为已执行，避免 install 终态 schema 与增量迁移冲突。"""
    cur.execute(f"USE `{database}`")
    cur.execute(
        """
        CREATE TABLE IF NOT EXISTS flyway_schema_history (
            installed_rank INT NOT NULL,
            version VARCHAR(50),
            description VARCHAR(200) NOT NULL,
            type VARCHAR(20) NOT NULL,
            script VARCHAR(1000) NOT NULL,
            checksum INT,
            installed_by VARCHAR(100) NOT NULL,
            installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            execution_time INT NOT NULL,
            success TINYINT(1) NOT NULL,
            PRIMARY KEY (installed_rank)
        ) ENGINE=InnoDB
        """
    )
    cur.execute("DELETE FROM flyway_schema_history")
    for rank, (version, description, mig_type, script) in enumerate(migrations, start=1):
        cur.execute(
            """
            INSERT INTO flyway_schema_history
            (installed_rank, version, description, type, script, checksum,
             installed_by, installed_on, execution_time, success)
            VALUES (%s, %s, %s, %s, %s, NULL, 'install_demo_db', NOW(), 0, 1)
            """,
            (rank, version, description, mig_type, script),
        )


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

        seed_flyway_history(cur, "payflow_admin", FLYWAY_ADMIN_MIGRATIONS)
        seed_flyway_history(cur, "payflow_cashier", FLYWAY_CASHIER_MIGRATIONS)
        print("OK  flyway_schema_history (admin + cashier)")

        print(f"\n演示库安装完成，共执行 {total} 条语句。")

        print("管理后台登录: admin / admin123")

        print("校验: python scripts/validate_table_prefixes.py")

    finally:

        cur.close()

        conn.close()





if __name__ == "__main__":

    main()


