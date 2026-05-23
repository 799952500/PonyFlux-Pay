-- =============================================================================
-- PonyFlux Pay — 运营库 payflow_admin 建表（全量 DDL）
-- 物理表 admin_* / sys_* / recon_*；兼容视图 channels、merchants 等
-- =============================================================================
USE payflow_admin;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP VIEW IF EXISTS `merchant_payment_routes`;
DROP VIEW IF EXISTS `payment_accounts`;
DROP VIEW IF EXISTS `merchant_payment_methods`;
DROP VIEW IF EXISTS `payment_methods`;
DROP VIEW IF EXISTS `merchants`;
DROP VIEW IF EXISTS `channels`;

DROP TABLE IF EXISTS `webhook_delivery_log`;
DROP TABLE IF EXISTS `merchant_webhook_endpoint`;
DROP TABLE IF EXISTS `merchant_open_app`;
DROP TABLE IF EXISTS `merchant_contract`;
DROP TABLE IF EXISTS `merchant_application`;
DROP TABLE IF EXISTS `payment_link`;
DROP TABLE IF EXISTS `cashier_risk_blacklist`;
DROP TABLE IF EXISTS `recon_routing_decision_log`;
DROP TABLE IF EXISTS `admin_fee_rate_audit_log`;
DROP TABLE IF EXISTS `admin_merchant_fee_snapshot`;
DROP TABLE IF EXISTS `admin_fee_rate_config`;
DROP TABLE IF EXISTS `admin_churn_alert`;
DROP TABLE IF EXISTS `admin_dashboard_metrics`;
DROP TABLE IF EXISTS `recon_handler_audit`;
DROP TABLE IF EXISTS `recon_diff`;
DROP TABLE IF EXISTS `recon_bill_record`;
DROP TABLE IF EXISTS `recon_merchant_task`;
DROP TABLE IF EXISTS `recon_task`;
DROP TABLE IF EXISTS `sys_user_roles`;
DROP TABLE IF EXISTS `sys_role_menus`;
DROP TABLE IF EXISTS `sys_users`;
DROP TABLE IF EXISTS `sys_menus`;
DROP TABLE IF EXISTS `sys_roles`;
DROP TABLE IF EXISTS `admin_channel_routes`;
DROP TABLE IF EXISTS `admin_merchant_payment_routes`;
DROP TABLE IF EXISTS `admin_merchant_payment_methods`;
DROP TABLE IF EXISTS `admin_payment_accounts`;
DROP TABLE IF EXISTS `admin_payment_methods`;
DROP TABLE IF EXISTS `admin_merchants`;
DROP TABLE IF EXISTS `admin_channels`;
DROP TABLE IF EXISTS `admin_risk_rule_audit_log`;
DROP TABLE IF EXISTS `admin_risk_hit_record`;
DROP TABLE IF EXISTS `admin_risk_rule_merchant_scope`;
DROP TABLE IF EXISTS `risk_rules`;
DROP TABLE IF EXISTS `admin_system_configs`;
DROP TABLE IF EXISTS `admin_audit_logs`;
DROP TABLE IF EXISTS `admin_users`;

CREATE TABLE `admin_users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `password` VARCHAR(256) NOT NULL COMMENT '密码(BCrypt)',
  `role` VARCHAR(32) DEFAULT NULL COMMENT 'SUPER_ADMIN/ADMIN/FINANCE/RISK',
  `nickname` VARCHAR(64) DEFAULT NULL,
  `status` VARCHAR(16) DEFAULT NULL COMMENT 'ACTIVE/DISABLED',
  `data_merchant_ids` VARCHAR(512) DEFAULT NULL COMMENT '数据权限商户号，逗号分隔',
  `ui_theme` VARCHAR(16) NOT NULL DEFAULT 'mint' COMMENT '主题：mint/ocean/violet/dark',
  `ui_table_density` VARCHAR(16) NOT NULL DEFAULT 'standard' COMMENT '表格密度：standard/compact',
  `ui_sidebar_collapsed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '侧栏是否折叠',
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员账号';

