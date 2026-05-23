USE payflow_admin;

INSERT INTO sys_menus (
  id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, created_at, updated_at
) VALUES
(13, 10, 'merchant_notifies', '回调记录', 'MENU', '/admin/merchant-notifies', NULL, 3, 1, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  menu_name = VALUES(menu_name),
  path = VALUES(path),
  sort_order = VALUES(sort_order),
  visible = VALUES(visible),
  status = VALUES(status),
  updated_at = NOW();

INSERT IGNORE INTO sys_role_menus (role_id, menu_id, created_at)
SELECT 3, 13, NOW() WHERE EXISTS (SELECT 1 FROM sys_menus WHERE id = 13);
