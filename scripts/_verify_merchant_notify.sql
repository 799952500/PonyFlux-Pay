USE payflow_cashier;
SELECT COUNT(*) AS notify_count FROM cashier_merchant_notify;
SELECT notify_id, order_id, summary_status, attempt_count FROM cashier_merchant_notify;

USE payflow_admin;
SELECT id, menu_code, menu_name, path FROM sys_menus WHERE menu_code = 'merchant_notifies';
