-- =============================================================================
-- PonyFlux Pay — 商户数据隔离治理增量迁移
-- Feature: 008-merchant-data-isolation
-- Date: 2026-05-21
-- =============================================================================

USE payflow_admin;
SET NAMES utf8mb4;

-- 后台操作日志补充结构化商户归属、资源和拒绝结果，支持跨商户拒绝审计闭环。
ALTER TABLE admin_audit_logs
  ADD COLUMN merchant_id VARCHAR(64) NULL COMMENT '操作涉及商户号；全局操作为空' AFTER username,
  ADD COLUMN operator_type VARCHAR(32) NULL COMMENT 'SYSTEM_ADMIN/MERCHANT_ADMIN/SYSTEM_TASK' AFTER username,
  ADD COLUMN resource_type VARCHAR(64) NULL COMMENT '资源类型' AFTER resource_path,
  ADD COLUMN resource_id VARCHAR(128) NULL COMMENT '资源标识' AFTER resource_type,
  ADD COLUMN result VARCHAR(32) NULL COMMENT 'SUCCESS/FAILED/DENIED' AFTER detail,
  ADD COLUMN deny_reason VARCHAR(256) NULL COMMENT '拒绝原因或脱敏说明' AFTER result,
  ADD KEY idx_audit_merchant_created (merchant_id, created_at),
  ADD KEY idx_audit_resource (resource_type, resource_id),
  ADD KEY idx_audit_result_created (result, created_at);

