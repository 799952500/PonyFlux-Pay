-- 风控 QLExpress 扩展（admin 库的表名）
-- 安全写法：仅当列不存在时添加
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'risk_rules' AND COLUMN_NAME = 'risk_expr');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `risk_rules` ADD COLUMN `risk_expr` VARCHAR(1024) NULL COMMENT ''QLExpress 表达式，rule_type=CUSTOM 且非空时优先于阈值逻辑'' AFTER `rule_type`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 风控黑名单
CREATE TABLE IF NOT EXISTS `cashier_risk_blacklist` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `entry_type` VARCHAR(32) NOT NULL COMMENT 'IP / MOBILE / DEVICE',
  `entry_value` VARCHAR(256) NOT NULL,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `remark` VARCHAR(512) NULL,
  `created_at` DATETIME NULL,
  `updated_at` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_value` (`entry_type`, `entry_value`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风控黑名单';

-- 商户进件 KYB
CREATE TABLE IF NOT EXISTS `merchant_application` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `application_no` VARCHAR(64) NOT NULL,
  `merchant_name` VARCHAR(128) NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SUBMITTED/APPROVED/REJECTED',
  `biz_license_no` VARCHAR(64) NULL,
  `contact_name` VARCHAR(64) NULL,
  `contact_phone` VARCHAR(32) NULL,
  `payload_json` TEXT NULL COMMENT '资质与表单 JSON',
  `reject_reason` VARCHAR(512) NULL,
  `created_at` DATETIME NULL,
  `updated_at` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_no` (`application_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `merchant_contract` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `merchant_id` VARCHAR(64) NOT NULL,
  `version` INT NOT NULL DEFAULT 1,
  `fee_rate_json` VARCHAR(1024) NULL,
  `effective_at` DATETIME NULL,
  `expire_at` DATETIME NULL,
  `created_at` DATETIME NULL,
  PRIMARY KEY (`id`),
  KEY `idx_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 开放平台应用（双密钥）
CREATE TABLE IF NOT EXISTS `merchant_open_app` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `merchant_id` VARCHAR(64) NOT NULL,
  `app_id` VARCHAR(64) NOT NULL,
  `app_name` VARCHAR(128) NULL,
  `secret_current` VARCHAR(512) NOT NULL,
  `secret_previous` VARCHAR(512) NULL,
  `secret_previous_expire_at` DATETIME NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  `created_at` DATETIME NULL,
  `updated_at` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app` (`app_id`),
  KEY `idx_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Webhook
CREATE TABLE IF NOT EXISTS `merchant_webhook_endpoint` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `merchant_id` VARCHAR(64) NOT NULL,
  `url` VARCHAR(512) NOT NULL,
  `secret` VARCHAR(256) NOT NULL,
  `event_codes` VARCHAR(512) NOT NULL COMMENT '逗号分隔事件',
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` DATETIME NULL,
  PRIMARY KEY (`id`),
  KEY `idx_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `webhook_delivery_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `merchant_id` VARCHAR(64) NOT NULL,
  `endpoint_id` BIGINT NOT NULL,
  `event_code` VARCHAR(64) NOT NULL,
  `payload_json` MEDIUMTEXT NULL,
  `http_status` INT NULL,
  `response_body` VARCHAR(2048) NULL,
  `attempt` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  `created_at` DATETIME NULL,
  PRIMARY KEY (`id`),
  KEY `idx_merchant_event` (`merchant_id`, `event_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Payment Link
CREATE TABLE IF NOT EXISTS `payment_link` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `link_id` VARCHAR(32) NOT NULL,
  `merchant_id` VARCHAR(64) NOT NULL,
  `title` VARCHAR(256) NOT NULL,
  `amount` BIGINT NULL COMMENT '空表示用户输入金额',
  `currency` VARCHAR(8) NOT NULL DEFAULT 'CNY',
  `max_use` INT NULL,
  `used_count` INT NOT NULL DEFAULT 0,
  `expire_at` DATETIME NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  `created_at` DATETIME NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_link` (`link_id`),
  KEY `idx_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 对账差异建议动作（运营工作台展示）
-- 安全写法：仅当列不存在时添加
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'recon_diff' AND COLUMN_NAME = 'suggested_action');
SET @sql = IF(@col_exists = 0, 'ALTER TABLE `recon_diff` ADD COLUMN `suggested_action` VARCHAR(128) NULL COMMENT ''AUTO_QUERY/REVIEW/MANUAL 等'' AFTER `handle_remark`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
