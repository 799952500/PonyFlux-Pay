USE payflow_admin;
SET NAMES utf8mb4;

INSERT INTO sys_roles (id, role_code, role_name, description, status, created_at, updated_at) VALUES
(1, 'SUPER_ADMIN', '超级管理员', '全量菜单', 'ACTIVE', NOW(), NOW()),
(2, 'ADMIN', '管理员', '业务管理', 'ACTIVE', NOW(), NOW()),
(3, 'FINANCE', '财务', '订单与退款', 'ACTIVE', NOW(), NOW()),
(4, 'RISK', '风控', '风控与安全', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), updated_at = NOW();

INSERT INTO sys_menus (
  id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, created_at, updated_at
) VALUES
(1, NULL, 'grp_workspace', '工作台', 'MENU', NULL, NULL, 1, 1, 'ACTIVE', NOW(), NOW()),
(2, 1, 'dashboard', '数据概览', 'MENU', '/admin/dashboard', NULL, 1, 1, 'ACTIVE', NOW(), NOW()),
(101, 1, 'insights_funnel', '支付漏斗', 'MENU', '/admin/insights/funnel', NULL, 2, 1, 'ACTIVE', NOW(), NOW()),
(104, 1, 'churn_alerts', '流失预警', 'MENU', '/admin/dashboard/churn-alerts', NULL, 3, 1, 'ACTIVE', NOW(), NOW()),
(3, 1, 'notifications', '通知中心', 'MENU', '/admin/notifications', NULL, 4, 1, 'ACTIVE', NOW(), NOW()),
(4, 1, 'search', '全局搜索', 'MENU', '/admin/search', NULL, 5, 1, 'ACTIVE', NOW(), NOW()),
(10, NULL, 'grp_trade', '交易与订单', 'MENU', NULL, NULL, 2, 1, 'ACTIVE', NOW(), NOW()),
(11, 10, 'orders', '订单管理', 'MENU', '/admin/orders', NULL, 1, 1, 'ACTIVE', NOW(), NOW()),
(12, 10, 'refunds', '退款管理', 'MENU', '/admin/refunds', NULL, 2, 1, 'ACTIVE', NOW(), NOW()),
(13, 10, 'merchant_notifies', '回调记录', 'MENU', '/admin/merchant-notifies', NULL, 3, 1, 'ACTIVE', NOW(), NOW()),
(60, NULL, 'grp_reconcile', '对账管理', 'MENU', NULL, NULL, 3, 1, 'ACTIVE', NOW(), NOW()),
(310, 60, 'reconcile_work_items', '差异工单', 'MENU', '/admin/reconcile/work-items', NULL, 1, 1, 'ACTIVE', NOW(), NOW()),
(61, 60, 'reconcile_tasks', '对账任务', 'MENU', '/admin/reconcile/tasks', NULL, 2, 1, 'ACTIVE', NOW(), NOW()),
(62, 60, 'reconcile_results', '对账结果', 'MENU', '/admin/reconcile/results', NULL, 3, 1, 'ACTIVE', NOW(), NOW()),
(311, 60, 'reconcile_insights_dashboard', '差异归因看板', 'MENU', '/admin/reconcile/insights-dashboard', NULL, 4, 1, 'ACTIVE', NOW(), NOW()),
(312, 60, 'reconcile_sla_rules', 'SLA 规则', 'MENU', '/admin/reconcile/sla-rules', NULL, 5, 1, 'ACTIVE', 'recon:manage', NULL, NOW(), NOW()),
(313, 60, 'reconcile_long_tail', '长尾差异', 'MENU', '/admin/reconcile/long-tail', NULL, 6, 1, 'ACTIVE', NOW(), NOW()),
(63, 60, 'reconcile_summary', '对账汇总', 'MENU', '/admin/reconcile/summary', NULL, 7, 1, 'ACTIVE', NOW(), NOW()),
(20, NULL, 'grp_channel_account', '渠道与账户', 'MENU', NULL, NULL, 4, 1, 'ACTIVE', NOW(), NOW()),
(21, 20, 'channels', '渠道管理', 'MENU', '/admin/channels', NULL, 1, 1, 'ACTIVE', NOW(), NOW()),
(24, 20, 'payment_methods', '支付方式', 'MENU', '/admin/payment-methods', NULL, 2, 1, 'ACTIVE', NOW(), NOW()),
(25, 20, 'payment_accounts', '支付账号', 'MENU', '/admin/payment-accounts', NULL, 3, 1, 'ACTIVE', NOW(), NOW()),
(68, NULL, 'grp_routing', '路由与费率', 'MENU', NULL, NULL, 5, 1, 'ACTIVE', NOW(), NOW()),
(103, 68, 'channel_routing_health', '路由健康度', 'MENU', '/admin/channel-routing/health', NULL, 2, 1, 'ACTIVE', NOW(), NOW()),
(67, 68, 'routing_logs', '路由决策日志', 'MENU', '/admin/routing/logs', NULL, 3, 1, 'ACTIVE', NOW(), NOW()),
(65, 68, 'fee_rate_config', '阶梯费率配置', 'MENU', '/admin/fee-rate/config', NULL, 4, 1, 'ACTIVE', NOW(), NOW()),
(66, 68, 'fee_rate_audit_log', '费率变更审计', 'MENU', '/admin/fee-rate/audit-log', NULL, 5, 1, 'ACTIVE', NOW(), NOW()),
(30, NULL, 'grp_merchant', '商户与风控', 'MENU', NULL, NULL, 6, 1, 'ACTIVE', NOW(), NOW()),
(31, 30, 'merchants', '商户管理', 'MENU', '/admin/merchants', NULL, 1, 1, 'ACTIVE', NOW(), NOW()),
(102, 30, 'onboarding', '商户进件', 'MENU', '/admin/onboarding', NULL, 2, 1, 'ACTIVE', NOW(), NOW()),
(32, 30, 'risk', '风控配置', 'MENU', '/admin/risk', NULL, 3, 1, 'ACTIVE', NOW(), NOW()),
(40, NULL, 'grp_system', '系统与设置', 'MENU', NULL, NULL, 7, 1, 'ACTIVE', NOW(), NOW()),
(41, 40, 'settings', '系统设置', 'MENU', '/admin/settings', NULL, 1, 1, 'ACTIVE', NOW(), NOW()),
(53, 40, 'users', '用户管理', 'MENU', '/admin/users', NULL, 2, 1, 'ACTIVE', NOW(), NOW()),
(51, 40, 'roles', '角色管理', 'MENU', '/admin/roles', NULL, 3, 1, 'ACTIVE', NOW(), NOW()),
(52, 40, 'menus', '菜单管理', 'MENU', '/admin/menus', NULL, 4, 1, 'ACTIVE', NOW(), NOW()),
(42, 40, 'dicts', '数据字典', 'MENU', '/admin/dicts', NULL, 5, 1, 'ACTIVE', NOW(), NOW()),
(54, 40, 'audit_logs', '操作日志', 'MENU', '/admin/audit-logs', NULL, 6, 1, 'ACTIVE', NOW(), NOW()),
(64, 40, 'security_audit', '安全审计', 'MENU', '/admin/security-audit', NULL, 7, 1, 'ACTIVE', NOW(), NOW()),
(69, 40, 'data_isolation', '数据隔离治理', 'MENU', '/admin/data-isolation', NULL, 8, 1, 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE menu_name = VALUES(menu_name), path = VALUES(path), sort_order = VALUES(sort_order), updated_at = NOW();

DELETE FROM sys_role_menus;
INSERT INTO sys_role_menus (role_id, menu_id, created_at) SELECT 1, id, NOW() FROM sys_menus;
INSERT INTO sys_role_menus (role_id, menu_id, created_at) SELECT 2, id, NOW() FROM sys_menus WHERE id NOT IN (51, 52, 53, 54, 64);
INSERT INTO sys_role_menus (role_id, menu_id, created_at) SELECT 3, id, NOW() FROM sys_menus WHERE id IN (1, 2, 3, 4, 101, 104, 10, 11, 12, 13, 60, 61, 62, 63);
INSERT INTO sys_role_menus (role_id, menu_id, created_at) SELECT 4, id, NOW() FROM sys_menus WHERE id IN (1, 2, 10, 11, 30, 31, 32, 64, 68, 103, 67, 65, 66);
