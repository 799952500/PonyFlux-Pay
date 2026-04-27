-- ================================================
-- 迁移脚本：新增支付账号与商户支付路由
-- 适用：已有 payflow_admin 数据库（不重建全库）
-- ================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 新增表：payment_accounts
-- ----------------------------
CREATE TABLE IF NOT EXISTS `payment_accounts` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `channel_id` BIGINT DEFAULT NULL COMMENT '所属渠道ID',
  `account_code` VARCHAR(64) NOT NULL COMMENT '账号编码(如1001/2001)',
  `account_name` VARCHAR(128) NOT NULL COMMENT '账号名称',
  `app_id` VARCHAR(128) DEFAULT NULL COMMENT '应用ID',
  `app_secret` VARCHAR(256) DEFAULT NULL COMMENT '应用密钥',
  `mch_id` VARCHAR(64) DEFAULT NULL COMMENT '商户号',
  `mch_key` VARCHAR(256) DEFAULT NULL COMMENT '商户密钥',
  `cert_path` VARCHAR(256) DEFAULT NULL COMMENT '证书路径',
  `cert_password` VARCHAR(128) DEFAULT NULL COMMENT '证书密码',
  `config_json` TEXT DEFAULT NULL COMMENT '扩展配置JSON',
  `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
  `priority` INT DEFAULT 0 COMMENT '优先级',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  UNIQUE KEY `uk_account_code` (`account_code`),
  KEY `idx_account_channel_id` (`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付账号表';

-- ----------------------------
-- 新增表：merchant_payment_routes
-- ----------------------------
CREATE TABLE IF NOT EXISTS `merchant_payment_routes` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `merchant_id` VARCHAR(64) NOT NULL COMMENT '商户号',
  `payment_method_id` BIGINT NOT NULL COMMENT '支付方式ID',
  `payment_account_id` BIGINT NOT NULL COMMENT '支付账号ID',
  `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
  `priority` INT DEFAULT 0 COMMENT '优先级',
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  UNIQUE KEY `uk_merchant_method_account` (`merchant_id`, `payment_method_id`, `payment_account_id`),
  KEY `idx_route_merchant_id` (`merchant_id`),
  KEY `idx_route_method_id` (`payment_method_id`),
  KEY `idx_route_account_id` (`payment_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户支付路由表';

-- ----------------------------
-- 回滚（如需）
-- DROP TABLE IF EXISTS `merchant_payment_routes`;
-- DROP TABLE IF EXISTS `payment_accounts`;

