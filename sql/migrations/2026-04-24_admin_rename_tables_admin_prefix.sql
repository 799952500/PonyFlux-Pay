-- ============================================================
-- admin 数据库：表名统一迁移脚本（MySQL 8）
-- 目标：按域前缀统一表名（admin_* / sys_*）
-- 注意：请在业务停机窗口执行，并提前备份数据库
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- admin 域（admin_users 已符合 admin_* 命名，不改名）
RENAME TABLE
  `channels` TO `admin_channels`,
  `merchants` TO `admin_merchants`,
  `payment_methods` TO `admin_payment_methods`,
  `merchant_payment_methods` TO `admin_merchant_payment_methods`,
  `payment_accounts` TO `admin_payment_accounts`,
  `merchant_payment_routes` TO `admin_merchant_payment_routes`;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 可选：兼容视图（给旧表名“留口子”）
-- 若你希望强制全量改造、禁止旧表名访问，可删除以下视图部分。
-- 注意：MySQL 不允许视图与同名表共存，因此只能在 RENAME 完成后创建。
-- ============================================================

CREATE OR REPLACE VIEW `channels` AS SELECT * FROM `admin_channels`;
CREATE OR REPLACE VIEW `merchants` AS SELECT * FROM `admin_merchants`;
CREATE OR REPLACE VIEW `payment_methods` AS SELECT * FROM `admin_payment_methods`;
CREATE OR REPLACE VIEW `merchant_payment_methods` AS SELECT * FROM `admin_merchant_payment_methods`;
CREATE OR REPLACE VIEW `payment_accounts` AS SELECT * FROM `admin_payment_accounts`;
CREATE OR REPLACE VIEW `merchant_payment_routes` AS SELECT * FROM `admin_merchant_payment_routes`;

