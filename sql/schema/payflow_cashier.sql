-- =============================================================================
-- PonyFlux Pay — 收银台库 payflow_cashier 建表（全量 DDL）
-- 与 payflow-cashier-server 实体、Flyway cashier V1–V4 对齐
-- =============================================================================
USE payflow_cashier;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `cashier_security_audit`;
DROP TABLE IF EXISTS `cashier_merchant_notify_attempt`;
DROP TABLE IF EXISTS `cashier_merchant_notify`;
DROP TABLE IF EXISTS `cashier_refunds`;
DROP TABLE IF EXISTS `cashier_payments`;
DROP TABLE IF EXISTS `cashier_orders`;
DROP TABLE IF EXISTS `cashier_channel_merchant_routes`;
DROP TABLE IF EXISTS `cashier_channel_accounts`;
DROP TABLE IF EXISTS `cashier_channels`;
DROP TABLE IF EXISTS `cashier_merchants`;

CREATE TABLE `cashier_merchants` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_id` VARCHAR(64) NOT NULL COMMENT '商户号',
  `merchant_name` VARCHAR(128) NOT NULL COMMENT '商户名称',
  `password` VARCHAR(256) DEFAULT NULL COMMENT '登录密码',
  `app_secret` VARCHAR(256) DEFAULT NULL COMMENT '商户签名密钥',
  `status` VARCHAR(16) DEFAULT NULL COMMENT '状态',
  `allowed_channels` VARCHAR(256) DEFAULT NULL COMMENT '允许的支付渠道',
  `allowed_pay_methods` VARCHAR(256) DEFAULT NULL COMMENT '允许的支付方式',
  `contact` VARCHAR(64) DEFAULT NULL COMMENT '联系人',
  `phone` VARCHAR(32) DEFAULT NULL COMMENT '联系电话',
  `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
  `created_at` DATETIME DEFAULT NULL COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_id` (`merchant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户表';

