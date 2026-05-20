-- 侧栏菜单重组为 7 组（与前端 layout 静态 fallback 一致）
-- 执行前: USE payflow_admin;

SET NAMES utf8mb4;

-- 新增分组与菜单
INSERT INTO sys_menus (id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, created_at, updated_at)
VALUES
(68, NULL, 'grp_routing', '路由与费率', 'MENU', NULL, NULL, 5, 1, 'ACTIVE', NOW(), NOW()),
(104, 1, 'churn_alerts', '流失预警', 'MENU', '/admin/dashboard/churn-alerts', NULL, 3, 1, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  parent_id = VALUES(parent_id),
  menu_name = VALUES(menu_name),
  path = VALUES(path),
  sort_order = VALUES(sort_order),
  visible = VALUES(visible),
  status = VALUES(status),
  updated_at = NOW();

-- 分组重命名与排序
UPDATE sys_menus SET menu_name = '工作台', menu_code = 'grp_workspace', sort_order = 1, icon = NULL, parent_id = NULL WHERE id = 1;
UPDATE sys_menus SET menu_name = '交易与订单', menu_code = 'grp_trade', sort_order = 2, icon = NULL WHERE id = 10;
UPDATE sys_menus SET menu_name = '对账管理', sort_order = 3, icon = NULL WHERE id = 60;
UPDATE sys_menus SET menu_name = '渠道与账户', menu_code = 'grp_channel_account', sort_order = 4, icon = NULL WHERE id = 20;
UPDATE sys_menus SET menu_name = '商户与风控', sort_order = 6, icon = NULL WHERE id = 30;
UPDATE sys_menus SET menu_name = '系统与设置', menu_code = 'grp_system', sort_order = 7, icon = NULL WHERE id = 40;

-- 工作台子项
UPDATE sys_menus SET parent_id = 1, sort_order = 1 WHERE id = 2;
UPDATE sys_menus SET parent_id = 1, menu_name = '支付漏斗', sort_order = 2 WHERE id = 101;
UPDATE sys_menus SET parent_id = 1, sort_order = 4 WHERE id = 3;
UPDATE sys_menus SET parent_id = 1, sort_order = 5 WHERE id = 4;

-- 渠道与账户（渠道 / 支付方式 / 支付账号；商户支付配置已并入商户管理）
UPDATE sys_menus SET parent_id = 20, sort_order = 1 WHERE id = 21;
UPDATE sys_menus SET parent_id = 20, sort_order = 2 WHERE id = 24;
UPDATE sys_menus SET parent_id = 20, sort_order = 3 WHERE id = 25;
UPDATE sys_menus SET visible = 0, status = 'DISABLED' WHERE id = 23;

-- 路由与费率
UPDATE sys_menus SET parent_id = 68, sort_order = 1 WHERE id = 22;
UPDATE sys_menus SET parent_id = 68, menu_name = '路由健康度', sort_order = 2 WHERE id = 103;
UPDATE sys_menus SET parent_id = 68, sort_order = 3 WHERE id = 67;
UPDATE sys_menus SET parent_id = 68, sort_order = 4 WHERE id = 65;
UPDATE sys_menus SET parent_id = 68, sort_order = 5 WHERE id = 66;

-- 商户与风控：进件归入本组
UPDATE sys_menus SET parent_id = 30, menu_name = '商户进件', sort_order = 2 WHERE id = 102;
UPDATE sys_menus SET parent_id = 30, sort_order = 3 WHERE id = 32;

-- 系统与设置：合并原「权限运维」子项
UPDATE sys_menus SET parent_id = 40, sort_order = 1 WHERE id = 41;
UPDATE sys_menus SET parent_id = 40, sort_order = 2 WHERE id = 53;
UPDATE sys_menus SET parent_id = 40, sort_order = 3 WHERE id = 51;
UPDATE sys_menus SET parent_id = 40, sort_order = 4 WHERE id = 52;
UPDATE sys_menus SET parent_id = 40, sort_order = 5 WHERE id = 42;
UPDATE sys_menus SET parent_id = 40, sort_order = 6 WHERE id = 54;
UPDATE sys_menus SET parent_id = 40, sort_order = 7 WHERE id = 64;

-- 废弃原「权限运维」空分组
DELETE FROM sys_role_menus WHERE menu_id = 50;
DELETE FROM sys_menus WHERE id = 50;

-- SUPER_ADMIN 补全新菜单
INSERT IGNORE INTO sys_role_menus (role_id, menu_id, created_at)
SELECT 1, id, NOW() FROM sys_menus WHERE id IN (68, 104);

-- 财务角色补全工作台新入口
INSERT IGNORE INTO sys_role_menus (role_id, menu_id, created_at)
SELECT 3, id, NOW() FROM sys_menus WHERE id IN (101, 104) AND NOT EXISTS (
  SELECT 1 FROM sys_role_menus rm WHERE rm.role_id = 3 AND rm.menu_id = sys_menus.id
);

-- 风控角色补全路由分组
INSERT IGNORE INTO sys_role_menus (role_id, menu_id, created_at)
SELECT 4, id, NOW() FROM sys_menus WHERE id IN (68, 22, 103, 67, 65, 66) AND NOT EXISTS (
  SELECT 1 FROM sys_role_menus rm WHERE rm.role_id = 4 AND rm.menu_id = sys_menus.id
);
