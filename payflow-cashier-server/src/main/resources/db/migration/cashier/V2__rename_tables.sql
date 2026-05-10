-- ============================================================
-- cashier 数据库：表名统一迁移脚本（MySQL 8）
-- 目标：按域前缀统一表名（cashier_*）
-- 注意：请在业务停机窗口执行，并提前备份数据库
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

RENAME TABLE
  `merchants` TO `cashier_merchants`,
  `orders` TO `cashier_orders`,
  `payments` TO `cashier_payments`,
  `callback_logs` TO `cashier_callback_logs`,
  `pay_channels` TO `cashier_channels`,
  `pay_channel_accounts` TO `cashier_channel_accounts`,
  `pay_channel_merchant_routes` TO `cashier_channel_merchant_routes`,
  `risk_rules` TO `cashier_risk_rules`;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 可选：兼容视图（给旧表名“留口子”）
-- 若你希望强制全量改造、禁止旧表名访问，可删除以下视图部分。
-- 注意：MySQL 不允许视图与同名表共存，因此只能在 RENAME 完成后创建。
-- ============================================================

CREATE OR REPLACE VIEW `merchants` AS SELECT * FROM `cashier_merchants`;
CREATE OR REPLACE VIEW `orders` AS SELECT * FROM `cashier_orders`;
CREATE OR REPLACE VIEW `payments` AS SELECT * FROM `cashier_payments`;
CREATE OR REPLACE VIEW `callback_logs` AS SELECT * FROM `cashier_callback_logs`;
CREATE OR REPLACE VIEW `pay_channels` AS SELECT * FROM `cashier_channels`;
CREATE OR REPLACE VIEW `pay_channel_accounts` AS SELECT * FROM `cashier_channel_accounts`;
CREATE OR REPLACE VIEW `pay_channel_merchant_routes` AS SELECT * FROM `cashier_channel_merchant_routes`;
CREATE OR REPLACE VIEW `risk_rules` AS SELECT * FROM `cashier_risk_rules`;

