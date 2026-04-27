-- ================================================
-- 新增表：admin_channel_routes（渠道路由）
-- 执行位置：payflow_admin 数据库
-- ================================================
USE payflow_admin;

CREATE TABLE IF NOT EXISTS `admin_channel_routes` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `merchant_id` VARCHAR(64) NOT NULL COMMENT '商户号',
  `channel_id` BIGINT NOT NULL COMMENT '渠道ID（关联 admin_channels.id）',
  `payment_account_id` BIGINT NOT NULL COMMENT '支付账号ID（关联 admin_payment_accounts.id）',
  `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
  `priority` INT DEFAULT 0 COMMENT '优先级（数字越小优先级越高）',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '路由描述',
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  UNIQUE KEY `uk_merchant_channel_account` (`merchant_id`, `channel_id`, `payment_account_id`),
  KEY `idx_route_merchant_id` (`merchant_id`),
  KEY `idx_route_channel_id` (`channel_id`),
  KEY `idx_route_account_id` (`payment_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户渠道路由表';
