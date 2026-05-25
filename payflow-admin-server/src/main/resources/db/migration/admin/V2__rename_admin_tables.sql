-- ============================================================
-- admin 数据库：表名统一迁移（幂等）
-- 空库 install_demo 已使用 admin_* 终态表名时跳过 RENAME，仅刷新兼容视图。
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

SET @legacy_channels := (
    SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'channels'
      AND table_type = 'BASE TABLE'
);

SET @ddl := IF(
    @legacy_channels > 0,
    'RENAME TABLE
      `channels` TO `admin_channels`,
      `merchants` TO `admin_merchants`,
      `payment_methods` TO `admin_payment_methods`,
      `merchant_payment_methods` TO `admin_merchant_payment_methods`,
      `payment_accounts` TO `admin_payment_accounts`,
      `merchant_payment_routes` TO `admin_merchant_payment_routes`',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;

CREATE OR REPLACE VIEW `channels` AS SELECT * FROM `admin_channels`;
CREATE OR REPLACE VIEW `merchants` AS SELECT * FROM `admin_merchants`;
CREATE OR REPLACE VIEW `payment_methods` AS SELECT * FROM `admin_payment_methods`;
CREATE OR REPLACE VIEW `merchant_payment_methods` AS SELECT * FROM `admin_merchant_payment_methods`;
CREATE OR REPLACE VIEW `payment_accounts` AS SELECT * FROM `admin_payment_accounts`;
CREATE OR REPLACE VIEW `merchant_payment_routes` AS SELECT * FROM `admin_merchant_payment_routes`;
