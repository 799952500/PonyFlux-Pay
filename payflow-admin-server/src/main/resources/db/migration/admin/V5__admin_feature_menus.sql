-- 新增功能菜单入口（用于前端后台可见）
-- 执行前 USE payflow_admin;

SET NAMES utf8mb4;

-- 放到「工作台」分组下（你的库里该分组 id=1，menu_code=grp_workspace）
INSERT INTO sys_menus (
  id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, created_at, updated_at
) VALUES
(101, 1, 'insights_funnel', '支付漏斗', 'MENU', '/admin/insights/funnel', NULL, 90, 1, 'ACTIVE', NOW(), NOW()),
(102, 1, 'onboarding', '商户进件', 'MENU', '/admin/onboarding', NULL, 91, 1, 'ACTIVE', NOW(), NOW()),
(103, 1, 'channel_routing_health', '路由健康度', 'MENU', '/admin/channel-routing/health', NULL, 92, 1, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_name = VALUES(menu_name),
  path = VALUES(path),
  sort_order = VALUES(sort_order),
  visible = VALUES(visible),
  status = VALUES(status),
  updated_at = NOW();

-- SUPER_ADMIN 角色补全新菜单
INSERT IGNORE INTO sys_role_menus (role_id, menu_id, created_at)
SELECT 1, id, NOW() FROM sys_menus WHERE id IN (101, 102, 103);