CREATE TABLE `admin_audit_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) DEFAULT NULL,
  `operator_type` VARCHAR(32) DEFAULT NULL COMMENT 'SYSTEM_ADMIN/MERCHANT_ADMIN/SYSTEM_TASK',
  `merchant_id` VARCHAR(64) DEFAULT NULL COMMENT '操作涉及商户号',
  `action` VARCHAR(32) DEFAULT NULL,
  `resource_path` VARCHAR(512) DEFAULT NULL,
  `resource_type` VARCHAR(64) DEFAULT NULL COMMENT '资源类型',
  `resource_id` VARCHAR(128) DEFAULT NULL COMMENT '资源标识',
  `detail` VARCHAR(1024) DEFAULT NULL,
  `result` VARCHAR(32) DEFAULT NULL COMMENT 'SUCCESS/FAILED/DENIED',
  `deny_reason` VARCHAR(256) DEFAULT NULL COMMENT '拒绝原因',
  `client_ip` VARCHAR(64) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_created` (`created_at`),
  KEY `idx_audit_username` (`username`),
  KEY `idx_audit_merchant_created` (`merchant_id`, `created_at`),
  KEY `idx_audit_resource` (`resource_type`, `resource_id`),
  KEY `idx_audit_result_created` (`result`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理端操作审计';

CREATE TABLE `admin_channels` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `channel_code` VARCHAR(64) NOT NULL,
  `channel_name` VARCHAR(128) NOT NULL,
  `channel_type` VARCHAR(32) DEFAULT NULL,
  `api_url` VARCHAR(256) DEFAULT NULL,
  `api_key` VARCHAR(256) DEFAULT NULL,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `priority` INT DEFAULT 0,
  `icon` VARCHAR(128) DEFAULT NULL COMMENT '图标',
  `description` VARCHAR(512) DEFAULT NULL,
  `fee_rate` DECIMAL(6,4) DEFAULT NULL COMMENT '渠道默认手续费率',
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_channel_code` (`channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付渠道';

CREATE TABLE `admin_merchants` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `merchant_id` VARCHAR(64) NOT NULL,
  `merchant_name` VARCHAR(128) NOT NULL,
  `merchant_key` VARCHAR(256) DEFAULT NULL,
  `callback_url` VARCHAR(256) DEFAULT NULL,
  `notify_url` VARCHAR(256) DEFAULT NULL,
  `commission_rate` DECIMAL(6,4) DEFAULT NULL,
  `rate_calc_mode` VARCHAR(16) DEFAULT 'flat' COMMENT 'flat/segmented',
  `merchant_group` VARCHAR(64) DEFAULT NULL COMMENT '费率组',
  `status` VARCHAR(16) DEFAULT NULL,
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户';

CREATE TABLE `admin_payment_methods` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `method_code` VARCHAR(64) NOT NULL,
  `method_name` VARCHAR(128) NOT NULL,
  `channel_id` BIGINT DEFAULT NULL,
  `app_id` VARCHAR(128) DEFAULT NULL,
  `app_secret` VARCHAR(256) DEFAULT NULL,
  `mch_id` VARCHAR(64) DEFAULT NULL,
  `mch_key` VARCHAR(256) DEFAULT NULL,
  `cert_path` VARCHAR(256) DEFAULT NULL,
  `cert_password` VARCHAR(128) DEFAULT NULL,
  `config_json` TEXT DEFAULT NULL,
  `enabled` TINYINT(1) DEFAULT 1,
  `priority` INT DEFAULT 0,
  `description` VARCHAR(512) DEFAULT NULL,
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_method_code` (`method_code`),
  KEY `idx_channel_id` (`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付方式';

CREATE TABLE `admin_merchant_payment_methods` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `merchant_id` VARCHAR(64) NOT NULL COMMENT '商户号',
  `payment_method_id` BIGINT DEFAULT NULL,
  `enabled` TINYINT(1) DEFAULT 1,
  `priority` INT DEFAULT 0,
  `custom_config_json` TEXT DEFAULT NULL,
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_merchant_id` (`merchant_id`),
  KEY `idx_payment_method_id` (`payment_method_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户支付方式绑定';

CREATE TABLE `admin_payment_accounts` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `channel_id` BIGINT DEFAULT NULL,
  `account_code` VARCHAR(64) NOT NULL,
  `account_name` VARCHAR(128) NOT NULL,
  `app_id` VARCHAR(128) DEFAULT NULL,
  `app_secret` VARCHAR(256) DEFAULT NULL,
  `mch_id` VARCHAR(64) DEFAULT NULL,
  `mch_key` VARCHAR(256) DEFAULT NULL,
  `cert_path` VARCHAR(256) DEFAULT NULL,
  `cert_password` VARCHAR(128) DEFAULT NULL,
  `config_json` TEXT DEFAULT NULL,
  `enabled` TINYINT(1) DEFAULT 1,
  `priority` INT DEFAULT 0,
  `description` VARCHAR(512) DEFAULT NULL,
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_code` (`account_code`),
  KEY `idx_account_channel_id` (`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付账号';

CREATE TABLE `admin_merchant_payment_routes` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `merchant_id` VARCHAR(64) NOT NULL,
  `payment_method_id` BIGINT NOT NULL,
  `payment_account_id` BIGINT NOT NULL,
  `enabled` TINYINT(1) DEFAULT 1,
  `priority` INT DEFAULT 0,
  `client_scopes` VARCHAR(64) NOT NULL DEFAULT 'PC,H5,APP',
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_method_account` (`merchant_id`, `payment_method_id`, `payment_account_id`),
  KEY `idx_route_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户支付路由';

CREATE TABLE `admin_channel_routes` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `merchant_id` VARCHAR(64) NOT NULL,
  `channel_id` BIGINT NOT NULL,
  `payment_account_id` BIGINT NOT NULL,
  `enabled` TINYINT(1) DEFAULT 1,
  `priority` INT DEFAULT 0,
  `description` VARCHAR(512) DEFAULT NULL,
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_channel_account` (`merchant_id`, `channel_id`, `payment_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户渠道路由';

CREATE TABLE `admin_system_configs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `config_key` VARCHAR(128) NOT NULL,
  `config_value` TEXT NOT NULL,
  `value_type` VARCHAR(32) NOT NULL DEFAULT 'STRING',
  `category` VARCHAR(64) NOT NULL,
  `description` VARCHAR(512) DEFAULT '',
  `sort_order` INT DEFAULT 0,
  `status` TINYINT DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置';

CREATE TABLE `risk_rules` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rule_code` VARCHAR(64) NOT NULL,
  `rule_name` VARCHAR(128) NOT NULL,
  `rule_type` VARCHAR(32) NOT NULL,
  `risk_expr` VARCHAR(1024) DEFAULT NULL COMMENT 'QLExpress 表达式',
  `threshold` DECIMAL(18,2) DEFAULT NULL COMMENT '兼容旧字段，后续使用 threshold_fen',
  `threshold_fen` BIGINT DEFAULT NULL COMMENT '阈值，金额类单位为分',
  `unit` VARCHAR(32) DEFAULT NULL,
  `action` VARCHAR(32) DEFAULT NULL,
  `enabled` TINYINT(1) DEFAULT 1,
  `priority` INT NOT NULL DEFAULT 100 COMMENT '优先级，数值越小越先评估',
  `owner_type` VARCHAR(32) NOT NULL DEFAULT 'PLATFORM' COMMENT 'PLATFORM/MERCHANT',
  `owner_merchant_id` VARCHAR(64) DEFAULT NULL COMMENT '商户自建规则归属商户',
  `scope_type` VARCHAR(32) NOT NULL DEFAULT 'ALL_MERCHANTS' COMMENT 'ALL_MERCHANTS/SELECTED_MERCHANTS/OWNER_MERCHANT_ONLY',
  `description` VARCHAR(512) DEFAULT NULL,
  `created_by` VARCHAR(64) DEFAULT NULL,
  `updated_by` VARCHAR(64) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_code` (`rule_code`),
  KEY `idx_risk_rules_owner` (`owner_type`, `owner_merchant_id`),
  KEY `idx_risk_rules_scope` (`scope_type`, `enabled`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风控规则';

CREATE TABLE `admin_risk_rule_merchant_scope` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rule_id` BIGINT NOT NULL,
  `merchant_id` VARCHAR(64) NOT NULL,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_merchant` (`rule_id`, `merchant_id`),
  KEY `idx_scope_merchant` (`merchant_id`, `enabled`),
  KEY `idx_scope_rule` (`rule_id`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风控规则商户作用范围';

CREATE TABLE `admin_risk_hit_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `trace_id` VARCHAR(128) DEFAULT NULL,
  `merchant_id` VARCHAR(64) NOT NULL,
  `merchant_name` VARCHAR(128) DEFAULT NULL,
  `order_id` VARCHAR(64) DEFAULT NULL,
  `merchant_order_no` VARCHAR(128) DEFAULT NULL,
  `rule_id` BIGINT NOT NULL,
  `rule_code` VARCHAR(64) NOT NULL,
  `rule_name` VARCHAR(128) NOT NULL,
  `owner_type` VARCHAR(32) NOT NULL,
  `scope_type` VARCHAR(32) NOT NULL,
  `action` VARCHAR(32) NOT NULL,
  `decision` VARCHAR(32) NOT NULL,
  `hit_reason` VARCHAR(512) DEFAULT NULL,
  `request_summary` VARCHAR(1024) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_hit_merchant_created` (`merchant_id`, `created_at`),
  KEY `idx_hit_rule_created` (`rule_id`, `created_at`),
  KEY `idx_hit_decision_created` (`decision`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风控命中记录';

CREATE TABLE `admin_risk_rule_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rule_id` BIGINT NOT NULL,
  `operator_id` VARCHAR(64) DEFAULT NULL,
  `operator_name` VARCHAR(128) DEFAULT NULL,
  `operator_type` VARCHAR(32) NOT NULL,
  `merchant_id` VARCHAR(64) DEFAULT NULL,
  `operation_type` VARCHAR(32) NOT NULL,
  `before_summary` VARCHAR(1024) DEFAULT NULL,
  `after_summary` VARCHAR(1024) DEFAULT NULL,
  `client_ip` VARCHAR(64) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_rule_created` (`rule_id`, `created_at`),
  KEY `idx_audit_operator_created` (`operator_type`, `created_at`),
  KEY `idx_audit_merchant_created` (`merchant_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风控规则变更审计';

CREATE TABLE `sys_roles` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_code` VARCHAR(64) NOT NULL,
  `role_name` VARCHAR(128) NOT NULL,
  `description` VARCHAR(512) DEFAULT NULL,
  `status` VARCHAR(16) DEFAULT 'ACTIVE',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色';

CREATE TABLE `sys_menus` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `parent_id` BIGINT DEFAULT NULL,
  `menu_code` VARCHAR(64) NOT NULL,
  `menu_name` VARCHAR(128) NOT NULL,
  `menu_type` VARCHAR(16) NOT NULL,
  `path` VARCHAR(256) DEFAULT NULL,
  `icon` VARCHAR(64) DEFAULT NULL,
  `sort_order` INT DEFAULT 0,
  `visible` TINYINT(1) DEFAULT 1,
  `status` VARCHAR(16) DEFAULT 'ACTIVE',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_menu_code` (`menu_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单';

CREATE TABLE `sys_role_menus` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_id` BIGINT NOT NULL,
  `menu_id` BIGINT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色菜单';

CREATE TABLE `sys_users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password` VARCHAR(255) NOT NULL,
  `nickname` VARCHAR(128) DEFAULT NULL,
  `phone` VARCHAR(32) DEFAULT NULL,
  `email` VARCHAR(128) DEFAULT NULL,
  `status` VARCHAR(32) DEFAULT 'ACTIVE',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户';

CREATE TABLE `sys_user_roles` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色';

CREATE TABLE `recon_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `task_id` VARCHAR(64) NOT NULL,
  `channel` VARCHAR(32) NOT NULL,
  `account_code` VARCHAR(64) NOT NULL,
  `bill_date` DATE NOT NULL,
  `bill_type` VARCHAR(32) NOT NULL DEFAULT 'trade',
  `status` VARCHAR(32) NOT NULL,
  `file_object_key` VARCHAR(512) DEFAULT NULL,
  `file_size` BIGINT DEFAULT NULL,
  `bill_total_count` INT DEFAULT NULL,
  `bill_total_amount` BIGINT DEFAULT NULL,
  `local_total_count` INT DEFAULT NULL,
  `local_total_amount` BIGINT DEFAULT NULL,
  `diff_count` INT NOT NULL DEFAULT 0,
  `elapsed_ms` BIGINT DEFAULT NULL,
  `error_msg` VARCHAR(1024) DEFAULT NULL,
  `triggered_by` VARCHAR(32) DEFAULT NULL,
  `xxl_log_id` BIGINT DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recon_task_biz` (`channel`, `account_code`, `bill_date`, `bill_type`),
  UNIQUE KEY `uk_recon_task_id` (`task_id`),
  KEY `idx_recon_task_bill_date` (`bill_date`),
  KEY `idx_recon_task_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对账任务';

CREATE TABLE `recon_bill_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `task_id` VARCHAR(64) NOT NULL,
  `channel` VARCHAR(32) NOT NULL,
  `channel_trade_no` VARCHAR(128) DEFAULT NULL,
  `out_trade_no` VARCHAR(128) DEFAULT NULL,
  `amount_fen` BIGINT DEFAULT NULL,
  `refund_fen` BIGINT DEFAULT NULL,
  `channel_status` VARCHAR(64) DEFAULT NULL,
  `finish_time` DATETIME DEFAULT NULL,
  `raw_line` TEXT,
  `parse_error` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_recon_bill_task` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='三方账单明细';

CREATE TABLE `recon_diff` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `task_id` VARCHAR(64) NOT NULL,
  `diff_type` VARCHAR(32) NOT NULL,
  `channel_trade_no` VARCHAR(128) DEFAULT NULL,
  `local_order_id` VARCHAR(64) DEFAULT NULL,
  `channel_amount` BIGINT DEFAULT NULL,
  `local_amount` BIGINT DEFAULT NULL,
  `channel_status` VARCHAR(64) DEFAULT NULL,
  `local_status` VARCHAR(64) DEFAULT NULL,
  `handle_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  `handle_remark` VARCHAR(512) DEFAULT NULL,
  `suggested_action` VARCHAR(128) DEFAULT NULL,
  `handled_by` VARCHAR(64) DEFAULT NULL,
  `handled_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_recon_diff_task` (`task_id`),
  KEY `idx_recon_diff_handle` (`handle_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对账差异';

CREATE TABLE `recon_handler_audit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `diff_id` BIGINT NOT NULL,
  `action` VARCHAR(32) NOT NULL,
  `operator` VARCHAR(64) DEFAULT NULL,
  `detail` VARCHAR(1024) DEFAULT NULL,
  `client_ip` VARCHAR(64) DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_recon_audit_diff` (`diff_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='差异处理审计';

CREATE TABLE `recon_merchant_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `merchant_task_id` VARCHAR(64) NOT NULL,
  `merchant_id` VARCHAR(64) NOT NULL,
  `bill_date` DATE NOT NULL,
  `status` VARCHAR(32) NOT NULL,
  `payment_count` INT DEFAULT NULL,
  `payment_amount_fen` BIGINT DEFAULT NULL,
  `statement_object_key` VARCHAR(512) DEFAULT NULL,
  `statement_size` BIGINT DEFAULT NULL,
  `elapsed_ms` BIGINT DEFAULT NULL,
  `error_msg` VARCHAR(1024) DEFAULT NULL,
  `triggered_by` VARCHAR(32) DEFAULT NULL,
  `xxl_log_id` BIGINT DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_recon_merchant_biz` (`merchant_id`, `bill_date`),
  UNIQUE KEY `uk_recon_merchant_task_id` (`merchant_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户对账子任务';

CREATE TABLE `admin_dashboard_metrics` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `metric_time` DATETIME NOT NULL,
  `granularity` VARCHAR(10) NOT NULL COMMENT '5min/hour/day',
  `channel_code` VARCHAR(32) DEFAULT 'ALL',
  `total_amount` BIGINT NOT NULL DEFAULT 0,
  `total_count` INT NOT NULL DEFAULT 0,
  `active_merchants` INT NOT NULL DEFAULT 0,
  `fee_income` BIGINT NOT NULL DEFAULT 0,
  `refund_amount` BIGINT NOT NULL DEFAULT 0,
  `refund_count` INT NOT NULL DEFAULT 0,
  `version` INT DEFAULT 0,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_metric_time` (`metric_time`),
  KEY `idx_granularity_time` (`granularity`, `metric_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='BI仪表盘指标';

CREATE TABLE `admin_churn_alert` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `merchant_id` BIGINT NOT NULL,
  `merchant_name` VARCHAR(128) DEFAULT NULL,
  `alert_level` VARCHAR(16) NOT NULL,
  `current_avg_count` DECIMAL(10,2) DEFAULT NULL,
  `baseline_avg_count` DECIMAL(10,2) DEFAULT NULL,
  `decline_pct` DECIMAL(5,2) DEFAULT NULL,
  `consecutive_days` INT DEFAULT 0,
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending',
  `assignee` VARCHAR(64) DEFAULT NULL,
  `note` TEXT,
  `resolved_time` DATETIME DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_merchant_id` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户流失预警';

CREATE TABLE `admin_fee_rate_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `scope_type` VARCHAR(16) NOT NULL DEFAULT 'global',
  `scope_value` VARCHAR(64) DEFAULT NULL,
  `channel_code` VARCHAR(32) DEFAULT 'ALL',
  `tier_min` BIGINT NOT NULL,
  `tier_max` BIGINT DEFAULT NULL,
  `fee_rate` DECIMAL(6,4) NOT NULL,
  `calc_mode` VARCHAR(16) NOT NULL DEFAULT 'flat',
  `priority` INT DEFAULT 0,
  `status` VARCHAR(10) DEFAULT 'enabled',
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='阶梯费率配置';

CREATE TABLE `admin_merchant_fee_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `merchant_id` BIGINT NOT NULL,
  `snapshot_month` VARCHAR(7) NOT NULL,
  `applicable_rate` DECIMAL(6,4) NOT NULL,
  `monthly_amount` BIGINT NOT NULL DEFAULT 0,
  `current_tier` INT DEFAULT 0,
  `next_tier_amount` BIGINT DEFAULT NULL,
  `next_tier_rate` DECIMAL(6,4) DEFAULT NULL,
  `calc_mode` VARCHAR(16) NOT NULL,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_month` (`merchant_id`, `snapshot_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户月费率快照';

CREATE TABLE `admin_fee_rate_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `merchant_id` BIGINT NOT NULL,
  `change_time` DATETIME NOT NULL,
  `old_rate` DECIMAL(6,4) DEFAULT NULL,
  `new_rate` DECIMAL(6,4) NOT NULL,
  `trigger_reason` VARCHAR(64) NOT NULL,
  `operator` VARCHAR(64) DEFAULT NULL,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_merchant_time` (`merchant_id`, `change_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='费率变更审计';

CREATE TABLE `recon_routing_decision_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `trade_no` VARCHAR(64) NOT NULL,
  `merchant_id` BIGINT NOT NULL,
  `available_channels` JSON DEFAULT NULL,
  `selected_channel` VARCHAR(32) NOT NULL,
  `selection_reason` VARCHAR(32) NOT NULL,
  `decision_cost_ms` INT DEFAULT NULL,
  `fallback_count` INT DEFAULT 0,
  `deleted` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_trade_no` (`trade_no`),
  KEY `idx_merchant_time` (`merchant_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='路由决策日志';

CREATE TABLE `cashier_risk_blacklist` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `entry_type` VARCHAR(32) NOT NULL,
  `entry_value` VARCHAR(256) NOT NULL,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `remark` VARCHAR(512) DEFAULT NULL,
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_value` (`entry_type`, `entry_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='风控黑名单';

CREATE TABLE `merchant_application` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `application_no` VARCHAR(64) NOT NULL,
  `merchant_name` VARCHAR(128) NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
  `application_source` VARCHAR(32) NOT NULL DEFAULT 'CASHIER_PUBLIC',
  `biz_license_no` VARCHAR(64) DEFAULT NULL,
  `contact_name` VARCHAR(64) DEFAULT NULL,
  `contact_phone` VARCHAR(32) DEFAULT NULL,
  `contact_email` VARCHAR(128) DEFAULT NULL,
  `allocated_merchant_id` VARCHAR(64) DEFAULT NULL,
  `secret_cipher` VARCHAR(512) DEFAULT NULL,
  `secret_viewed_at` DATETIME DEFAULT NULL,
  `result_query_count` INT NOT NULL DEFAULT 0,
  `approver_id` BIGINT DEFAULT NULL,
  `approved_at` DATETIME DEFAULT NULL,
  `rejected_at` DATETIME DEFAULT NULL,
  `payload_json` TEXT,
  `reject_reason` VARCHAR(512) DEFAULT NULL,
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_no` (`application_no`),
  KEY `idx_status` (`status`),
  KEY `idx_contact_phone` (`contact_phone`),
  KEY `idx_contact_email` (`contact_email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户进件';

CREATE TABLE `merchant_contract` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `merchant_id` VARCHAR(64) NOT NULL,
  `version` INT NOT NULL DEFAULT 1,
  `fee_rate_json` VARCHAR(1024) DEFAULT NULL,
  `effective_at` DATETIME DEFAULT NULL,
  `expire_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户合同';

CREATE TABLE `merchant_open_app` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `merchant_id` VARCHAR(64) NOT NULL,
  `app_id` VARCHAR(64) NOT NULL,
  `app_name` VARCHAR(128) DEFAULT NULL,
  `secret_current` VARCHAR(512) NOT NULL,
  `secret_previous` VARCHAR(512) DEFAULT NULL,
  `secret_previous_expire_at` DATETIME DEFAULT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  `created_at` DATETIME DEFAULT NULL,
  `updated_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='开放平台应用';

CREATE TABLE `merchant_webhook_endpoint` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `merchant_id` VARCHAR(64) NOT NULL,
  `url` VARCHAR(512) NOT NULL,
  `secret` VARCHAR(256) NOT NULL,
  `event_codes` VARCHAR(512) NOT NULL,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_merchant` (`merchant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Webhook 端点';

CREATE TABLE `webhook_delivery_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `merchant_id` VARCHAR(64) NOT NULL,
  `endpoint_id` BIGINT NOT NULL,
  `event_code` VARCHAR(64) NOT NULL,
  `payload_json` MEDIUMTEXT,
  `http_status` INT DEFAULT NULL,
  `response_body` VARCHAR(2048) DEFAULT NULL,
  `attempt` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  `created_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_merchant_event` (`merchant_id`, `event_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Webhook 投递日志';

CREATE TABLE `payment_link` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `link_id` VARCHAR(32) NOT NULL,
  `merchant_id` VARCHAR(64) NOT NULL,
  `title` VARCHAR(256) NOT NULL,
  `amount` BIGINT DEFAULT NULL,
  `currency` VARCHAR(8) NOT NULL DEFAULT 'CNY',
  `max_use` INT DEFAULT NULL,
  `used_count` INT NOT NULL DEFAULT 0,
  `expire_at` DATETIME DEFAULT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  `created_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_link` (`link_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收款链接';

CREATE OR REPLACE VIEW `channels` AS SELECT * FROM `admin_channels`;
CREATE OR REPLACE VIEW `merchants` AS SELECT * FROM `admin_merchants`;
CREATE OR REPLACE VIEW `payment_methods` AS SELECT * FROM `admin_payment_methods`;
CREATE OR REPLACE VIEW `merchant_payment_methods` AS SELECT * FROM `admin_merchant_payment_methods`;
CREATE OR REPLACE VIEW `payment_accounts` AS SELECT * FROM `admin_payment_accounts`;
CREATE OR REPLACE VIEW `merchant_payment_routes` AS SELECT * FROM `admin_merchant_payment_routes`;

SET FOREIGN_KEY_CHECKS = 1;
