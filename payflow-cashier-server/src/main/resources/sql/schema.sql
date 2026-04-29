-- ================================================
-- PonyFlux-Pay 收银台数据库 Schema (MySQL 8)
-- ================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table: merchants 商户表
-- ----------------------------
DROP TABLE IF EXISTS `cashier_merchants`;
CREATE TABLE `cashier_merchants` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
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
  UNIQUE KEY `uk_merchant_id` (`merchant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户表';

-- ----------------------------
-- Table: orders 订单表
-- ----------------------------
DROP TABLE IF EXISTS `cashier_orders`;
CREATE TABLE `cashier_orders` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  `order_id` VARCHAR(64) NOT NULL COMMENT '平台订单号',
  `merchant_id` VARCHAR(64) DEFAULT NULL COMMENT '商户号',
  `merchant_order_no` VARCHAR(64) DEFAULT NULL COMMENT '商户侧订单号',
  `amount` BIGINT DEFAULT NULL COMMENT '订单金额(分)',
  `currency` VARCHAR(8) DEFAULT NULL COMMENT '币种',
  `pay_amount` BIGINT DEFAULT NULL COMMENT '实付金额(分)',
  `subject` VARCHAR(256) DEFAULT NULL COMMENT '订单标题',
  `body` VARCHAR(512) DEFAULT NULL COMMENT '订单详情',
  `attach` VARCHAR(512) DEFAULT NULL COMMENT '透传字段',
  `channel` VARCHAR(32) DEFAULT NULL COMMENT '支付渠道',
  `status` VARCHAR(16) DEFAULT NULL COMMENT '订单状态',
  `notify_url` VARCHAR(256) DEFAULT NULL COMMENT '异步通知地址',
  `merchant_notify_url` VARCHAR(256) DEFAULT NULL COMMENT '商户回调地址',
  `return_url` VARCHAR(256) DEFAULT NULL COMMENT '回跳地址',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `created_at` DATETIME DEFAULT NULL COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT NULL COMMENT '更新时间',
  `notify_status` VARCHAR(16) DEFAULT NULL COMMENT '回调状态',
  `notify_retry_count` INT DEFAULT 0 COMMENT '回调重试次数',
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- ----------------------------
-- Table: pay_channels 支付渠道表
-- ----------------------------
DROP TABLE IF EXISTS `cashier_channels`;
CREATE TABLE `cashier_channels` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `channel_code` VARCHAR(64) NOT NULL COMMENT '渠道编码',
  `channel_name` VARCHAR(128) NOT NULL COMMENT '渠道名称',
  `icon_url` VARCHAR(256) DEFAULT NULL COMMENT '图标URL',
  `status` VARCHAR(16) DEFAULT NULL COMMENT '状态',
  `sort_weight` INT DEFAULT 0 COMMENT '排序权重',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  UNIQUE KEY `uk_channel_code` (`channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付渠道表';

-- ----------------------------
-- Table: pay_channel_accounts 渠道账户表
-- ----------------------------
DROP TABLE IF EXISTS `cashier_channel_accounts`;
CREATE TABLE `cashier_channel_accounts` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `channel_id` BIGINT NOT NULL COMMENT '所属渠道ID',
  `account_code` VARCHAR(64) NOT NULL COMMENT '账户编码',
  `account_name` VARCHAR(128) NOT NULL COMMENT '账户名称',
  `channel_config` TEXT DEFAULT NULL COMMENT '渠道配置JSON',
  `status` VARCHAR(16) DEFAULT NULL COMMENT '状态',
  `remark` VARCHAR(256) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  UNIQUE KEY `uk_account_code` (`account_code`),
  KEY `idx_channel_id` (`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='渠道账户表';

-- ----------------------------
-- Table: pay_channel_merchant_routes 商户渠道路由表
-- ----------------------------
DROP TABLE IF EXISTS `cashier_channel_merchant_routes`;
CREATE TABLE `cashier_channel_merchant_routes` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `channel_account_id` BIGINT NOT NULL COMMENT '渠道账户ID',
  `merchant_id` VARCHAR(64) NOT NULL COMMENT '商户号',
  `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
  `priority` INT DEFAULT 0 COMMENT '优先级',
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_channel_account_id` (`channel_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户渠道路由表';

-- ----------------------------
-- Table: payments 支付记录表
-- ----------------------------
DROP TABLE IF EXISTS `cashier_payments`;
CREATE TABLE `cashier_payments` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  `payment_id` VARCHAR(64) NOT NULL COMMENT '支付记录ID',
  `order_id` VARCHAR(64) DEFAULT NULL COMMENT '关联订单号',
  `pay_channel` VARCHAR(32) DEFAULT NULL COMMENT '支付渠道',
  `pay_method` VARCHAR(32) DEFAULT NULL COMMENT '支付方式',
  `channel_transaction_id` VARCHAR(128) DEFAULT NULL COMMENT '第三方交易流水号',
  `amount` BIGINT DEFAULT NULL COMMENT '支付金额(分)',
  `status` VARCHAR(16) DEFAULT NULL COMMENT '支付状态',
  `created_at` DATETIME DEFAULT NULL COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT NULL COMMENT '更新时间',
  UNIQUE KEY `uk_payment_id` (`payment_id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

-- ----------------------------
-- Table: risk_rules 风控规则表
-- ----------------------------
DROP TABLE IF EXISTS `cashier_risk_rules`;
CREATE TABLE `cashier_risk_rules` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  `rule_code` VARCHAR(64) NOT NULL COMMENT '规则编码',
  `rule_name` VARCHAR(128) NOT NULL COMMENT '规则名称',
  `rule_type` VARCHAR(32) NOT NULL COMMENT '规则类型 AMOUNT_SINGLE/AMOUNT_DAILY/IP_LIMIT/MOBILE_LIMIT/CUSTOM',
  `threshold` DECIMAL(18,2) DEFAULT NULL COMMENT '阈值',
  `unit` VARCHAR(16) DEFAULT NULL COMMENT '单位',
  `action` VARCHAR(16) NOT NULL COMMENT '动作 REJECT/REVIEW',
  `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  UNIQUE KEY `uk_risk_rule_code` (`rule_code`),
  KEY `idx_risk_rule_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风控规则表';

-- ----------------------------
-- Table: refunds 退款记录表
-- ----------------------------
DROP TABLE IF EXISTS `cashier_refunds`;
CREATE TABLE `cashier_refunds` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  `refund_id` VARCHAR(64) NOT NULL COMMENT '退款记录ID',
  `payment_id` VARCHAR(64) NOT NULL COMMENT '关联支付记录ID',
  `order_id` VARCHAR(64) NOT NULL COMMENT '关联订单号',
  `pay_channel` VARCHAR(32) NOT NULL COMMENT '支付渠道',
  `refund_amount` BIGINT NOT NULL COMMENT '退款金额（分）',
  `reason` VARCHAR(512) DEFAULT NULL COMMENT '退款原因',
  `status` VARCHAR(16) NOT NULL DEFAULT 'REFUNDING' COMMENT '退款状态',
  `channel_refund_no` VARCHAR(128) DEFAULT NULL COMMENT '渠道退款单号',
  `merchant_refund_no` VARCHAR(64) DEFAULT NULL COMMENT '商户侧退款单号',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_refund_id` (`refund_id`),
  KEY `idx_payment_id` (`payment_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='退款记录表';

SET FOREIGN_KEY_CHECKS = 1;
