-- noinspection SqlNoDataSourceInspectionForFile

-- ============================================================
-- 生产环境加固 — 数据库迁移
-- Date: 2026-05-15
-- Feature: 005-production-hardening
-- ============================================================

-- ==================== 1. cashier_refunds 乐观锁 + 索引 ====================

-- 1.1 添加乐观锁版本号字段
ALTER TABLE cashier_refunds ADD COLUMN IF NOT EXISTS version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';

-- 1.2 添加复合索引优化 sumRefundedAmount 查询（WHERE payment_id = ? AND status = ?）
CREATE INDEX IF NOT EXISTS idx_payment_status ON cashier_refunds (payment_id, status);

-- ==================== 2. admin_merchant_payment_methods 唯一约束 ====================

-- 防止同一商户重复绑定同一支付方式
CREATE UNIQUE INDEX IF NOT EXISTS uk_merchant_method ON admin_merchant_payment_methods (merchant_id, payment_method_id);

-- ==================== 3. 修复缺失的 NOT NULL 约束 ====================

ALTER TABLE cashier_orders MODIFY COLUMN merchant_id VARCHAR(64) NOT NULL COMMENT '商户ID';
ALTER TABLE cashier_orders MODIFY COLUMN status VARCHAR(16) NOT NULL COMMENT '订单状态';
ALTER TABLE cashier_payments MODIFY COLUMN pay_method VARCHAR(32) NOT NULL COMMENT '支付方式';
ALTER TABLE cashier_payments MODIFY COLUMN status VARCHAR(16) NOT NULL COMMENT '支付状态';
ALTER TABLE admin_users MODIFY COLUMN status VARCHAR(16) NOT NULL COMMENT '状态';
ALTER TABLE admin_merchants MODIFY COLUMN status VARCHAR(16) NOT NULL COMMENT '状态';
ALTER TABLE admin_channels MODIFY COLUMN enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用';
