#!/usr/bin/env python3
"""创建或重置商户管理员账号（payflow_admin.admin_users + sys_user_roles）。"""
import argparse
import sys

import mysql.connector

# BCrypt(admin123)，与 sql/seed/payflow_admin_seed.sql 一致
BCRYPT_ADMIN123 = "$2b$10$UHTRg4BSLqaHosl88JbOE.WOCrOmMusFph5Jws0aEEOKrMPq4Px5a"

# 商户管理员可见菜单（与 specs/008 acceptance 一致）：
# 工作台(概览/通知)、交易、对账、支付账号、本商户支付配置(商户管理页)、风控
# 不含：进件、渠道管理、支付方式公共定义、阶梯费率、费率审计、路由日志、系统设置等
MERCHANT_MENU_IDS = (
    1, 2, 3,
    10, 11, 12,
    60, 61, 62, 63,
    25,
    31,
    32,
)


def main() -> int:
    parser = argparse.ArgumentParser(description="创建商户管理员")
    parser.add_argument("--username", default="merchant_demo", help="登录用户名")
    parser.add_argument("--password-hash", default=BCRYPT_ADMIN123, help="BCrypt 哈希（默认 admin123）")
    parser.add_argument("--merchant-id", default="M100001", help="授权商户号")
    parser.add_argument("--nickname", default="演示商户管理员")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=3306)
    parser.add_argument("--user", default="root")
    parser.add_argument("--password", default="root")
    args = parser.parse_args()

    conn = mysql.connector.connect(
        host=args.host,
        port=args.port,
        user=args.user,
        password=args.password,
        database="payflow_admin",
        autocommit=False,
    )
    cur = conn.cursor()

    try:
        cur.execute(
            "SELECT merchant_id, merchant_name FROM merchants WHERE merchant_id = %s",
            (args.merchant_id,),
        )
        row = cur.fetchone()
        if not row:
            print(f"错误：商户 {args.merchant_id} 不存在，请先执行 scripts/install_demo_db.py", file=sys.stderr)
            return 1

        cur.execute(
            """
            INSERT INTO sys_roles (role_code, role_name, description, status, created_at, updated_at)
            VALUES ('MERCHANT_ADMIN', '商户管理员', '商户数据隔离演示角色', 'ACTIVE', NOW(), NOW())
            ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), status = 'ACTIVE', updated_at = NOW()
            """
        )
        cur.execute("SELECT id FROM sys_roles WHERE role_code = 'MERCHANT_ADMIN'")
        role_id = cur.fetchone()[0]

        cur.execute("DELETE FROM sys_role_menus WHERE role_id = %s", (role_id,))
        for menu_id in MERCHANT_MENU_IDS:
            cur.execute(
                "INSERT IGNORE INTO sys_role_menus (role_id, menu_id, created_at) VALUES (%s, %s, NOW())",
                (role_id, menu_id),
            )

        cur.execute(
            """
            INSERT INTO admin_users (username, password, role, nickname, status, data_merchant_ids, created_at, updated_at)
            VALUES (%s, %s, 'ADMIN', %s, 'ACTIVE', %s, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
              password = VALUES(password),
              role = 'ADMIN',
              nickname = VALUES(nickname),
              status = 'ACTIVE',
              data_merchant_ids = VALUES(data_merchant_ids),
              updated_at = NOW()
            """,
            (args.username, args.password_hash, args.nickname, args.merchant_id),
        )
        cur.execute("SELECT id FROM admin_users WHERE username = %s", (args.username,))
        user_id = cur.fetchone()[0]

        cur.execute("DELETE FROM sys_user_roles WHERE user_id = %s", (user_id,))
        cur.execute(
            "INSERT INTO sys_user_roles (user_id, role_id, created_at) VALUES (%s, %s, NOW())",
            (user_id, role_id),
        )

        conn.commit()
        print("商户管理员已就绪：")
        print(f"  用户名: {args.username}")
        print("  密码:   admin123")
        print(f"  授权商户: {args.merchant_id} ({row[1]})")
        print(f"  角色: MERCHANT_ADMIN (role_id={role_id})")
        print("  登录: http://localhost:3001/login")
        return 0
    except mysql.connector.Error as e:
        conn.rollback()
        print(f"数据库错误: {e}", file=sys.stderr)
        return 1
    finally:
        cur.close()
        conn.close()


if __name__ == "__main__":
    raise SystemExit(main())
