-- 对账模块（落在 payflow_admin 库）
-- 执行前请 USE payflow_admin;

CREATE TABLE IF NOT EXISTS `recon_task` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  `task_id` VARCHAR(64) NOT NULL COMMENT '业务任务号',
  `channel` VARCHAR(32) NOT NULL COMMENT '对账渠道编码：alipay / wxpay',
  `account_code` VARCHAR(64) NOT NULL COMMENT '渠道账户编码',
  `bill_date` DATE NOT NULL COMMENT '账单日',
  `bill_type` VARCHAR(32) NOT NULL DEFAULT 'trade' COMMENT '账单类型',
  `status` VARCHAR(32) NOT NULL COMMENT 'INIT/DOWNLOADING/PARSING/COMPARING/SUCCESS/FAIL',
  `file_object_key` VARCHAR(512) DEFAULT NULL COMMENT '对象存储或本地路径键',
  `file_size` BIGINT DEFAULT NULL COMMENT '原始文件字节数',
  `bill_total_count` INT DEFAULT NULL COMMENT '账单明细条数',
  `bill_total_amount` BIGINT DEFAULT NULL COMMENT '账单金额合计（分）',
  `local_total_count` INT DEFAULT NULL COMMENT '本地参与比对条数',
  `local_total_amount` BIGINT DEFAULT NULL COMMENT '本地金额合计（分）',
  `diff_count` INT NOT NULL DEFAULT 0 COMMENT '差异条数',
  `elapsed_ms` BIGINT DEFAULT NULL COMMENT '耗时毫秒',
  `error_msg` VARCHAR(1024) DEFAULT NULL COMMENT '失败原因',
  `triggered_by` VARCHAR(32) DEFAULT NULL COMMENT 'XXL_JOB / MANUAL / API',
  `xxl_log_id` BIGINT DEFAULT NULL COMMENT 'xxl-job 日志ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_recon_task_biz` (`channel`, `account_code`, `bill_date`, `bill_type`),
  UNIQUE KEY `uk_recon_task_id` (`task_id`),
  KEY `idx_recon_task_bill_date` (`bill_date`),
  KEY `idx_recon_task_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对账任务';

CREATE TABLE IF NOT EXISTS `recon_bill_record` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `task_id` VARCHAR(64) NOT NULL COMMENT '关联 recon_task.task_id',
  `channel` VARCHAR(32) NOT NULL,
  `channel_trade_no` VARCHAR(128) DEFAULT NULL COMMENT '第三方交易号',
  `out_trade_no` VARCHAR(128) DEFAULT NULL COMMENT '商户订单号/平台订单号',
  `amount_fen` BIGINT DEFAULT NULL COMMENT '金额（分）',
  `refund_fen` BIGINT DEFAULT NULL COMMENT '退款金额（分）',
  `channel_status` VARCHAR(64) DEFAULT NULL,
  `finish_time` DATETIME DEFAULT NULL,
  `raw_line` TEXT COMMENT '原始行',
  `parse_error` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_recon_bill_task` (`task_id`),
  KEY `idx_recon_bill_trade` (`channel_trade_no`),
  KEY `idx_recon_bill_out` (`out_trade_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='三方账单明细';

CREATE TABLE IF NOT EXISTS `recon_diff` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `task_id` VARCHAR(64) NOT NULL,
  `diff_type` VARCHAR(32) NOT NULL COMMENT 'CHANNEL_ONLY/LOCAL_ONLY/AMOUNT_MISMATCH/STATUS_MISMATCH',
  `channel_trade_no` VARCHAR(128) DEFAULT NULL,
  `local_order_id` VARCHAR(64) DEFAULT NULL,
  `channel_amount` BIGINT DEFAULT NULL,
  `local_amount` BIGINT DEFAULT NULL,
  `channel_status` VARCHAR(64) DEFAULT NULL,
  `local_status` VARCHAR(64) DEFAULT NULL,
  `handle_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSED/IGNORED',
  `handle_remark` VARCHAR(512) DEFAULT NULL,
  `handled_by` VARCHAR(64) DEFAULT NULL,
  `handled_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_recon_diff_task` (`task_id`),
  KEY `idx_recon_diff_type` (`diff_type`),
  KEY `idx_recon_diff_handle` (`handle_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对账差异';

CREATE TABLE IF NOT EXISTS `recon_handler_audit` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `diff_id` BIGINT NOT NULL,
  `action` VARCHAR(32) NOT NULL,
  `operator` VARCHAR(64) DEFAULT NULL,
  `detail` VARCHAR(1024) DEFAULT NULL,
  `client_ip` VARCHAR(64) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_recon_audit_diff` (`diff_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='差异处理审计';
