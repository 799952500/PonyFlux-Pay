-- 商户支付配置并入「商户管理」行内弹窗，隐藏独立菜单入口
USE payflow_admin;
SET NAMES utf8mb4;

DELETE FROM sys_role_menus WHERE menu_id = 23;

UPDATE sys_menus
SET visible = 0, status = 'DISABLED', updated_at = NOW()
WHERE id = 23 OR path = '/admin/merchant-payments' OR menu_code = 'merchant_payments';
