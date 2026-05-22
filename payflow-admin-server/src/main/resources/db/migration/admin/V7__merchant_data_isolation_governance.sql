-- 商户数据隔离治理（Feature 008）

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

ALTER TABLE recon_task
  ADD COLUMN merchant_id VARCHAR(64) NULL COMMENT '对账任务涉及商户号' AFTER account_code,
  ADD KEY idx_recon_task_merchant_date (merchant_id, bill_date);

ALTER TABLE recon_diff
  ADD COLUMN merchant_id VARCHAR(64) NULL COMMENT '差异归属商户号' AFTER task_id,
  ADD KEY idx_recon_diff_merchant (merchant_id, handle_status);
