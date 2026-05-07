-- 商户对账子任务表（落在 payflow_admin 库）
-- 执行前请 USE payflow_admin;

CREATE TABLE IF NOT EXISTS `recon_merchant_task` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  `merchant_task_id` VARCHAR(64) NOT NULL COMMENT '商户对账任务号',
  `merchant_id` VARCHAR(64) NOT NULL COMMENT '商户编号',
  `bill_date` DATE NOT NULL COMMENT '账单日',
  `status` VARCHAR(32) NOT NULL COMMENT 'INIT/GENERATING/SUCCESS/FAIL',
  `payment_count` INT DEFAULT NULL COMMENT '成功支付笔数',
  `payment_amount_fen` BIGINT DEFAULT NULL COMMENT '成功支付金额合计（分）',
  `statement_object_key` VARCHAR(512) DEFAULT NULL COMMENT '对账单存储键（本地路径或对象键）',
  `statement_size` BIGINT DEFAULT NULL COMMENT '对账单文件字节数',
  `elapsed_ms` BIGINT DEFAULT NULL COMMENT '耗时毫秒',
  `error_msg` VARCHAR(1024) DEFAULT NULL COMMENT '失败原因',
  `triggered_by` VARCHAR(32) DEFAULT NULL COMMENT 'XXL_MASTER / MANUAL / POLLER',
  `xxl_log_id` BIGINT DEFAULT NULL COMMENT 'xxl-job 日志ID',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_recon_merchant_biz` (`merchant_id`, `bill_date`),
  UNIQUE KEY `uk_recon_merchant_task_id` (`merchant_task_id`),
  KEY `idx_recon_merchant_bill_date` (`bill_date`),
  KEY `idx_recon_merchant_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户对账子任务';
