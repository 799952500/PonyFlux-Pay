-- ================================================
-- PonyFlux-Pay 运营后台数据库 Schema (MySQL 8)
-- ================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table: admin_users 管理员账号表
-- ----------------------------
DROP TABLE IF EXISTS `admin_users`;
CREATE TABLE `admin_users` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `password` VARCHAR(256) NOT NULL COMMENT '密码(BCrypt加密)',
  `role` VARCHAR(32) DEFAULT NULL COMMENT '角色 SUPER_ADMIN/ADMIN/FINANCE/RISK',
  `nickname` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
  `status` VARCHAR(16) DEFAULT NULL COMMENT '状态 ACTIVE/DISABLED',
  `data_merchant_ids` VARCHAR(512) DEFAULT NULL COMMENT '数据权限：可见商户号列表，逗号分隔；空则仅 SUPER_ADMIN 看全量',
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员账号表';

-- ----------------------------
-- Table: admin_audit_logs 操作审计
-- ----------------------------
DROP TABLE IF EXISTS `admin_audit_logs`;
CREATE TABLE `admin_audit_logs` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(64) DEFAULT NULL COMMENT '操作者',
  `action` VARCHAR(32) DEFAULT NULL COMMENT 'HTTP 方法',
  `resource_path` VARCHAR(512) DEFAULT NULL COMMENT '请求路径',
  `detail` VARCHAR(1024) DEFAULT NULL COMMENT '摘要',
  `client_ip` VARCHAR(64) DEFAULT NULL COMMENT '客户端 IP',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  KEY `idx_audit_created` (`created_at`),
  KEY `idx_audit_username` (`username`),
  KEY `idx_audit_action` (`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理端审计日志';

-- ----------------------------
-- Table: channels 支付渠道表
-- ----------------------------
DROP TABLE IF EXISTS `admin_channels`;
CREATE TABLE `admin_channels` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `channel_code` VARCHAR(64) NOT NULL COMMENT '渠道编码',
  `channel_name` VARCHAR(128) NOT NULL COMMENT '渠道名称',
  `channel_type` VARCHAR(32) DEFAULT NULL COMMENT '渠道类型 WECHAT/ALIPAY/UNION/CARD',
  `api_url` VARCHAR(256) DEFAULT NULL COMMENT 'API地址',
  `api_key` VARCHAR(256) DEFAULT NULL COMMENT '渠道密钥',
  `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
  `priority` INT DEFAULT 0 COMMENT '优先级',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  UNIQUE KEY `uk_channel_code` (`channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付渠道表';

-- ----------------------------
-- Table: merchants 商户表
-- ----------------------------
DROP TABLE IF EXISTS `admin_merchants`;
CREATE TABLE `admin_merchants` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `merchant_id` VARCHAR(64) NOT NULL COMMENT '商户号',
  `merchant_name` VARCHAR(128) NOT NULL COMMENT '商户名称',
  `merchant_key` VARCHAR(256) DEFAULT NULL COMMENT '商户密钥',
  `callback_url` VARCHAR(256) DEFAULT NULL COMMENT '支付结果回调地址',
  `notify_url` VARCHAR(256) DEFAULT NULL COMMENT '通知地址',
  `commission_rate` DECIMAL(6,4) DEFAULT NULL COMMENT '手续费分成比例',
  `status` VARCHAR(16) DEFAULT NULL COMMENT '状态 ACTIVE/SUSPENDED',
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  UNIQUE KEY `uk_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户表';

-- ----------------------------
-- Table: payment_methods 支付方式表
-- ----------------------------
DROP TABLE IF EXISTS `admin_payment_methods`;
CREATE TABLE `admin_payment_methods` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `method_code` VARCHAR(64) NOT NULL COMMENT '支付方式编码',
  `method_name` VARCHAR(128) NOT NULL COMMENT '支付方式名称',
  `channel_id` BIGINT DEFAULT NULL COMMENT '所属渠道ID',
  `app_id` VARCHAR(128) DEFAULT NULL COMMENT '应用ID',
  `app_secret` VARCHAR(256) DEFAULT NULL COMMENT '应用密钥',
  `mch_id` VARCHAR(64) DEFAULT NULL COMMENT '商户号',
  `mch_key` VARCHAR(256) DEFAULT NULL COMMENT '商户密钥',
  `cert_path` VARCHAR(256) DEFAULT NULL COMMENT '证书路径',
  `cert_password` VARCHAR(128) DEFAULT NULL COMMENT '证书密码',
  `config_json` TEXT DEFAULT NULL COMMENT '扩展配置JSON',
  `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
  `priority` INT DEFAULT 0 COMMENT '优先级',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  UNIQUE KEY `uk_method_code` (`method_code`),
  KEY `idx_channel_id` (`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付方式表';

-- ----------------------------
-- Table: merchant_payment_methods 商户支付方式绑定表
-- ----------------------------
DROP TABLE IF EXISTS `admin_merchant_payment_methods`;
CREATE TABLE `admin_merchant_payment_methods` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `merchant_id` BIGINT DEFAULT NULL COMMENT '商户ID',
  `payment_method_id` BIGINT DEFAULT NULL COMMENT '支付方式ID',
  `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
  `priority` INT DEFAULT 0 COMMENT '优先级',
  `custom_config_json` TEXT DEFAULT NULL COMMENT '自定义配置JSON',
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_payment_method_id` (`payment_method_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户支付方式绑定表';

-- ----------------------------
-- Table: payment_accounts 支付账号（收款账户池）
-- ----------------------------
DROP TABLE IF EXISTS `admin_payment_accounts`;
CREATE TABLE `admin_payment_accounts` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `channel_id` BIGINT DEFAULT NULL COMMENT '所属渠道ID',
  `account_code` VARCHAR(64) NOT NULL COMMENT '账号编码(如1001/2001)',
  `account_name` VARCHAR(128) NOT NULL COMMENT '账号名称',
  `app_id` VARCHAR(128) DEFAULT NULL COMMENT '应用ID',
  `app_secret` VARCHAR(256) DEFAULT NULL COMMENT '应用密钥',
  `mch_id` VARCHAR(64) DEFAULT NULL COMMENT '商户号',
  `mch_key` VARCHAR(256) DEFAULT NULL COMMENT '商户密钥',
  `cert_path` VARCHAR(256) DEFAULT NULL COMMENT '证书路径',
  `cert_password` VARCHAR(128) DEFAULT NULL COMMENT '证书密码',
  `config_json` TEXT DEFAULT NULL COMMENT '扩展配置JSON',
  `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
  `priority` INT DEFAULT 0 COMMENT '优先级',
  `description` VARCHAR(512) DEFAULT NULL COMMENT '描述',
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  UNIQUE KEY `uk_account_code` (`account_code`),
  KEY `idx_account_channel_id` (`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付账号表';

-- ----------------------------
-- Table: merchant_payment_routes 商户支付路由表(方式+账号)
-- ----------------------------
DROP TABLE IF EXISTS `admin_merchant_payment_routes`;
CREATE TABLE `admin_merchant_payment_routes` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `merchant_id` VARCHAR(64) NOT NULL COMMENT '商户号',
  `payment_method_id` BIGINT NOT NULL COMMENT '支付方式ID',
  `payment_account_id` BIGINT NOT NULL COMMENT '支付账号ID',
  `enabled` TINYINT(1) DEFAULT 1 COMMENT '是否启用',
  `priority` INT DEFAULT 0 COMMENT '优先级',
  `client_scopes` VARCHAR(64) NOT NULL DEFAULT 'PC,H5,APP' COMMENT '终端可见：PC,H5,APP 逗号分隔',
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  UNIQUE KEY `uk_merchant_method_account` (`merchant_id`, `payment_method_id`, `payment_account_id`),
  KEY `idx_route_merchant_id` (`merchant_id`),
  KEY `idx_route_method_id` (`payment_method_id`),
  KEY `idx_route_account_id` (`payment_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户支付路由表';

-- ----------------------------
-- 对账模块 recon_*（与 admin-alter-202605-recon.sql 一致）
-- ----------------------------
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
  `suggested_action` VARCHAR(128) DEFAULT NULL COMMENT 'AUTO_QUERY/MANUAL_REVIEW 等',
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

SET FOREIGN_KEY_CHECKS = 1;
