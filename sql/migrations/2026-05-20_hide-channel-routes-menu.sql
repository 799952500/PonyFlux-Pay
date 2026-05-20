-- 支付路由并入「商户管理」行内「支付配置」，隐藏独立菜单入口
USE payflow_admin;
SET NAMES utf8mb4;

DELETE FROM sys_role_menus WHERE menu_id = 22;

UPDATE sys_menus
SET visible = 0, status = 'DISABLED', updated_at = NOW()
WHERE id = 22 OR path = '/admin/channel-routes' OR menu_code = 'channel_routes';