CREATE TABLE `cashier_orders` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` VARCHAR(64) NOT NULL COMMENT '平台订单号',
  `merchant_id` VARCHAR(64) DEFAULT NULL COMMENT '商户号',
  `merchant_order_no` VARCHAR(64) DEFAULT NULL COMMENT '商户侧订单号',
  `amount` BIGINT NOT NULL COMMENT '订单金额(分)',
  `currency` VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '币种',
  `pay_amount` BIGINT DEFAULT NULL COMMENT '实付金额(分)',
  `subject` VARCHAR(256) DEFAULT NULL COMMENT '订单标题',
  `body` VARCHAR(512) DEFAULT NULL COMMENT '订单详情',
  `attach` VARCHAR(512) DEFAULT NULL COMMENT '透传字段',
  `channel` VARCHAR(32) DEFAULT NULL COMMENT '支付渠道',
  `status` VARCHAR(16) DEFAULT NULL COMMENT '订单状态',
  `notify_url` VARCHAR(256) DEFAULT NULL COMMENT '异步通知地址',
  `merchant_notify_url` VARCHAR(256) DEFAULT NULL COMMENT '商户回调地址',
  `return_url` VARCHAR(256) DEFAULT NULL COMMENT '回跳地址',
  `success_url` VARCHAR(256) DEFAULT NULL COMMENT '支付成功跳转',
  `fail_url` VARCHAR(256) DEFAULT NULL COMMENT '支付失败跳转',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `created_at` DATETIME DEFAULT NULL COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT NULL COMMENT '更新时间',
  `notify_status` VARCHAR(16) DEFAULT NULL COMMENT '回调状态',
  `notify_retry_count` INT DEFAULT 0 COMMENT '回调重试次数',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_merchant_merchant_order` (`merchant_id`, `merchant_order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

CREATE TABLE `cashier_channels` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `channel_code` VARCHAR(64) NOT NULL COMMENT '渠道编码',
  `channel_name` VARCHAR(128) NOT NULL COMMENT '渠道名称',
  `icon_url` VARCHAR(256) DEFAULT NULL COMMENT '图标URL',
  `fee_rate` DECIMAL(6,4) DEFAULT NULL COMMENT '渠道手续费率',
  `status` VARCHAR(16) DEFAULT NULL COMMENT '状态',
  `sort_weight` INT DEFAULT 0 COMMENT '排序权重',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_channel_code` (`channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付渠道表';

CREATE TABLE `cashier_channel_accounts` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `channel_id` BIGINT NOT NULL COMMENT '所属渠道ID',
  `account_code` VARCHAR(64) NOT NULL COMMENT '账户编码',
  `account_name` VARCHAR(128) NOT NULL COMMENT '账户名称',
  `channel_config` TEXT DEFAULT NULL COMMENT '渠道配置JSON',
  `status` VARCHAR(16) DEFAULT NULL COMMENT '状态',
  `remark` VARCHAR(256) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_code` (`account_code`),
  KEY `idx_channel_id` (`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='渠道账户表';

CREATE TABLE `cashier_channel_merchant_routes` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `channel_account_id` BIGINT NOT NULL COMMENT '渠道账户ID',
  `merchant_id` VARCHAR(64) NOT NULL COMMENT '商户号',
  `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
  `priority` INT DEFAULT 0 COMMENT '优先级',
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_channel_account_id` (`channel_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户渠道路由表';

CREATE TABLE `cashier_payments` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `payment_id` VARCHAR(64) NOT NULL COMMENT '支付记录ID',
  `order_id` VARCHAR(64) NOT NULL COMMENT '关联订单号',
  `pay_channel` VARCHAR(32) NOT NULL COMMENT '支付渠道',
  `account_code` VARCHAR(64) DEFAULT NULL COMMENT '收款渠道账户编码',
  `pay_method` VARCHAR(32) DEFAULT NULL COMMENT '支付方式',
  `channel_transaction_id` VARCHAR(128) DEFAULT NULL COMMENT '第三方交易流水号',
  `amount` BIGINT NOT NULL COMMENT '支付金额(分)',
  `status` VARCHAR(16) DEFAULT NULL COMMENT '支付状态',
  `created_at` DATETIME DEFAULT NULL COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_id` (`payment_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_pay_channel_status` (`pay_channel`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

CREATE TABLE `cashier_refunds` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `refund_id` VARCHAR(64) NOT NULL COMMENT '退款记录ID',
  `payment_id` VARCHAR(64) NOT NULL COMMENT '关联支付记录ID',
  `order_id` VARCHAR(64) NOT NULL COMMENT '关联订单号',
  `pay_channel` VARCHAR(32) NOT NULL COMMENT '支付渠道',
  `refund_amount` BIGINT NOT NULL COMMENT '退款金额（分）',
  `reason` VARCHAR(512) DEFAULT NULL COMMENT '退款原因',
  `status` VARCHAR(16) NOT NULL DEFAULT 'REFUNDING' COMMENT '退款状态',
  `channel_refund_no` VARCHAR(128) DEFAULT NULL COMMENT '渠道退款单号',
  `merchant_refund_no` VARCHAR(64) DEFAULT NULL COMMENT '商户侧退款单号',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_id` (`refund_id`),
  KEY `idx_payment_id` (`payment_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_status` (`status`),
  KEY `idx_order_status` (`order_id`, `status`),
  KEY `idx_payment_status` (`payment_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退款记录表';

CREATE TABLE `cashier_security_audit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `merchant_id` VARCHAR(64) NOT NULL COMMENT '调用方商户号',
  `target_merchant_id` VARCHAR(64) DEFAULT NULL COMMENT '请求体中声称的商户号',
  `auth_mode` VARCHAR(16) NOT NULL COMMENT 'JWT/HMAC/INTERNAL',
  `http_method` VARCHAR(10) NOT NULL COMMENT 'HTTP 方法',
  `request_path` VARCHAR(512) NOT NULL COMMENT '请求路径',
  `resource_type` VARCHAR(32) DEFAULT NULL COMMENT '资源类型',
  `resource_id` VARCHAR(64) DEFAULT NULL COMMENT '资源 ID',
  `client_ip` VARCHAR(64) DEFAULT NULL COMMENT '客户端 IP',
  `user_agent` VARCHAR(512) DEFAULT NULL COMMENT 'User-Agent',
  `outcome` VARCHAR(16) NOT NULL COMMENT 'DENIED',
  `reason_code` VARCHAR(16) NOT NULL COMMENT '5101/5102/5103',
  `reason_detail` VARCHAR(512) DEFAULT NULL COMMENT '内部原因',
  `created_at` DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '发生时间',
  PRIMARY KEY (`id`),
  KEY `idx_merchant_created` (`merchant_id`, `created_at`),
  KEY `idx_outcome_created` (`outcome`, `created_at`),
  KEY `idx_reason_created` (`reason_code`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户安全审计（越权拒绝）';

CREATE TABLE `cashier_merchant_notify` (
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

CREATE TABLE `cashier_merchant_notify_attempt` (
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

SET FOREIGN_KEY_CHECKS = 1;
