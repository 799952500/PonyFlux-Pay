-- 侧栏菜单文案：商户回调记录 → 回调记录（与订单管理、退款管理对齐）
USE payflow_admin;
SET NAMES utf8mb4;

UPDATE sys_menus SET menu_name = '回调记录', updated_at = NOW() WHERE id = 13;
