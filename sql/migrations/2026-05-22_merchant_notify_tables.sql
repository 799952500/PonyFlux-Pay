-- 商户回调记录（平台 → 商户），与 cashier_callback_logs（渠道入站）区分
USE payflow_cashier;

CREATE TABLE IF NOT EXISTS `cashier_merchant_notify` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `notify_id` VARCHAR(64) NOT NULL COMMENT '业务主键',
  `order_id` VARCHAR(64) NOT NULL COMMENT '平台订单号',
  `merchant_id` VARCHAR(64) NOT NULL COMMENT '商户号',
  `merchant_order_no` VARCHAR(128) DEFAULT NULL COMMENT '商户订单号',
  `notify_type` VARCHAR(16) NOT NULL COMMENT 'PAYMENT/REFUND',
  `notify_url` VARCHAR(512) DEFAULT NULL COMMENT '回调地址快照',
  `summary_status` VARCHAR(16) NOT NULL COMMENT 'NOT_CONFIGURED/PENDING/IN_PROGRESS/SUCCESS/FAILED',
  `attempt_count` INT NOT NULL DEFAULT 0 COMMENT '累计尝试次数',
  `last_attempt_at` DATETIME DEFAULT NULL COMMENT '最近尝试时间',
  `last_fail_reason` VARCHAR(256) DEFAULT NULL COMMENT '最近失败摘要',
  `last_response_preview` VARCHAR(512) DEFAULT NULL COMMENT '最近响应预览',
  `order_status_snapshot` VARCHAR(16) DEFAULT NULL COMMENT '订单状态快照',
  `notify_payload_status` VARCHAR(16) DEFAULT NULL COMMENT '通知报文业务状态',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notify_id` (`notify_id`),
  UNIQUE KEY `uk_order_notify_type` (`order_id`, `notify_type`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_summary_status` (`summary_status`),
  KEY `idx_last_attempt_at` (`last_attempt_at`),
  KEY `idx_merchant_order_no` (`merchant_id`, `merchant_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户回调汇总';

CREATE TABLE IF NOT EXISTS `cashier_merchant_notify_attempt` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `notify_id` VARCHAR(64) NOT NULL COMMENT '关联汇总 notify_id',
  `attempt_no` INT NOT NULL COMMENT '尝试序号',
  `request_params` TEXT COMMENT '请求参数 JSON',
  `response_body` TEXT COMMENT '商户响应原文',
  `http_status` INT DEFAULT NULL COMMENT 'HTTP 状态码',
  `result_status` VARCHAR(16) NOT NULL COMMENT 'SUCCESS/FAILED/IN_PROGRESS',
  `fail_reason_type` VARCHAR(32) DEFAULT NULL COMMENT '失败原因分类',
  `fail_reason_detail` VARCHAR(512) DEFAULT NULL COMMENT '失败详情',
  `duration_ms` INT DEFAULT NULL COMMENT '耗时毫秒',
  `truncated` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '报文是否截断',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '尝试时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notify_attempt` (`notify_id`, `attempt_no`),
  KEY `idx_notify_id` (`notify_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户回调明细';
