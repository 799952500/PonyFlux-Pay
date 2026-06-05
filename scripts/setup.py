#!/usr/bin/env python3
"""
PonyFlux Pay 一键初始化（客户交付入口）

步骤：环境检测 →（可选）启动 Redis → 重置并安装演示库 → 校验 → 构建后端/前端依赖

用法:
  python scripts/setup.py
  python scripts/setup.py --db-only
  python scripts/setup.py --no-reset --skip-build
  python scripts/setup.py --host 127.0.0.1 --user root --password root
"""

from __future__ import annotations

import argparse
import os
import platform
import shutil
import socket
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

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

ADMIN_PATTERN = r"^(admin_|recon_)"
CASHIER_PREFIX = "cashier_"
IGNORED_TABLES = frozenset({"flyway_schema_history"})
ESSENTIAL_ADMIN_TABLES = (
    "admin_users",
    "admin_channels",
    "admin_payment_methods",
    "admin_notifications",
    "admin_churn_alert",
)
ESSENTIAL_CASHIER_TABLES = ("cashier_orders", "cashier_payments")
ADMIN_BCRYPT = "$2b$10$UHTRg4BSLqaHosl88JbOE.WOCrOmMusFph5Jws0aEEOKrMPq4Px5a"


def _import_deps():
    try:
        import mysql.connector  # noqa: F401
        import bcrypt  # noqa: F401
    except ImportError as exc:
        print("缺少 Python 依赖，请先执行：")
        print("  pip install -r scripts/requirements.txt")
        raise SystemExit(1) from exc


def split_sql(text: str) -> list[str]:
    out: list[str] = []
    buf: list[str] = []
    in_single = False
    in_double = False
    escape = False

    for ch in text:
        if escape:
            buf.append(ch)
            escape = False
            continue
        if ch == "\\":
            buf.append(ch)
            escape = True
            continue
        if ch == "'" and not in_double:
            in_single = not in_single
            buf.append(ch)
            continue
        if ch == '"' and not in_single:
            in_double = not in_double
            buf.append(ch)
            continue
        if ch == ";" and not in_single and not in_double:
            stmt = "".join(buf).strip()
            buf = []
            if stmt:
                out.append(stmt)
            continue
        buf.append(ch)

    tail = "".join(buf).strip()
    if tail:
        out.append(tail)
    return out


def is_comment_only(stmt: str) -> bool:
    for line in stmt.splitlines():
        s = line.strip()
        if not s:
            continue
        if s.startswith("--"):
            continue
        return False
    return True


def execute_file(cur, path: Path) -> int:
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


def port_open(host: str, port: int, timeout: float = 1.5) -> bool:
    try:
        with socket.create_connection((host, port), timeout=timeout):
            return True
    except OSError:
        return False


def log_ok(msg: str) -> None:
    print(f"[OK] {msg}")


def log_warn(msg: str) -> None:
    print(f"[WARN] {msg}")


def log_fail(msg: str) -> None:
    print(f"[FAIL] {msg}", file=sys.stderr)


def check_prerequisites(args: argparse.Namespace) -> None:
    print("=== 1/6 环境检测 ===")
    if sys.version_info < (3, 9):
        raise SystemExit("需要 Python 3.9+")

    if not port_open(args.host, args.port):
        log_fail(f"MySQL 未监听 {args.host}:{args.port}，请先启动 MySQL")
        raise SystemExit(1)
    log_ok(f"MySQL {args.host}:{args.port}")

    if args.skip_redis:
        log_warn("已跳过 Redis 检测")
    elif port_open("127.0.0.1", 6379):
        log_ok("Redis 127.0.0.1:6379")
    else:
        log_warn("Redis 6379 未启动，将尝试自动启动（Windows 常见路径 D:\\apps\\Redis）")
        try_start_redis()


