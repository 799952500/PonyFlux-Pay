-- 对账差异处置工作流升级（工单 + SLA + 报告订阅 + 预聚合兜底）
-- 说明：对账表位于 payflow_admin 库，前缀 recon_。

CREATE TABLE IF NOT EXISTS recon_diff_assignment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  diff_id BIGINT NOT NULL,
  merchant_id VARCHAR(64) NOT NULL,
  assignee_id VARCHAR(64) NULL,
  workflow_status VARCHAR(32) NOT NULL,
  assigned_at DATETIME NULL,
  due_at DATETIME NULL,
  escalated_at DATETIME NULL,
  escalated_to_role VARCHAR(64) NULL,
  last_reminded_at DATETIME NULL,
  processed_at DATETIME NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_recon_diff_assignment_diff (diff_id),
  KEY idx_recon_diff_assignment_assignee (assignee_id),
  KEY idx_recon_diff_assignment_status (workflow_status),
  KEY idx_recon_diff_assignment_due (due_at),
  KEY idx_recon_diff_assignment_merchant (merchant_id)
);

CREATE TABLE IF NOT EXISTS recon_diff_sla_rule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  diff_type VARCHAR(32) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  sla_hours INT NOT NULL,
  due_soon_ratio DECIMAL(6,4) NOT NULL DEFAULT 0.2000,
  escalate_to_role VARCHAR(64) NOT NULL DEFAULT 'recon:manage',
  updated_by VARCHAR(64) NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_recon_diff_sla_rule_type (diff_type)
);

CREATE TABLE IF NOT EXISTS recon_report_subscription (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  subscriber_id VARCHAR(64) NOT NULL,
  report_type VARCHAR(16) NOT NULL,
  scope VARCHAR(32) NOT NULL,
  enabled TINYINT NOT NULL DEFAULT 1,
  last_sent_at DATETIME NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_recon_report_subscription (subscriber_id, report_type)
);

CREATE TABLE IF NOT EXISTS recon_report_snapshot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  snapshot_id VARCHAR(64) NOT NULL,
  subscriber_id VARCHAR(64) NOT NULL,
  report_type VARCHAR(16) NOT NULL,
  period_start DATETIME NOT NULL,
  period_end DATETIME NOT NULL,
  payload_json TEXT NOT NULL,
  generated_at DATETIME NOT NULL,
  UNIQUE KEY uk_recon_report_snapshot_snapshot (snapshot_id),
  KEY idx_recon_report_snapshot_subscriber (subscriber_id, report_type, generated_at)
);

-- 可选兜底：按日预聚合快照（是否启用由服务层根据数据量/性能决策）
CREATE TABLE IF NOT EXISTS recon_aggregation_snapshot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  stat_date DATE NOT NULL,
  merchant_id VARCHAR(64) NOT NULL,
  channel VARCHAR(32) NOT NULL,
  diff_type VARCHAR(32) NOT NULL,
  diff_count BIGINT NOT NULL,
  diff_amount BIGINT NOT NULL,
  processed_count BIGINT NOT NULL DEFAULT 0,
  ignored_count BIGINT NOT NULL DEFAULT 0,
  accepted_loss_count BIGINT NOT NULL DEFAULT 0,
  sla_met_count BIGINT NOT NULL DEFAULT 0,
  sla_total_count BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  UNIQUE KEY uk_recon_aggregation_snapshot (stat_date, merchant_id, channel, diff_type),
  KEY idx_recon_aggregation_snapshot_date (stat_date),
  KEY idx_recon_aggregation_snapshot_merchant (merchant_id)
);

-- 按钮级权限：对账差异工作流（依赖 V8__button_permissions.sql 已为 admin_sys_menus 增加 perm_code/api_pattern）
-- parent_id 约定：沿用 V8 中的对账 parent（60-63），并将新按钮挂到“对账任务/差异”节点下（62）。
INSERT INTO admin_sys_menus (id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, perm_code, api_pattern, created_at, updated_at) VALUES
(301, 62, 'btn_recon_work_item_assign', '工单指派/改派', 'BUTTON', NULL, NULL, 2, 1, 'ACTIVE', 'recon:diff:assign', 'POST:/api/v1/admin/reconcile/diffs/*/assign', NOW(), NOW()),
(302, 62, 'btn_recon_work_item_claim', '工单认领', 'BUTTON', NULL, NULL, 3, 1, 'ACTIVE', 'recon:diff:assign', 'POST:/api/v1/admin/reconcile/diffs/*/claim', NOW(), NOW()),
(303, 62, 'btn_recon_work_item_start', '开始处理', 'BUTTON', NULL, NULL, 4, 1, 'ACTIVE', 'recon:diff:handle', 'POST:/api/v1/admin/reconcile/diffs/*/start', NOW(), NOW()),
(304, 62, 'btn_recon_work_item_complete', '工单处置完成', 'BUTTON', NULL, NULL, 5, 1, 'ACTIVE', 'recon:diff:handle', 'POST:/api/v1/admin/reconcile/diffs/*/complete', NOW(), NOW()),
(305, 62, 'btn_recon_work_item_comment', '工单留言', 'BUTTON', NULL, NULL, 6, 1, 'ACTIVE', 'recon:diff:handle', 'POST:/api/v1/admin/reconcile/diffs/*/comment', NOW(), NOW()),
(306, 62, 'btn_recon_sla_rule_write', 'SLA 规则维护', 'BUTTON', NULL, NULL, 7, 1, 'ACTIVE', 'recon:manage', 'PUT:/api/v1/admin/reconcile/sla-rules/*', NOW(), NOW()),
(307, 62, 'btn_recon_report_subscribe', '报告订阅', 'BUTTON', NULL, NULL, 8, 1, 'ACTIVE', 'recon:report:subscribe', 'POST:/api/v1/admin/reconcile/subscriptions', NOW(), NOW());

-- SUPER_ADMIN 全量授权新增按钮
INSERT INTO admin_sys_role_menus (role_id, menu_id, created_at)
SELECT 1, id, NOW() FROM admin_sys_menus WHERE id IN (301,302,303,304,305,306,307);

-- ADMIN 角色默认授权（不含 system_config:write 等无关项，这里直接授权新增按钮）
INSERT INTO admin_sys_role_menus (role_id, menu_id, created_at)
SELECT 2, id, NOW() FROM admin_sys_menus WHERE id IN (301,302,303,304,305,306,307);

-- FINANCE 默认授权：差异处理 + 工单基础动作 + 报告订阅
INSERT INTO admin_sys_role_menus (role_id, menu_id, created_at)
SELECT 3, id, NOW() FROM admin_sys_menus WHERE id IN (232,301,302,303,304,305,307);

-- RISK 默认授权：差异处理 + 工单基础动作（不授权订阅）
INSERT INTO admin_sys_role_menus (role_id, menu_id, created_at)
SELECT 4, id, NOW() FROM admin_sys_menus WHERE id IN (232,301,302,303,304,305);

