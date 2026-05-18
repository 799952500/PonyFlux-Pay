-- 安全审计菜单（系统管理下）
SET NAMES utf8mb4;

INSERT INTO sys_menus (id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, created_at, updated_at)
VALUES (64, 12, 'security_audit', '安全审计', 'MENU', '/admin/security-audit', NULL, 5, 1, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_name = VALUES(menu_name),
  path = VALUES(path),
  sort_order = VALUES(sort_order),
  visible = VALUES(visible),
  status = VALUES(status),
  updated_at = NOW();

INSERT IGNORE INTO sys_role_menus (role_id, menu_id, created_at)
SELECT 1, 64, NOW();

INSERT IGNORE INTO sys_role_menus (role_id, menu_id, created_at)
SELECT 4, 64, NOW();