def try_start_redis() -> None:
    candidates = [
        Path(r"D:\apps\Redis\redis-server.exe"),
        Path(r"C:\Program Files\Redis\redis-server.exe"),
    ]
    exe = shutil.which("redis-server")
    if exe:
        candidates.insert(0, Path(exe))

    for path in candidates:
        if not path or not path.is_file():
            continue
        flags = getattr(subprocess, "CREATE_NO_WINDOW", 0)
        subprocess.Popen([str(path)], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, creationflags=flags)
        for _ in range(10):
            time.sleep(0.5)
            if port_open("127.0.0.1", 6379):
                log_ok(f"已启动 Redis: {path}")
                return
    log_warn("未能自动启动 Redis；启动 cashier/admin 前请手动运行 redis-server")


def mysql_connect(args: argparse.Namespace):
    import mysql.connector

    return mysql.connector.connect(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        autocommit=True,
        use_pure=True,
    )


def reset_databases(cur) -> None:
    print("=== 2/6 重置数据库 ===")
    for db in ("payflow_admin", "payflow_cashier"):
        cur.execute(f"DROP DATABASE IF EXISTS `{db}`")
        log_ok(f"DROP DATABASE {db}")


def seed_flyway_history(cur, database: str, migrations: list[tuple[str, str, str, str]]) -> None:
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
            VALUES (%s, %s, %s, %s, %s, NULL, 'setup.py', NOW(), 0, 1)
            """,
            (rank, version, description, mig_type, script),
        )


def install_database(args: argparse.Namespace) -> None:
    print("=== 3/6 安装演示库（schema + seed）===")
    conn = mysql_connect(args)
    cur = conn.cursor()
    try:
        if args.reset:
            reset_databases(cur)

        total = 0
        for path in SQL_FILES:
            if not path.is_file():
                raise FileNotFoundError(path)
            n = execute_file(cur, path)
            total += n
            log_ok(f"{path.relative_to(ROOT)} ({n} statements)")

        seed_flyway_history(cur, "payflow_admin", FLYWAY_ADMIN_MIGRATIONS)
        seed_flyway_history(cur, "payflow_cashier", FLYWAY_CASHIER_MIGRATIONS)
        log_ok("flyway_schema_history (admin + cashier)")
        print(f"数据库安装完成，共 {total} 条语句。")
    finally:
        cur.close()
        conn.close()


def validate_database(args: argparse.Namespace) -> None:
    print("=== 4/6 校验数据库 ===")
    import bcrypt
    import mysql.connector

    conn = mysql_connect(args)
    cur = conn.cursor()
    try:
        for schema, tables in (
            ("payflow_admin", ESSENTIAL_ADMIN_TABLES),
            ("payflow_cashier", ESSENTIAL_CASHIER_TABLES),
        ):
            cur.execute(
                """
                SELECT TABLE_NAME FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = %s AND TABLE_TYPE = 'BASE TABLE'
                """,
                (schema,),
            )
            existing = {row[0] for row in cur.fetchall()}
            missing = [t for t in tables if t not in existing]
            if missing:
                raise SystemExit(f"{schema} 缺少表: {', '.join(missing)}")

        cur.execute(
            """
            SELECT TABLE_NAME FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = %s AND TABLE_TYPE = 'BASE TABLE'
              AND TABLE_NAME NOT REGEXP %s
            """,
            ("payflow_admin", ADMIN_PATTERN),
        )
        admin_bad = [r[0] for r in cur.fetchall() if r[0] not in IGNORED_TABLES]
        if admin_bad:
            raise SystemExit(f"payflow_admin 表前缀违规: {', '.join(admin_bad)}")

        cur.execute(
            """
            SELECT TABLE_NAME FROM information_schema.TABLES
            WHERE TABLE_SCHEMA = %s AND TABLE_TYPE = 'BASE TABLE'
              AND TABLE_NAME NOT LIKE %s
            """,
            ("payflow_cashier", CASHIER_PREFIX + "%"),
        )
        cashier_bad = [r[0] for r in cur.fetchall() if r[0] not in IGNORED_TABLES]
        if cashier_bad:
            raise SystemExit(f"payflow_cashier 表前缀违规: {', '.join(cashier_bad)}")

        cur.execute("USE payflow_admin")
        cur.execute("SELECT password FROM admin_users WHERE username = %s AND status = 'ACTIVE'", ("admin",))
        row = cur.fetchone()
        if not row:
            raise SystemExit("admin 用户不存在，seed 可能未正确导入")
        if not bcrypt.checkpw(b"admin123", row[0].encode("utf-8")):
            raise SystemExit("admin 密码校验失败，期望 admin123")

        log_ok("表结构、前缀与 admin/admin123 校验通过")
    finally:
        cur.close()
        conn.close()


def run_cmd(cmd: list[str], cwd: Path | None = None) -> None:
    print(f"  $ {' '.join(cmd)}")
    subprocess.run(cmd, cwd=cwd or ROOT, check=True)


def build_project(args: argparse.Namespace) -> None:
    print("=== 5/6 构建项目 ===")
    mvn = shutil.which("mvn")
    if not mvn:
        log_warn("未找到 mvn，跳过后端构建")
    else:
        run_cmd(
            [
                mvn,
                "-B",
                "-pl",
                "payflow-admin-server,payflow-cashier-server,payflow-recon-server",
                "-am",
                "-Dmaven.test.skip=true",
                "install",
            ]
        )
        log_ok("Maven 模块已安装")

    if args.skip_frontend:
        log_warn("已跳过前端 npm install")
        return

    npm = shutil.which("npm")
    if not npm:
        log_warn("未找到 npm，跳过前端依赖安装")
        return

    for client in ("payflow-admin-client", "payflow-cashier-client"):
        client_dir = ROOT / client
        if (client_dir / "package.json").is_file():
            run_cmd([npm, "install"], cwd=client_dir)
            log_ok(f"{client} npm install")


def print_next_steps() -> None:
    print("=== 6/6 完成 ===")
    print(
        """
