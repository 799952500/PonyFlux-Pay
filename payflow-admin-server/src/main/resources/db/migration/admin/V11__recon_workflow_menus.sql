-- 对账差异工作流：侧栏菜单（工单 / 看板 / SLA / 长尾）
INSERT INTO admin_sys_menus (id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, perm_code, api_pattern, created_at, updated_at) VALUES
(310, 60, 'reconcile_work_items', '差异工单', 'MENU', '/admin/reconcile/work-items', NULL, 1, 1, 'ACTIVE', NULL, NULL, NOW(), NOW()),
(311, 60, 'reconcile_insights_dashboard', '差异归因看板', 'MENU', '/admin/reconcile/insights-dashboard', NULL, 4, 1, 'ACTIVE', NULL, NULL, NOW(), NOW()),
(312, 60, 'reconcile_sla_rules', 'SLA 规则', 'MENU', '/admin/reconcile/sla-rules', NULL, 5, 1, 'ACTIVE', 'recon:manage', NULL, NOW(), NOW()),
(313, 60, 'reconcile_long_tail', '长尾差异', 'MENU', '/admin/reconcile/long-tail', NULL, 6, 1, 'ACTIVE', NULL, NULL, NOW(), NOW());

-- 调整原有对账子菜单排序（工单置顶）
UPDATE admin_sys_menus SET sort_order = 2 WHERE id = 61;
UPDATE admin_sys_menus SET sort_order = 3 WHERE id = 62;
UPDATE admin_sys_menus SET sort_order = 7 WHERE id = 63;

-- 角色授权
INSERT INTO admin_sys_role_menus (role_id, menu_id, created_at)
SELECT 1, id, NOW() FROM admin_sys_menus WHERE id IN (310, 311, 312, 313);

INSERT INTO admin_sys_role_menus (role_id, menu_id, created_at)
SELECT 2, id, NOW() FROM admin_sys_menus WHERE id IN (310, 311, 312, 313);

INSERT INTO admin_sys_role_menus (role_id, menu_id, created_at)
SELECT 3, id, NOW() FROM admin_sys_menus WHERE id IN (310, 311, 313);

INSERT INTO admin_sys_role_menus (role_id, menu_id, created_at)
SELECT 4, id, NOW() FROM admin_sys_menus WHERE id IN (310, 311, 313);
