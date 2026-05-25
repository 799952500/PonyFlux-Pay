-- Webhook 投递日志与端点（与 WebhookRetryTask / WebhookDispatchService 对齐）
USE payflow_cashier;

CREATE TABLE IF NOT EXISTS `cashier_merchant_webhook_endpoint` (
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

CREATE TABLE IF NOT EXISTS `cashier_webhook_delivery_log` (
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
  KEY `idx_merchant_event` (`merchant_id`, `event_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
