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
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员账号表';

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
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  UNIQUE KEY `uk_merchant_method_account` (`merchant_id`, `payment_method_id`, `payment_account_id`),
  KEY `idx_route_merchant_id` (`merchant_id`),
  KEY `idx_route_method_id` (`payment_method_id`),
  KEY `idx_route_account_id` (`payment_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户支付路由表';

SET FOREIGN_KEY_CHECKS = 1;
