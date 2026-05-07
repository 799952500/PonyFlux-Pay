-- 对账管理菜单（按支付账号对账子页）+ 从「交易」下移除旧单笔对账入口
-- 执行前 USE payflow_admin;
-- 若库中曾使用 full-reseed 的 id=13「资金对账」，请先解除角色关联再删菜单。

DELETE FROM sys_role_menus WHERE menu_id = 13;
DELETE FROM sys_menus WHERE id = 13;

INSERT INTO sys_menus (
  id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, created_at, updated_at
) VALUES
(60, NULL, 'grp_reconcile', '对账管理', 'MENU', NULL, '📒', 15, 1, 'ACTIVE', NOW(), NOW()),
(61, 60, 'reconcile_tasks', '对账任务', 'MENU', '/admin/reconcile/tasks', NULL, 1, 1, 'ACTIVE', NOW(), NOW()),
(62, 60, 'reconcile_results', '对账结果', 'MENU', '/admin/reconcile/results', NULL, 2, 1, 'ACTIVE', NOW(), NOW()),
(63, 60, 'reconcile_summary', '对账汇总', 'MENU', '/admin/reconcile/summary', NULL, 3, 1, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  menu_name = VALUES(menu_name),
  path = VALUES(path),
  parent_id = VALUES(parent_id),
  icon = VALUES(icon),
  sort_order = VALUES(sort_order),
  updated_at = NOW();

-- 超级管理员补全新菜单（已存在则忽略）
INSERT IGNORE INTO sys_role_menus (role_id, menu_id, created_at)
SELECT 1, id, NOW() FROM sys_menus WHERE id IN (60, 61, 62, 63);

-- 财务角色示例：与订单同权时可按需加入 60–63
-- INSERT IGNORE INTO sys_role_menus (role_id, menu_id, created_at) SELECT 3, id, NOW() FROM sys_menus WHERE id IN (60, 61, 62, 63);
