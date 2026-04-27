-- 初始化角色
INSERT IGNORE INTO sys_roles (role_code, role_name, description, status) VALUES
('SUPER_ADMIN', '超级管理员', '拥有所有权限', 'ACTIVE'),
('ADMIN', '管理员', '常规管理权限', 'ACTIVE'),
('FINANCE', '财务', '查看订单和退款', 'ACTIVE'),
('RISK', '风控', '管理风控规则', 'ACTIVE');

-- 初始化菜单
INSERT IGNORE INTO sys_menus (id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order) VALUES
(1, NULL, 'dashboard', '数据概览', 'MENU', '/admin/dashboard', '📊', 1),
(2, NULL, 'order-group', '订单管理', 'MENU', NULL, '📋', 2),
(3, 2, 'orders', '订单列表', 'MENU', '/admin/orders', NULL, 1),
(4, 2, 'refunds', '退款管理', 'MENU', '/admin/refunds', NULL, 2),
(5, NULL, 'channels', '渠道管理', 'MENU', '/admin/channels', '💳', 3),
(6, NULL, 'payment-group', '支付管理', 'MENU', NULL, '💰', 4),
(7, 6, 'payment-methods', '支付方式', 'MENU', '/admin/payment-methods', NULL, 1),
(8, 6, 'payment-accounts', '支付账号', 'MENU', '/admin/payment-accounts', NULL, 2),
(9, NULL, 'merchants', '商户管理', 'MENU', '/admin/merchants', '🏪', 5),
(10, NULL, 'risk', '风控管理', 'MENU', '/admin/risk', '⚖️', 6),
(11, NULL, 'settings', '系统设置', 'MENU', '/admin/settings', '⚙️', 7),
(12, NULL, 'sys-group', '系统管理', 'MENU', NULL, '🔧', 8),
(13, 12, 'roles', '角色管理', 'MENU', '/admin/roles', NULL, 1),
(14, 12, 'sys-menus', '菜单管理', 'MENU', '/admin/menus', NULL, 2);

-- 超级管理员拥有所有菜单
INSERT IGNORE INTO sys_role_menus (role_id, menu_id) SELECT 1, id FROM sys_menus;

-- 管理员拥有除系统管理外的菜单
INSERT IGNORE INTO sys_role_menus (role_id, menu_id) SELECT 2, id FROM sys_menus WHERE id NOT IN (12,13,14);

-- 财务只有订单相关和首页
INSERT IGNORE INTO sys_role_menus (role_id, menu_id) SELECT 3, id FROM sys_menus WHERE id IN (1,2,3,4);

-- 风控只有风控和订单首页
INSERT IGNORE INTO sys_role_menus (role_id, menu_id) SELECT 4, id FROM sys_menus WHERE id IN (1,2,3,10);

-- 为现有admin用户分配SUPER_ADMIN角色
INSERT IGNORE INTO sys_user_roles (user_id, role_id) SELECT id, 1 FROM admin_users WHERE username = 'admin';