-- 数据隔离治理检查项，记录数据表、页面、接口、异步任务和导出任务的隔离状态。
CREATE TABLE IF NOT EXISTS admin_data_isolation_checks (
  id BIGINT NOT NULL AUTO_INCREMENT,
  check_id VARCHAR(64) NOT NULL COMMENT '检查项标识',
  target_type VARCHAR(32) NOT NULL COMMENT 'DATA_TABLE/PAGE/API/ASYNC_TASK/EXPORT_TASK',
  target_name VARCHAR(256) NOT NULL COMMENT '检查目标名称',
  classification VARCHAR(32) NOT NULL COMMENT 'MERCHANT/GLOBAL/SYSTEM_AUDIT/MANUAL_REVIEW',
  merchant_field_status VARCHAR(32) NOT NULL COMMENT 'PRESENT/MISSING/NOT_APPLICABLE/PENDING_CONFIRM',
  risk_level VARCHAR(16) NOT NULL COMMENT 'HIGH/MEDIUM/LOW',
  affected_entries VARCHAR(1024) DEFAULT NULL COMMENT '影响入口摘要',
  remediation_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/IN_PROGRESS/DONE/EXEMPTED/NEEDS_MANUAL_REVIEW',
  decision_reason VARCHAR(1024) DEFAULT NULL COMMENT '分类或豁免理由',
  merchant_id VARCHAR(64) DEFAULT NULL COMMENT '检查项关联商户；全局或待确认可为空',
  last_scanned_at DATETIME DEFAULT NULL COMMENT '最近扫描时间',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_check_id (check_id),
  KEY idx_isolation_classification (classification, remediation_status),
  KEY idx_isolation_risk (risk_level, remediation_status),
  KEY idx_isolation_target (target_type, target_name),
  KEY idx_isolation_merchant (merchant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商户数据隔离检查项';

-- 对账核心任务补充可选商户归属。渠道级任务仍可为空，商户级任务或可推断任务应写入 merchant_id。
ALTER TABLE recon_task
  ADD COLUMN merchant_id VARCHAR(64) NULL COMMENT '对账任务涉及商户号；渠道级聚合任务为空或待确认' AFTER account_code,
  ADD KEY idx_recon_task_merchant_date (merchant_id, bill_date);

ALTER TABLE recon_bill_record
  ADD COLUMN merchant_id VARCHAR(64) NULL COMMENT '账单明细推断商户号；无法推断时为空' AFTER task_id,
  ADD KEY idx_recon_bill_merchant (merchant_id, created_at);

ALTER TABLE recon_diff
  ADD COLUMN merchant_id VARCHAR(64) NULL COMMENT '差异归属商户号；无法推断时为空' AFTER task_id,
  ADD KEY idx_recon_diff_merchant (merchant_id, handle_status);

ALTER TABLE recon_handler_audit
  ADD COLUMN merchant_id VARCHAR(64) NULL COMMENT '差异处理涉及商户号' AFTER diff_id,
  ADD KEY idx_recon_audit_merchant (merchant_id, created_at);

-- 初始化治理检查基线。后续扫描服务可刷新 last_scanned_at、风险等级和整改状态。
INSERT INTO admin_data_isolation_checks (
  check_id, target_type, target_name, classification, merchant_field_status,
  risk_level, affected_entries, remediation_status, decision_reason, merchant_id, last_scanned_at
) VALUES
('CHK-ADMIN-ORDERS', 'API', '/admin/orders', 'MERCHANT', 'PRESENT', 'HIGH', '订单列表/详情/关闭/导出', 'PENDING', 'cashier_orders.merchant_id 可直接限定授权范围', NULL, NOW()),
('CHK-ADMIN-PAYMENTS', 'DATA_TABLE', 'cashier_payments', 'MERCHANT', 'PENDING_CONFIRM', 'HIGH', '后台支付查询', 'NEEDS_MANUAL_REVIEW', '需通过 order_id 关联 cashier_orders 推断商户', NULL, NOW()),
('CHK-ADMIN-REFUNDS', 'API', '/admin/refunds', 'MERCHANT', 'PENDING_CONFIRM', 'HIGH', '退款列表/审核/拒绝', 'NEEDS_MANUAL_REVIEW', '需通过 order_id 或 payment_id 推断商户', NULL, NOW()),
('CHK-ADMIN-CHANNEL-ROUTES', 'DATA_TABLE', 'admin_channel_routes', 'MERCHANT', 'PRESENT', 'HIGH', '渠道路由维护', 'PENDING', 'admin_channel_routes.merchant_id 已具备直接归属', NULL, NOW()),
('CHK-ADMIN-MERCHANT-METHODS', 'DATA_TABLE', 'admin_merchant_payment_methods', 'MERCHANT', 'PRESENT', 'HIGH', '商户支付方式配置', 'PENDING', 'admin_merchant_payment_methods.merchant_id 已具备直接归属', NULL, NOW()),
('CHK-RECON-TASK', 'DATA_TABLE', 'recon_task', 'MANUAL_REVIEW', 'PENDING_CONFIRM', 'HIGH', '对账任务和差异查询', 'NEEDS_MANUAL_REVIEW', '历史任务缺少直接 merchant_id，需要按账号或订单推断', NULL, NOW()),
('CHK-RECON-DIFF', 'DATA_TABLE', 'recon_diff', 'MANUAL_REVIEW', 'PENDING_CONFIRM', 'HIGH', '对账差异处理', 'NEEDS_MANUAL_REVIEW', '历史差异缺少直接 merchant_id，处理前必须校验归属', NULL, NOW()),
('CHK-RISK-RULES', 'API', '/admin/risk/rules', 'MERCHANT', 'PRESENT', 'MEDIUM', '风控规则和命中记录', 'PENDING', 'owner_merchant_id 与作用商户范围可用于隔离', NULL, NOW()),
('CHK-FEE-RATE', 'DATA_TABLE', 'admin_fee_rate_config', 'GLOBAL', 'NOT_APPLICABLE', 'MEDIUM', '费率配置', 'PENDING', 'global/merchant_group 为共享配置，merchant 范围需按授权限制', NULL, NOW()),
('CHK-SYSTEM-CONFIG', 'API', '/admin/system-configs', 'GLOBAL', 'NOT_APPLICABLE', 'LOW', '系统配置', 'PENDING', '平台公共配置共享，敏感项输出脱敏或迁移为商户级配置', NULL, NOW()),
('CHK-SYS-MENUS', 'DATA_TABLE', 'sys_menus', 'GLOBAL', 'NOT_APPLICABLE', 'LOW', '菜单模板', 'EXEMPTED', '菜单是平台模板，菜单授权不能替代数据授权', NULL, NOW()),
('CHK-CASHIER-ORDERS', 'DATA_TABLE', 'cashier_orders', 'MERCHANT', 'PRESENT', 'HIGH', '收银端订单创建/查询', 'PENDING', 'cashier_orders.merchant_id 已具备直接归属', NULL, NOW()),
('CHK-CASHIER-SECURITY-AUDIT', 'DATA_TABLE', 'cashier_security_audit', 'SYSTEM_AUDIT', 'PRESENT', 'MEDIUM', '跨商户拒绝安全审计', 'PENDING', 'merchant_id/target_merchant_id 支持拒绝闭环', NULL, NOW())
ON DUPLICATE KEY UPDATE
  target_type = VALUES(target_type),
  target_name = VALUES(target_name),
  classification = VALUES(classification),
  merchant_field_status = VALUES(merchant_field_status),
  risk_level = VALUES(risk_level),
  affected_entries = VALUES(affected_entries),
  remediation_status = VALUES(remediation_status),
  decision_reason = VALUES(decision_reason),
  last_scanned_at = VALUES(last_scanned_at),
  updated_at = NOW();
