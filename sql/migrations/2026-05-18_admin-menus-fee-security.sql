-- 修正安全审计层级，并补充阶梯费率 / 路由日志菜单
USE payflow_admin;
SET NAMES utf8mb4;

-- 安全审计：挂到「权限运维」分组（id=50），避免成为「退款管理」子项
INSERT INTO sys_menus (id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, created_at, updated_at)
VALUES (64, 50, 'security_audit', '安全审计', 'MENU', '/admin/security-audit', NULL, 5, 1, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_name = VALUES(menu_name),
  path = VALUES(path),
  sort_order = VALUES(sort_order),
  visible = VALUES(visible),
  status = VALUES(status),
  updated_at = NOW();

-- 计费与路由（渠道分组下）
INSERT INTO sys_menus (id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, created_at, updated_at)
VALUES
(65, 20, 'fee_rate_config', '阶梯费率配置', 'MENU', '/admin/fee-rate/config', NULL, 6, 1, 'ACTIVE', NOW(), NOW()),
(66, 20, 'fee_rate_audit_log', '费率变更审计', 'MENU', '/admin/fee-rate/audit-log', NULL, 7, 1, 'ACTIVE', NOW(), NOW()),
(67, 20, 'routing_logs', '路由决策日志', 'MENU', '/admin/routing/logs', NULL, 8, 1, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_name = VALUES(menu_name),
  path = VALUES(path),
  sort_order = VALUES(sort_order),
  visible = VALUES(visible),
  status = VALUES(status),
  updated_at = NOW();

INSERT IGNORE INTO sys_role_menus (role_id, menu_id, created_at)
SELECT 1, id, NOW() FROM sys_menus WHERE id IN (64, 65, 66, 67);

INSERT IGNORE INTO sys_role_menus (role_id, menu_id, created_at)
SELECT 2, id, NOW() FROM sys_menus WHERE id IN (65, 66, 67);

INSERT IGNORE INTO sys_role_menus (role_id, menu_id, created_at)
SELECT 4, id, NOW() FROM sys_menus WHERE id IN (64, 65, 66, 67);