演示环境已就绪。请在新终端分别启动：

  # 后端（各开一个终端）
  mvn -B -pl payflow-admin-server "-Dmaven.test.skip=true" spring-boot:run
  mvn -B -pl payflow-cashier-server spring-boot:run

  # 前端
  cd payflow-admin-client && npm run dev    # http://127.0.0.1:3001
  cd payflow-cashier-client && npm run dev  # http://127.0.0.1:5173

访问：
  管理后台  admin / admin123  → http://127.0.0.1:3001
  收银台演示                → http://127.0.0.1:5173/cashier/pc/demo
"""
    )


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="PonyFlux Pay 一键初始化")
    p.add_argument("--host", default="127.0.0.1")
    p.add_argument("--port", type=int, default=3306)
    p.add_argument("--user", default="root")
    p.add_argument("--password", default="root")
    p.add_argument(
        "--reset",
        action=argparse.BooleanOptionalAction,
        default=True,
        help="安装前删除 payflow_admin / payflow_cashier（默认开启，避免半套库）",
    )
    p.add_argument("--db-only", action="store_true", help="仅执行数据库步骤")
    p.add_argument("--skip-build", action="store_true", help="跳过 Maven/npm 构建")
    p.add_argument("--skip-frontend", action="store_true", help="跳过 npm install")
    p.add_argument("--skip-redis", action="store_true", help="跳过 Redis 检测与自启动")
    return p.parse_args()


def main() -> None:
    _import_deps()
    args = parse_args()
    os.chdir(ROOT)

    print("PonyFlux Pay Setup")
    print(f"平台: {platform.system()} | 项目: {ROOT}")
    check_prerequisites(args)
    install_database(args)
    validate_database(args)

    if args.db_only or args.skip_build:
        print_next_steps()
        return

    build_project(args)
    print_next_steps()


if __name__ == "__main__":
    main()
