-- 已有库增量变更（列已存在时 ALTER 会报错，可跳过该语句）
ALTER TABLE `admin_users`
  ADD COLUMN `data_merchant_ids` VARCHAR(512) DEFAULT NULL COMMENT '数据权限：可见商户号列表，逗号分隔' AFTER `status`;

CREATE TABLE IF NOT EXISTS `admin_audit_logs` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(64) DEFAULT NULL,
  `action` VARCHAR(32) DEFAULT NULL,
  `resource_path` VARCHAR(512) DEFAULT NULL,
  `detail` VARCHAR(1024) DEFAULT NULL,
  `client_ip` VARCHAR(64) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_audit_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理端审计日志';
