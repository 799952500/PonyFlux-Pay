-- ============================================================
-- PonyFlux-Pay 数据库优化脚本
-- 日期：2026-05-05
-- 说明：添加缺失索引、修复字段类型、加强 NOT NULL 约束
-- 注意：幂等设计，可重复执行
-- ============================================================

-- ============= payflow_cashier =============

USE payflow_cashier;

-- 订单表：商户+商户订单号联合索引（防重+查询）
SET @exist := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'payflow_cashier' AND TABLE_NAME = 'cashier_orders' AND INDEX_NAME = 'idx_merchant_merchant_order');
SET @sql := IF(@exist = 0,
    'ALTER TABLE cashier_orders ADD INDEX idx_merchant_merchant_order (merchant_id, merchant_order_no)',
    'SELECT ''索引已存在: idx_merchant_merchant_order''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 支付记录表：渠道+状态索引
SET @exist := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'payflow_cashier' AND TABLE_NAME = 'cashier_payments' AND INDEX_NAME = 'idx_pay_channel_status');
SET @sql := IF(@exist = 0,
    'ALTER TABLE cashier_payments ADD INDEX idx_pay_channel_status (pay_channel, status)',
    'SELECT ''索引已存在: idx_pay_channel_status''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 退款表：订单+状态索引
SET @exist := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'payflow_cashier' AND TABLE_NAME = 'cashier_refunds' AND INDEX_NAME = 'idx_order_status');
SET @sql := IF(@exist = 0,
    'ALTER TABLE cashier_refunds ADD INDEX idx_order_status (order_id, status)',
    'SELECT ''索引已存在: idx_order_status''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- NOT NULL 约束
ALTER TABLE cashier_orders MODIFY COLUMN amount BIGINT NOT NULL COMMENT '订单金额(分)';
ALTER TABLE cashier_orders MODIFY COLUMN currency VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '币种';
ALTER TABLE cashier_payments MODIFY COLUMN order_id VARCHAR(64) NOT NULL COMMENT '关联订单号';
ALTER TABLE cashier_payments MODIFY COLUMN pay_channel VARCHAR(32) NOT NULL COMMENT '支付渠道';
ALTER TABLE cashier_payments MODIFY COLUMN amount BIGINT NOT NULL COMMENT '支付金额(分)';

-- ============= payflow_admin =============

USE payflow_admin;

-- 审计日志：用户+时间索引
SET @exist := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'payflow_admin' AND TABLE_NAME = 'admin_audit_logs' AND INDEX_NAME = 'idx_username_created');
SET @sql := IF(@exist = 0,
    'ALTER TABLE admin_audit_logs ADD INDEX idx_username_created (username, created_at)',
    'SELECT ''索引已存在: idx_username_created''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 路由表：商户+支付方式索引
SET @exist := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'payflow_admin' AND TABLE_NAME = 'merchant_payment_routes' AND INDEX_NAME = 'idx_route_merchant_method');
SET @sql := IF(@exist = 0,
    'ALTER TABLE merchant_payment_routes ADD INDEX idx_route_merchant_method (merchant_id, payment_method_id)',
    'SELECT ''索引已存在: idx_route_merchant_method''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 修复 merchant_payment_methods.merchant_id 类型：BIGINT → VARCHAR(64)
ALTER TABLE merchant_payment_methods MODIFY COLUMN merchant_id VARCHAR(64) NOT NULL COMMENT '商户号';
