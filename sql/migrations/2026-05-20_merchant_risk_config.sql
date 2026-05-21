-- =============================================================================
-- PonyFlux Pay — 商户级风控配置增量迁移
-- Date: 2026-05-20
-- =============================================================================
USE payflow_admin;
SET NAMES utf8mb4;

ALTER TABLE `risk_rules`
  ADD COLUMN `threshold_fen` BIGINT DEFAULT NULL COMMENT '阈值，金额类单位为分' AFTER `threshold`,
  ADD COLUMN `priority` INT NOT NULL DEFAULT 100 COMMENT '优先级，数值越小越先评估' AFTER `enabled`,
  ADD COLUMN `owner_type` VARCHAR(32) NOT NULL DEFAULT 'PLATFORM' COMMENT 'PLATFORM/MERCHANT' AFTER `priority`,
  ADD COLUMN `owner_merchant_id` VARCHAR(64) DEFAULT NULL COMMENT '商户自建规则归属商户' AFTER `owner_type`,
  ADD COLUMN `scope_type` VARCHAR(32) NOT NULL DEFAULT 'ALL_MERCHANTS' COMMENT 'ALL_MERCHANTS/SELECTED_MERCHANTS/OWNER_MERCHANT_ONLY' AFTER `owner_merchant_id`,
  ADD COLUMN `created_by` VARCHAR(64) DEFAULT NULL AFTER `description`,
  ADD COLUMN `updated_by` VARCHAR(64) DEFAULT NULL AFTER `created_by`,
  ADD KEY `idx_risk_rules_owner` (`owner_type`, `owner_merchant_id`),
  ADD KEY `idx_risk_rules_scope` (`scope_type`, `enabled`, `priority`);

UPDATE `risk_rules`
SET `threshold_fen` = CASE
      WHEN `unit` = 'CNY_FEN' THEN CAST(`threshold` AS SIGNED)
      WHEN `threshold` IS NULL THEN NULL
      ELSE CAST(`threshold` AS SIGNED)
    END,
    `owner_type` = 'PLATFORM',
    `owner_merchant_id` = NULL,
    `scope_type` = 'ALL_MERCHANTS',
    `priority` = 100
WHERE `threshold_fen` IS NULL OR `owner_type` = 'PLATFORM';

CREATE TABLE IF NOT EXISTS `admin_risk_rule_merchant_scope` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rule_id` BIGINT NOT NULL,
  `merchant_id` VARCHAR(64) NOT NULL,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_merchant` (`rule_id`, `merchant_id`),
  KEY `idx_scope_merchant` (`merchant_id`, `enabled`),
  KEY `idx_scope_rule` (`rule_id`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风控规则商户作用范围';

CREATE TABLE IF NOT EXISTS `admin_risk_hit_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `trace_id` VARCHAR(128) DEFAULT NULL,
  `merchant_id` VARCHAR(64) NOT NULL,
  `merchant_name` VARCHAR(128) DEFAULT NULL,
  `order_id` VARCHAR(64) DEFAULT NULL,
  `merchant_order_no` VARCHAR(128) DEFAULT NULL,
  `rule_id` BIGINT NOT NULL,
  `rule_code` VARCHAR(64) NOT NULL,
  `rule_name` VARCHAR(128) NOT NULL,
  `owner_type` VARCHAR(32) NOT NULL,
  `scope_type` VARCHAR(32) NOT NULL,
  `action` VARCHAR(32) NOT NULL,
  `decision` VARCHAR(32) NOT NULL,
  `hit_reason` VARCHAR(512) DEFAULT NULL,
  `request_summary` VARCHAR(1024) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_hit_merchant_created` (`merchant_id`, `created_at`),
  KEY `idx_hit_rule_created` (`rule_id`, `created_at`),
  KEY `idx_hit_decision_created` (`decision`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风控命中记录';

CREATE TABLE IF NOT EXISTS `admin_risk_rule_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rule_id` BIGINT NOT NULL,
  `operator_id` VARCHAR(64) DEFAULT NULL,
  `operator_name` VARCHAR(128) DEFAULT NULL,
  `operator_type` VARCHAR(32) NOT NULL,
  `merchant_id` VARCHAR(64) DEFAULT NULL,
  `operation_type` VARCHAR(32) NOT NULL,
  `before_summary` VARCHAR(1024) DEFAULT NULL,
  `after_summary` VARCHAR(1024) DEFAULT NULL,
  `client_ip` VARCHAR(64) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_rule_created` (`rule_id`, `created_at`),
  KEY `idx_audit_operator_created` (`operator_type`, `created_at`),
  KEY `idx_audit_merchant_created` (`merchant_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风控规则变更审计';
