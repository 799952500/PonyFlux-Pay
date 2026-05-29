-- =============================================================================
-- V9: 站内通知中心
-- =============================================================================

CREATE TABLE IF NOT EXISTS `admin_notifications` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `recipient_user_id` BIGINT NOT NULL COMMENT '接收人（admin_sys_users.id）',
  `merchant_id` VARCHAR(64) DEFAULT NULL COMMENT '商户隔离字段',
  `biz_type` VARCHAR(32) NOT NULL COMMENT '通知业务类型',
  `biz_key` VARCHAR(128) NOT NULL COMMENT '业务唯一键（幂等去重）',
  `title` VARCHAR(256) NOT NULL COMMENT '通知标题',
  `summary` VARCHAR(512) DEFAULT NULL COMMENT '正文摘要',
  `link` VARCHAR(512) DEFAULT NULL COMMENT '跳转URL（相对路径）',
  `read_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0=未读 1=已读',
  `read_at` DATETIME DEFAULT NULL COMMENT '标记已读时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notification_biz` (`biz_type`, `biz_key`, `recipient_user_id`),
  KEY `idx_notification_recipient` (`recipient_user_id`, `read_status`, `created_at` DESC),
  KEY `idx_notification_merchant` (`merchant_id`),
  KEY `idx_notification_cleanup` (`read_status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站内通知';
