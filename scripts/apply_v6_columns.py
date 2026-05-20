"""补全 V6 中 admin_* 列与兼容视图（表已创建但列未加时执行）。"""

import mysql.connector

STMTS = [
    """
    SET @col_exists := (
        SELECT COUNT(*) FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'admin_channels' AND COLUMN_NAME = 'fee_rate'
    )
    """,
    """
    SET @ddl := IF(@col_exists = 0,
        'ALTER TABLE admin_channels ADD COLUMN fee_rate DECIMAL(6,4) DEFAULT NULL COMMENT ''渠道默认手续费率''',
        'SELECT 1')
    """,
    "PREPARE stmt FROM @ddl",
    "EXECUTE stmt",
    "DEALLOCATE PREPARE stmt",
    """
    SET @col_exists := (
        SELECT COUNT(*) FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'admin_merchants' AND COLUMN_NAME = 'rate_calc_mode'
    )
    """,
    """
    SET @ddl := IF(@col_exists = 0,
        'ALTER TABLE admin_merchants ADD COLUMN rate_calc_mode VARCHAR(16) DEFAULT ''flat'' COMMENT ''费率计算模式''',
        'SELECT 1')
    """,
    "PREPARE stmt FROM @ddl",
    "EXECUTE stmt",
    "DEALLOCATE PREPARE stmt",
    """
    SET @col_exists := (
        SELECT COUNT(*) FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'admin_merchants' AND COLUMN_NAME = 'merchant_group'
    )
    """,
    """
    SET @ddl := IF(@col_exists = 0,
        'ALTER TABLE admin_merchants ADD COLUMN merchant_group VARCHAR(64) DEFAULT NULL COMMENT ''商户所属费率组''',
        'SELECT 1')
    """,
    "PREPARE stmt FROM @ddl",
    "EXECUTE stmt",
    "DEALLOCATE PREPARE stmt",
    "CREATE OR REPLACE VIEW `channels` AS SELECT * FROM `admin_channels`",
    "CREATE OR REPLACE VIEW `merchants` AS SELECT * FROM `admin_merchants`",
]


def main() -> None:
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
        for st in STMTS:
            cur.execute(st.strip())
            if getattr(cur, "with_rows", False):
                cur.fetchall()
        print("admin_* columns and views updated")
    finally:
        cur.close()
        conn.close()


if __name__ == "__main__":
    main()
