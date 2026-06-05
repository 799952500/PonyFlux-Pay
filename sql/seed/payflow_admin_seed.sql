-- =============================================================================
-- PonyFlux Pay — 运营库 payflow_admin 演示数据
-- 覆盖管理后台各列表页、仪表盘、对账、费率、进件、RBAC
-- 登录：admin / admin123（BCrypt）
-- =============================================================================
USE payflow_admin;
SET NAMES utf8mb4;

-- BCrypt(admin123)
SET @pwd = '$2b$10$UHTRg4BSLqaHosl88JbOE.WOCrOmMusFph5Jws0aEEOKrMPq4Px5a';

INSERT INTO admin_users (id, username, password, role, nickname, status, data_merchant_ids, created_at, updated_at) VALUES
(1, 'admin', @pwd, 'SUPER_ADMIN', '超级管理员', 'ACTIVE', NULL, NOW(), NOW()),
(2, 'finance_demo', @pwd, 'FINANCE', '财务-张敏', 'ACTIVE', 'M100001', NOW(), NOW()),
(3, 'risk_demo', @pwd, 'RISK', '风控-陈磊', 'ACTIVE', NULL, NOW(), NOW()),
(4, 'merchant_m100001', @pwd, 'ADMIN', '星云零售商户管理员', 'ACTIVE', 'M100001', NOW(), NOW()),
(5, 'merchant_m100002', @pwd, 'ADMIN', '蓝海餐饮商户管理员', 'ACTIVE', 'M100002', NOW(), NOW());

INSERT INTO admin_channels (
  id, channel_code, channel_name, channel_type, api_url, api_key, enabled, priority, icon, description, fee_rate, created_at, updated_at
) VALUES
(1, 'WECHAT_PAY', '微信支付', 'WECHAT', 'https://api.mch.weixin.qq.com', 'demo-wechat-key', 1, 100, '💚', '微信官方渠道', 0.0060, NOW(), NOW()),
(2, 'ALIPAY', '支付宝', 'ALIPAY', 'https://openapi.alipay.com', 'demo-alipay-key', 1, 90, '🔵', '支付宝官方渠道', 0.0055, NOW(), NOW()),
(3, 'UNION_PAY', '银联云闪付', 'UNION', 'https://gateway.95516.com', 'demo-union-key', 1, 80, '🟦', '银联云闪付', 0.0050, NOW(), NOW());

INSERT INTO admin_merchants (
  id, merchant_id, merchant_name, merchant_key, callback_url, notify_url, commission_rate,
  rate_calc_mode, merchant_group, status, created_at, updated_at
) VALUES
(1, 'M100001', '星云零售旗舰店', 'merchant_key_demo_m100001_xxxxxxxx', 'https://m100001.demo/cb', 'https://m100001.demo/notify', 0.0060, 'segmented', 'retail_vip', 'ACTIVE', NOW(), NOW()),
(2, 'M100002', '蓝海餐饮连锁', 'merchant_key_demo_m100002_xxxxxxxx', 'https://m100002.demo/cb', 'https://m100002.demo/notify', 0.0055, 'flat', 'catering_std', 'ACTIVE', NOW(), NOW()),
(3, 'M100003', '青禾教育培训', 'merchant_key_demo_m100003_xxxxxxxx', 'https://m100003.demo/cb', 'https://m100003.demo/notify', 0.0065, 'flat', 'edu_std', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 30 DAY), NOW());

INSERT INTO admin_payment_accounts (
  id, channel_id, account_code, account_name, app_id, app_secret, mch_id, mch_key,
  config_json, enabled, priority, description, created_at, updated_at
) VALUES
(1, 1, 'ACC-WX-001', '微信华东主账户', 'wx_demo_appid', 'wx_demo_secret', 'wx_mch_001', 'wx_mch_key_demo', '{"pool":"A"}', 1, 100, '演示微信', NOW(), NOW()),
(2, 2, 'ACC-ALI-001', '支付宝华东主账户', 'ali_demo_appid', 'ali_demo_secret', 'ali_pid_001', 'ali_key_demo', '{"pool":"B"}', 1, 100, '演示支付宝', NOW(), NOW()),
(3, 2, 'ACC-ALI-002', '支付宝备用账户', 'ali_demo_appid_2', 'ali_demo_secret_2', 'ali_pid_002', 'ali_key_demo_2', '{"pool":"C"}', 1, 90, '演示支付宝备用', NOW(), NOW()),
(4, 3, 'ACC-UNION-001', '银联华东主账户', NULL, NULL, 'union_mch_001', NULL, '{"env":"sandbox"}', 1, 80, '演示银联', NOW(), NOW());

-- 支付方式仅存能力编码与场景参数；appId/密钥在 payment_accounts
INSERT INTO admin_payment_methods (
  id, method_code, method_name,
  method_name_zh_cn, method_name_zh_tw, method_name_en,
  channel_id, app_id, enabled, priority, description,
  description_zh_cn, description_zh_tw, description_en,
  config_json, created_at, updated_at
) VALUES
(1, 'WECHAT_APP', '微信 App 支付', '微信 App 支付', '微信 App 支付', 'WeChat Pay App', 1, NULL, 1, 100, 'App 内调起', 'App 内调起', 'App 內調起', 'In-app WeChat Pay', '{"tradeType":"APP"}', NOW(), NOW()),
(2, 'WECHAT_H5', '微信 H5 支付', '微信 H5 支付', '微信 H5 支付', 'WeChat Pay H5', 1, NULL, 1, 90, '手机浏览器', '手机浏览器', '手機瀏覽器', 'Mobile browser', '{"tradeType":"MWEB"}', NOW(), NOW()),
(3, 'ALIPAY_APP', '支付宝 App 支付', '支付宝 App 支付', '支付寶 App 支付', 'Alipay App', 2, NULL, 1, 100, 'App 支付', 'App 支付', 'App 支付', 'In-app Alipay', '{"payType":"APP"}', NOW(), NOW()),
(4, 'ALIPAY_WAP', '支付宝 WAP', '支付宝 WAP', '支付寶 WAP', 'Alipay WAP', 2, NULL, 1, 90, '手机网站', '手机网站', '手機網站', 'Mobile web', '{"payWay":"WAP"}', NOW(), NOW()),
(5, 'UNION_H5', '银联云闪付 H5', '银联云闪付 H5', '銀聯雲閃付 H5', 'UnionPay H5', 3, NULL, 1, 80, 'H5 收银台', 'H5 收银台', 'H5 收銀台', 'H5 checkout', '{"payType":"H5"}', NOW(), NOW()),
(6, 'UNION_QR', '银联扫码支付', '银联扫码支付', '銀聯掃碼支付', 'UnionPay QR', 3, NULL, 1, 70, '用户扫码', '用户扫码', '用戶掃碼', 'Scan to pay', '{"payType":"QR"}', NOW(), NOW());

INSERT INTO admin_merchant_payment_methods (id, merchant_id, payment_method_id, enabled, priority, created_at, updated_at) VALUES
(1, 'M100001', 1, 1, 100, NOW(), NOW()),
(2, 'M100001', 2, 1, 90, NOW(), NOW()),
(3, 'M100001', 3, 1, 100, NOW(), NOW()),
(4, 'M100001', 6, 1, 60, NOW(), NOW()),
(5, 'M100002', 2, 1, 90, NOW(), NOW()),
(6, 'M100002', 4, 1, 90, NOW(), NOW()),
(7, 'M100003', 1, 1, 100, NOW(), NOW()),
(8, 'M100003', 2, 1, 90, NOW(), NOW());

INSERT INTO admin_merchant_payment_routes (
  id, merchant_id, payment_method_id, payment_account_id, enabled, priority, client_scopes, created_at, updated_at
) VALUES
(1, 'M100001', 1, 1, 1, 100, 'PC,APP', NOW(), NOW()),
(2, 'M100001', 2, 1, 1, 90, 'H5', NOW(), NOW()),
(3, 'M100001', 3, 2, 1, 100, 'PC,H5,APP', NOW(), NOW()),
(4, 'M100002', 4, 3, 1, 80, 'PC,H5,APP', NOW(), NOW()),
(5, 'M100003', 1, 1, 1, 100, 'APP', NOW(), NOW());

INSERT INTO admin_channel_routes (
  id, merchant_id, channel_id, payment_account_id, enabled, priority, description, created_at, updated_at
) VALUES
(1, 'M100001', 1, 1, 1, 10, '星云零售 → 微信主账户', NOW(), NOW()),
(2, 'M100001', 2, 2, 1, 20, '星云零售 → 支付宝主账户', NOW(), NOW()),
(3, 'M100002', 2, 3, 1, 30, '蓝海餐饮 → 支付宝备用', NOW(), NOW()),
(4, 'M100001', 3, 4, 1, 40, '星云零售 → 银联', NOW(), NOW());

INSERT INTO admin_risk_rules (
  id, rule_code, rule_name, rule_type, risk_expr, threshold, threshold_fen, unit, action, enabled,
  priority, owner_type, owner_merchant_id, scope_type, description, created_by, updated_by, created_at, updated_at
) VALUES
(1, 'RISK_AMT_SINGLE_GLOBAL', '平台全商户单笔限额', 'AMOUNT_SINGLE', NULL, 500000.00, 500000, 'CNY_FEN', 'REJECT', 1, 10, 'PLATFORM', NULL, 'ALL_MERCHANTS', '平台全局：单笔超过 5000 元拒绝', 'admin', 'admin', NOW(), NOW()),
(2, 'RISK_AMT_SINGLE_SELECTED', '平台定向单笔限额', 'AMOUNT_SINGLE', NULL, 300000.00, 300000, 'CNY_FEN', 'REJECT', 1, 20, 'PLATFORM', NULL, 'SELECTED_MERCHANTS', '仅指定商户单笔超过 3000 元拒绝', 'admin', 'admin', NOW(), NOW()),
(3, 'RISK_M100001_SINGLE_LIMIT', '星云零售自建单笔限额', 'AMOUNT_SINGLE', NULL, 100000.00, 100000, 'CNY_FEN', 'REJECT', 1, 100, 'MERCHANT', 'M100001', 'OWNER_MERCHANT_ONLY', '商户自建：单笔超过 1000 元拒绝', 'M100001', 'M100001', NOW(), NOW());

INSERT INTO admin_risk_rule_merchant_scope (id, rule_id, merchant_id, enabled, created_at) VALUES
(1, 2, 'M100001', 1, NOW()),
(2, 2, 'M100002', 1, NOW());

INSERT INTO admin_risk_rule_audit_log (
  id, rule_id, operator_id, operator_name, operator_type, merchant_id, operation_type, before_summary, after_summary, client_ip, created_at
) VALUES
(1, 1, '1', '超级管理员', 'ADMIN', NULL, 'CREATE', NULL, '创建平台全商户单笔限额规则', '127.0.0.1', NOW()),
(2, 2, '1', '超级管理员', 'ADMIN', NULL, 'SCOPE_CHANGE', NULL, '指定商户：M100001,M100002', '127.0.0.1', NOW()),
(3, 3, 'M100001', '星云零售旗舰店', 'MERCHANT', 'M100001', 'CREATE', NULL, '创建商户自建单笔限额规则', '127.0.0.1', NOW());

INSERT INTO admin_system_configs (
  id, config_key, config_value, value_type, category, description, sort_order, status, created_at, updated_at
) VALUES
(1, 'payment.timeout_minutes', '30', 'NUMBER', 'payment', '下单支付超时（分钟）', 10, 1, NOW(), NOW()),
(2, 'payment.notify_retry_max', '5', 'NUMBER', 'payment', '异步通知最大重试次数', 20, 1, NOW(), NOW()),
(3, 'risk.enabled', 'true', 'BOOLEAN', 'risk', '是否启用风控拦截', 10, 1, NOW(), NOW()),
(4, 'fee.platform_rate', '0.0025', 'NUMBER', 'fee', '平台基础费率', 10, 1, NOW(), NOW()),
(5, 'system.site_name', 'PonyFlux Pay 演示环境', 'STRING', 'system', '站点名称', 10, 1, NOW(), NOW()),
(6, 'system.support_email', 'support@demo.payflow.local', 'STRING', 'system', '支持邮箱', 20, 1, NOW(), NOW());

INSERT INTO admin_audit_logs (username, operator_type, merchant_id, action, resource_path, resource_type, resource_id, detail, result, deny_reason, client_ip, created_at) VALUES
('admin', 'SYSTEM_ADMIN', NULL, 'POST', '/api/v1/admin/auth/login', 'AUTH', NULL, '登录成功', 'SUCCESS', NULL, '127.0.0.1', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
('admin', 'SYSTEM_ADMIN', NULL, 'PUT', '/api/v1/admin/channels/1', 'CHANNEL', '1', '更新渠道：微信支付', 'SUCCESS', NULL, '127.0.0.1', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
('finance_demo', 'MERCHANT_ADMIN', 'M100001', 'GET', '/api/v1/admin/orders', 'ORDER', NULL, '查询订单列表', 'SUCCESS', NULL, '10.0.0.8', DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
('admin', 'SYSTEM_ADMIN', 'M100001', 'POST', '/api/v1/admin/refunds/REF-20260518-001/reject', 'REFUND', 'REF-20260518-001', '拒绝退款', 'SUCCESS', NULL, '127.0.0.1', DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
('risk_demo', 'SYSTEM_ADMIN', NULL, 'GET', '/api/v1/admin/security/audit', 'SECURITY_AUDIT', NULL, '查询安全审计', 'SUCCESS', NULL, '10.0.0.12', NOW()),
('merchant_m100001', 'MERCHANT_ADMIN', 'M100001', 'GET', '/api/v1/admin/orders/ORD-20260518-0003', 'ORDER', 'ORD-20260518-0003', '跨商户订单详情访问被拒绝', 'DENIED', '授权商户范围不包含目标资源', '10.0.0.21', DATE_SUB(NOW(), INTERVAL 20 MINUTE)),
('merchant_m100002', 'MERCHANT_ADMIN', 'M100002', 'POST', '/api/v1/admin/refunds/REF-20260518-001/approve', 'REFUND', 'REF-20260518-001', '跨商户退款审核被拒绝', 'DENIED', '授权商户范围不包含目标资源', '10.0.0.22', DATE_SUB(NOW(), INTERVAL 18 MINUTE));

INSERT INTO recon_task (
  task_id, channel, account_code, bill_date, bill_type, status,
  bill_total_count, bill_total_amount, local_total_count, local_total_amount,
  diff_count, elapsed_ms, triggered_by, created_at, updated_at
) VALUES
('RECON-20260517-001', 'alipay', 'CASHIER_ALI_001', DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'trade', 'SUCCESS',
 3, 43200, 2, 38700, 2, 2850, 'XXL_JOB', NOW(), NOW()),
('RECON-20260516-001', 'wxpay', 'CASHIER_WX_001', DATE_SUB(CURDATE(), INTERVAL 2 DAY), 'trade', 'SUCCESS',
  5, 198900, 4, 178800, 1, 3100, 'MANUAL', DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
('RECON-20260515-FAIL', 'wxpay', 'CASHIER_WX_001', DATE_SUB(CURDATE(), INTERVAL 3 DAY), 'trade', 'FAIL',
  NULL, NULL, NULL, NULL, 0, NULL, 'XXL_JOB', DATE_SUB(NOW(), INTERVAL 2 DAY), NOW());

INSERT INTO recon_bill_record (
  task_id, channel, channel_trade_no, out_trade_no, amount_fen, refund_fen, channel_status, finish_time, raw_line, parse_error
) VALUES
('RECON-20260517-001', 'alipay', '2026051822001234567', 'ORD-20260518-0003', 8800, 0, 'SUCCESS', NOW(), 'demo,csv,line', 0),
('RECON-20260517-001', 'alipay', 'ALI_ONLY_999', NULL, 100, 0, 'SUCCESS', NOW(), 'channel_only', 0),
('RECON-20260516-001', 'wxpay', '4200001234567890', 'ORD-20260518-0001', 29900, 0, 'SUCCESS', NOW(), 'wx,line', 0);

INSERT INTO recon_diff (
  task_id, diff_type, channel_trade_no, local_order_id,
  channel_amount, local_amount, channel_status, local_status, handle_status, suggested_action, created_at
) VALUES
('RECON-20260517-001', 'AMOUNT_MISMATCH', '2026051822001234567', 'ORD-20260518-0003', 8801, 8800, 'SUCCESS', 'PAID', 'PENDING', 'MANUAL_REVIEW', NOW()),
('RECON-20260517-001', 'CHANNEL_ONLY', 'ALI_ONLY_999', NULL, 100, NULL, 'SUCCESS', NULL, 'PENDING', 'AUTO_QUERY', NOW()),
('RECON-20260516-001', 'LOCAL_ONLY', NULL, 'ORD-20260516-0007', NULL, 6600, NULL, 'PAYING', 'PENDING', 'REVIEW', NOW());

-- 对账差异 SLA 默认规则（用于演示）
INSERT INTO recon_diff_sla_rule (diff_type, enabled, sla_hours, due_soon_ratio, escalate_to_role, updated_by, updated_at)
VALUES
('AMOUNT_MISMATCH', 1, 24, 0.2000, 'recon:manage', 'seed', NOW()),
('STATUS_MISMATCH', 1, 12, 0.2000, 'recon:manage', 'seed', NOW()),
('CHANNEL_ONLY', 1, 8, 0.2000, 'recon:manage', 'seed', NOW()),
('LOCAL_ONLY', 1, 8, 0.2000, 'recon:manage', 'seed', NOW())
ON DUPLICATE KEY UPDATE
  enabled = VALUES(enabled),
  sla_hours = VALUES(sla_hours),
  due_soon_ratio = VALUES(due_soon_ratio),
  escalate_to_role = VALUES(escalate_to_role),
  updated_by = VALUES(updated_by),
  updated_at = VALUES(updated_at);

-- 对账报告订阅示例（用于演示）
INSERT INTO recon_report_subscription (subscriber_id, report_type, scope, enabled, created_at, updated_at)
VALUES
('admin', 'DAILY', 'ALL_AUTHORIZED', 1, NOW(), NOW()),
('admin', 'WEEKLY', 'ALL_AUTHORIZED', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  scope = VALUES(scope),
  enabled = VALUES(enabled),
  updated_at = VALUES(updated_at);

INSERT INTO admin_dashboard_metrics (
  metric_time, granularity, channel_code, total_amount, total_count, active_merchants, fee_income, refund_amount, refund_count
) VALUES
(DATE_SUB(CURDATE(), INTERVAL 6 DAY), 'day', 'ALL', 125600, 18, 2, 754, 0, 0),
(DATE_SUB(CURDATE(), INTERVAL 5 DAY), 'day', 'ALL', 198900, 24, 3, 1193, 5600, 1),
(DATE_SUB(CURDATE(), INTERVAL 4 DAY), 'day', 'ALL', 87600, 15, 2, 526, 0, 0),
(DATE_SUB(CURDATE(), INTERVAL 3 DAY), 'day', 'ALL', 256000, 32, 3, 1536, 5600, 1),
(DATE_SUB(CURDATE(), INTERVAL 2 DAY), 'day', 'ALL', 312400, 41, 3, 1874, 0, 0),
(DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'day', 'ALL', 428800, 52, 3, 2573, 4400, 1),
(CURDATE(), 'day', 'ALL', 186700, 22, 3, 1120, 38700, 2),
(DATE_FORMAT(NOW(), '%Y-%m-%d %H:00:00'), 'hour', 'WECHAT_PAY', 29900, 3, 2, 179, 9900, 1),
(DATE_FORMAT(NOW(), '%Y-%m-%d %H:00:00'), 'hour', 'ALIPAY', 8800, 2, 1, 48, 0, 0);

INSERT INTO admin_churn_alert (
  merchant_id, merchant_name, alert_level, current_avg_count, baseline_avg_count, decline_pct, consecutive_days, status, assignee, note
) VALUES
(2, '蓝海餐饮连锁', 'yellow', 8.20, 12.50, 34.40, 2, 'pending', NULL, '近 7 天笔数下滑，待运营跟进'),
(3, '青禾教育培训', 'orange', 3.10, 8.00, 61.25, 4, 'following', 'risk_demo', '暑期班结束，交易量季节性下降'),
(1, '星云零售旗舰店', 'red', 15.00, 28.00, 46.43, 5, 'pending', NULL, '连续 5 日下降，建议客户经理回访');

INSERT INTO admin_fee_rate_config (
  scope_type, scope_value, channel_code, tier_min, tier_max, fee_rate, calc_mode, priority, status
) VALUES
('global', NULL, 'ALL', 0, 1000000, 0.0060, 'flat', 0, 'enabled'),
('global', NULL, 'ALL', 1000000, 10000000, 0.0055, 'flat', 0, 'enabled'),
('global', NULL, 'ALL', 10000000, NULL, 0.0050, 'flat', 0, 'enabled'),
('merchant_group', 'retail_vip', 'WECHAT_PAY', 0, NULL, 0.0058, 'segmented', 10, 'enabled'),
('merchant_group', 'catering_std', 'ALIPAY', 0, 5000000, 0.0052, 'flat', 5, 'enabled');

INSERT INTO admin_fee_rate_audit_log (merchant_id, change_time, old_rate, new_rate, trigger_reason, operator) VALUES
(1, DATE_SUB(NOW(), INTERVAL 7 DAY), 0.0065, 0.0060, 'monthly_upgrade', 'admin'),
(2, DATE_SUB(NOW(), INTERVAL 3 DAY), 0.0060, 0.0055, 'manual_adjust', 'finance_demo'),
(1, DATE_SUB(NOW(), INTERVAL 1 DAY), 0.0060, 0.0058, 'merchant_group_change', 'admin');

INSERT INTO admin_merchant_fee_snapshot (
  merchant_id, snapshot_month, applicable_rate, monthly_amount, current_tier, next_tier_amount, next_tier_rate, calc_mode
) VALUES
(1, DATE_FORMAT(NOW(), '%Y-%m'), 0.0058, 1867000, 2, 8133000, 0.0055, 'segmented'),
(2, DATE_FORMAT(NOW(), '%Y-%m'), 0.0055, 34400, 0, 965600, 0.0055, 'flat');

INSERT INTO admin_routing_decision_log (
  trade_no, merchant_id, available_channels, selected_channel, selection_reason, decision_cost_ms, fallback_count, create_time
) VALUES
('ORD-20260518-0001', 1, '[{"code":"WECHAT_PAY","rate":0.006,"available":true},{"code":"ALIPAY","rate":0.0055,"available":true}]', 'ALIPAY', 'lowest_cost', 12, 0, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
('ORD-20260517-0005', 1, '[{"code":"UNION_PAY","rate":0.005,"available":true}]', 'UNION_PAY', 'lowest_cost', 8, 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
('ORD-20260518-0003', 2, '[{"code":"ALIPAY","rate":0.0055,"available":true}]', 'ALIPAY', 'lowest_cost', 15, 0, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
('ORD-20260516-0007', 1, '[{"code":"WECHAT_PAY","rate":0.006,"available":false},{"code":"ALIPAY","rate":0.0055,"available":true}]', 'ALIPAY', 'fallback', 45, 1, NOW()),
('ORD-20260515-0008', 2, '[{"code":"ALIPAY","rate":0.0052,"available":true}]', 'ALIPAY', 'lowest_cost', 11, 0, DATE_SUB(NOW(), INTERVAL 3 DAY));

INSERT INTO admin_merchant_application (
  application_no, merchant_name, status, application_source, biz_license_no, contact_name, contact_phone, contact_email,
  allocated_merchant_id, payload_json, created_at, updated_at
) VALUES
('KYB-20260518-001', '晨曦便利店', 'SUBMITTED', 'CASHIER_PUBLIC', '91310000MA1XXXX001', '赵晨曦', '13900001111', 'chenxi@demo.local',
 NULL, '{"businessScope":"retail","stores":12}', DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
('KYB-20260510-002', '悦动健身工作室', 'APPROVED', 'CASHIER_PUBLIC', '91310000MA1YYYY002', '孙悦', '13900002222', 'yuedong@demo.local',
 'M100002', '{"businessScope":"service","plan":"premium"}', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
('KYB-20260505-003', '快送同城物流', 'REJECTED', 'CASHIER_PUBLIC', '91310000MA1ZZZZ003', '周快', '13900003333', 'zhoukuai@demo.local',
 NULL, '{"businessScope":"logistics"}', DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_SUB(NOW(), INTERVAL 12 DAY));

UPDATE admin_merchant_application SET reject_reason = '资质照片不清晰，请重新上传营业执照' WHERE application_no = 'KYB-20260505-003';

INSERT INTO admin_merchant_open_app (merchant_id, app_id, app_name, secret_current, status, created_at, updated_at) VALUES
('M100001', 'app_m100001_live', '星云零售-生产应用', 'sk_live_demo_m100001_xxxxxxxxxxxxxxxx', 'ACTIVE', NOW(), NOW()),
('M100002', 'app_m100002_live', '蓝海餐饮-生产应用', 'sk_live_demo_m100002_xxxxxxxxxxxxxxxx', 'ACTIVE', NOW(), NOW());

INSERT INTO admin_sys_roles (id, role_code, role_name, description, status, created_at, updated_at) VALUES
(1, 'SUPER_ADMIN', '超级管理员', '全量菜单', 'ACTIVE', NOW(), NOW()),
(2, 'ADMIN', '管理员', '业务管理', 'ACTIVE', NOW(), NOW()),
(3, 'FINANCE', '财务', '订单与退款', 'ACTIVE', NOW(), NOW()),
(4, 'RISK', '风控', '风控与安全', 'ACTIVE', NOW(), NOW());

INSERT INTO admin_sys_menus (
  id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, created_at, updated_at
) VALUES
-- 1 工作台
(1, NULL, 'grp_workspace', '工作台', 'MENU', NULL, NULL, 1, 1, 'ACTIVE', NOW(), NOW()),
(2, 1, 'dashboard', '数据概览', 'MENU', '/admin/dashboard', NULL, 1, 1, 'ACTIVE', NOW(), NOW()),
(101, 1, 'insights_funnel', '支付漏斗', 'MENU', '/admin/insights/funnel', NULL, 2, 1, 'ACTIVE', NOW(), NOW()),
(104, 1, 'churn_alerts', '流失预警', 'MENU', '/admin/dashboard/churn-alerts', NULL, 3, 1, 'ACTIVE', NOW(), NOW()),
(3, 1, 'notifications', '通知中心', 'MENU', '/admin/notifications', NULL, 4, 1, 'ACTIVE', NOW(), NOW()),
(4, 1, 'search', '全局搜索', 'MENU', '/admin/search', NULL, 5, 1, 'ACTIVE', NOW(), NOW()),
-- 2 交易与订单
(10, NULL, 'grp_trade', '交易与订单', 'MENU', NULL, NULL, 2, 1, 'ACTIVE', NOW(), NOW()),
(11, 10, 'orders', '订单管理', 'MENU', '/admin/orders', NULL, 1, 1, 'ACTIVE', NOW(), NOW()),
(12, 10, 'refunds', '退款管理', 'MENU', '/admin/refunds', NULL, 2, 1, 'ACTIVE', NOW(), NOW()),
(13, 10, 'merchant_notifies', '回调记录', 'MENU', '/admin/merchant-notifies', NULL, 3, 1, 'ACTIVE', NOW(), NOW()),
-- 3 对账管理
(60, NULL, 'grp_reconcile', '对账管理', 'MENU', NULL, NULL, 3, 1, 'ACTIVE', NOW(), NOW()),
(310, 60, 'reconcile_work_items', '差异工单', 'MENU', '/admin/reconcile/work-items', NULL, 1, 1, 'ACTIVE', NOW(), NOW()),
(61, 60, 'reconcile_tasks', '对账任务', 'MENU', '/admin/reconcile/tasks', NULL, 2, 1, 'ACTIVE', NOW(), NOW()),
(62, 60, 'reconcile_results', '对账结果', 'MENU', '/admin/reconcile/results', NULL, 3, 1, 'ACTIVE', NOW(), NOW()),
(311, 60, 'reconcile_insights_dashboard', '差异归因看板', 'MENU', '/admin/reconcile/insights-dashboard', NULL, 4, 1, 'ACTIVE', NOW(), NOW()),
(312, 60, 'reconcile_sla_rules', 'SLA 规则', 'MENU', '/admin/reconcile/sla-rules', NULL, 5, 1, 'ACTIVE', NOW(), NOW()),
(313, 60, 'reconcile_long_tail', '长尾差异', 'MENU', '/admin/reconcile/long-tail', NULL, 6, 1, 'ACTIVE', NOW(), NOW()),
(63, 60, 'reconcile_summary', '对账汇总', 'MENU', '/admin/reconcile/summary', NULL, 7, 1, 'ACTIVE', NOW(), NOW()),
-- 4 渠道与账户
(20, NULL, 'grp_channel_account', '渠道与账户', 'MENU', NULL, NULL, 4, 1, 'ACTIVE', NOW(), NOW()),
(21, 20, 'channels', '渠道管理', 'MENU', '/admin/channels', NULL, 1, 1, 'ACTIVE', NOW(), NOW()),
(24, 20, 'payment_methods', '支付方式', 'MENU', '/admin/payment-methods', NULL, 2, 1, 'ACTIVE', NOW(), NOW()),
(25, 20, 'payment_accounts', '支付账号', 'MENU', '/admin/payment-accounts', NULL, 3, 1, 'ACTIVE', NOW(), NOW()),
-- 商户支付配置已并入「商户管理」→ 行内「支付配置」弹窗，不再单独挂菜单（原 id=23）
-- 5 路由与费率
(68, NULL, 'grp_routing', '路由与费率', 'MENU', NULL, NULL, 5, 1, 'ACTIVE', NOW(), NOW()),
-- 支付路由已并入「商户管理」→ 行内「支付配置」，不再单独挂菜单（原 id=22）
(103, 68, 'channel_routing_health', '路由健康度', 'MENU', '/admin/channel-routing/health', NULL, 2, 1, 'ACTIVE', NOW(), NOW()),
(67, 68, 'routing_logs', '路由决策日志', 'MENU', '/admin/routing/logs', NULL, 3, 1, 'ACTIVE', NOW(), NOW()),
(65, 68, 'fee_rate_config', '阶梯费率配置', 'MENU', '/admin/fee-rate/config', NULL, 4, 1, 'ACTIVE', NOW(), NOW()),
(66, 68, 'fee_rate_audit_log', '费率变更审计', 'MENU', '/admin/fee-rate/audit-log', NULL, 5, 1, 'ACTIVE', NOW(), NOW()),
-- 6 商户与风控
(30, NULL, 'grp_merchant', '商户与风控', 'MENU', NULL, NULL, 6, 1, 'ACTIVE', NOW(), NOW()),
(31, 30, 'merchants', '商户管理', 'MENU', '/admin/merchants', NULL, 1, 1, 'ACTIVE', NOW(), NOW()),
(102, 30, 'onboarding', '商户进件', 'MENU', '/admin/onboarding', NULL, 2, 1, 'ACTIVE', NOW(), NOW()),
(32, 30, 'risk', '风控配置', 'MENU', '/admin/risk', NULL, 3, 1, 'ACTIVE', NOW(), NOW()),
-- 7 系统与设置（含原「权限运维」子项）
(40, NULL, 'grp_system', '系统与设置', 'MENU', NULL, NULL, 7, 1, 'ACTIVE', NOW(), NOW()),
(41, 40, 'settings', '系统设置', 'MENU', '/admin/settings', NULL, 1, 1, 'ACTIVE', NOW(), NOW()),
(53, 40, 'users', '用户管理', 'MENU', '/admin/users', NULL, 2, 1, 'ACTIVE', NOW(), NOW()),
(51, 40, 'roles', '角色管理', 'MENU', '/admin/roles', NULL, 3, 1, 'ACTIVE', NOW(), NOW()),
(52, 40, 'menus', '菜单管理', 'MENU', '/admin/menus', NULL, 4, 1, 'ACTIVE', NOW(), NOW()),
(42, 40, 'dicts', '数据字典', 'MENU', '/admin/dicts', NULL, 5, 1, 'ACTIVE', NOW(), NOW()),
(54, 40, 'audit_logs', '操作日志', 'MENU', '/admin/audit-logs', NULL, 6, 1, 'ACTIVE', NOW(), NOW()),
(64, 40, 'security_audit', '安全审计', 'MENU', '/admin/security-audit', NULL, 7, 1, 'ACTIVE', NOW(), NOW()),
(69, 40, 'data_isolation', '数据隔离治理', 'MENU', '/admin/data-isolation', NULL, 8, 1, 'ACTIVE', NOW(), NOW());

-- 按钮级权限（BUTTON）
INSERT INTO admin_sys_menus (id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, perm_code, api_pattern, created_at, updated_at) VALUES
(201, 11, 'btn_order_export', '导出订单', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'order:export', 'GET:/api/v1/admin/orders/export', NOW(), NOW()),
(202, 11, 'btn_order_close', '关单', 'BUTTON', NULL, NULL, 2, 1, 'ACTIVE', 'order:close', 'POST:/api/v1/admin/orders/*/close', NOW(), NOW()),
(203, 11, 'btn_refund_create', '申请退款', 'BUTTON', NULL, NULL, 3, 1, 'ACTIVE', 'refund:create', 'POST:/api/v1/admin/orders/*/refund-requests', NOW(), NOW()),
(204, 11, 'btn_order_payment_query', '查单并同步', 'BUTTON', NULL, NULL, 4, 1, 'ACTIVE', 'order:payment:query', 'POST:/api/v1/admin/orders/*/payments/*/query-channel', NOW(), NOW()),
(205, 12, 'btn_refund_approve', '审批通过', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'refund:approve', 'POST:/api/v1/admin/refunds/*/approve', NOW(), NOW()),
(206, 12, 'btn_refund_reject', '审批拒绝', 'BUTTON', NULL, NULL, 2, 1, 'ACTIVE', 'refund:reject', 'POST:/api/v1/admin/refunds/*/reject', NOW(), NOW()),
(211, 21, 'btn_channel_create', '新增渠道', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'channel:create', 'POST:/api/v1/admin/channels', NOW(), NOW()),
(212, 21, 'btn_channel_edit', '编辑渠道', 'BUTTON', NULL, NULL, 2, 1, 'ACTIVE', 'channel:edit', 'PUT:/api/v1/admin/channels/*', NOW(), NOW()),
(213, 21, 'btn_channel_delete', '删除渠道', 'BUTTON', NULL, NULL, 3, 1, 'ACTIVE', 'channel:delete', 'DELETE:/api/v1/admin/channels/*', NOW(), NOW()),
(214, 24, 'btn_payment_method_create', '新增支付方式', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'payment_method:create', 'POST:/api/v1/admin/payment-methods', NOW(), NOW()),
(215, 24, 'btn_payment_method_edit', '编辑支付方式', 'BUTTON', NULL, NULL, 2, 1, 'ACTIVE', 'payment_method:edit', 'PUT:/api/v1/admin/payment-methods/*', NOW(), NOW()),
(216, 24, 'btn_payment_method_delete', '删除支付方式', 'BUTTON', NULL, NULL, 3, 1, 'ACTIVE', 'payment_method:delete', 'DELETE:/api/v1/admin/payment-methods/*', NOW(), NOW()),
(217, 25, 'btn_payment_account_create', '新增支付账号', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'payment_account:create', 'POST:/api/v1/admin/payment-accounts', NOW(), NOW()),
(218, 25, 'btn_payment_account_edit', '编辑支付账号', 'BUTTON', NULL, NULL, 2, 1, 'ACTIVE', 'payment_account:edit', 'PUT:/api/v1/admin/payment-accounts/*', NOW(), NOW()),
(219, 25, 'btn_payment_account_delete', '删除支付账号', 'BUTTON', NULL, NULL, 3, 1, 'ACTIVE', 'payment_account:delete', 'DELETE:/api/v1/admin/payment-accounts/*', NOW(), NOW()),
(221, 31, 'btn_merchant_edit', '编辑商户', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'merchant:edit', 'PUT:/api/v1/admin/merchants/*', NOW(), NOW()),
(222, 31, 'btn_merchant_delete', '删除商户', 'BUTTON', NULL, NULL, 2, 1, 'ACTIVE', 'merchant:delete', 'DELETE:/api/v1/admin/merchants/*', NOW(), NOW()),
(223, 102, 'btn_onboarding_approve', '进件通过', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'onboarding:approve', 'POST:/api/v1/admin/onboarding/applications/*/approve', NOW(), NOW()),
(224, 102, 'btn_onboarding_reject', '进件拒绝', 'BUTTON', NULL, NULL, 2, 1, 'ACTIVE', 'onboarding:reject', 'POST:/api/v1/admin/onboarding/applications/*/reject', NOW(), NOW()),
(225, 32, 'btn_risk_rule_write', '风控规则维护', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'risk:rule:write', 'POST:/api/v1/admin/risk/rules', NOW(), NOW()),
(231, 61, 'btn_recon_manual_run', '手动对账', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'recon:manual_run', 'POST:/api/v1/admin/reconcile/tasks/manual-run', NOW(), NOW()),
(232, 62, 'btn_recon_diff_handle', '差异处理', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'recon:diff:handle', 'POST:/api/v1/admin/reconcile/diffs/*/handle', NOW(), NOW()),
(301, 62, 'btn_recon_work_item_assign', '工单指派/改派', 'BUTTON', NULL, NULL, 2, 1, 'ACTIVE', 'recon:diff:assign', 'POST:/api/v1/admin/reconcile/diffs/*/assign', NOW(), NOW()),
(302, 62, 'btn_recon_work_item_claim', '工单认领', 'BUTTON', NULL, NULL, 3, 1, 'ACTIVE', 'recon:diff:assign', 'POST:/api/v1/admin/reconcile/diffs/*/claim', NOW(), NOW()),
(303, 62, 'btn_recon_work_item_start', '开始处理', 'BUTTON', NULL, NULL, 4, 1, 'ACTIVE', 'recon:diff:handle', 'POST:/api/v1/admin/reconcile/diffs/*/start', NOW(), NOW()),
(304, 62, 'btn_recon_work_item_complete', '工单处置完成', 'BUTTON', NULL, NULL, 5, 1, 'ACTIVE', 'recon:diff:handle', 'POST:/api/v1/admin/reconcile/diffs/*/complete', NOW(), NOW()),
(305, 62, 'btn_recon_work_item_comment', '工单留言', 'BUTTON', NULL, NULL, 6, 1, 'ACTIVE', 'recon:diff:handle', 'POST:/api/v1/admin/reconcile/diffs/*/comment', NOW(), NOW()),
(306, 62, 'btn_recon_sla_rule_write', 'SLA 规则维护', 'BUTTON', NULL, NULL, 7, 1, 'ACTIVE', 'recon:manage', 'PUT:/api/v1/admin/reconcile/sla-rules/*', NOW(), NOW()),
(307, 62, 'btn_recon_report_subscribe', '报告订阅', 'BUTTON', NULL, NULL, 8, 1, 'ACTIVE', 'recon:report:subscribe', 'POST:/api/v1/admin/reconcile/subscriptions', NOW(), NOW()),
(233, 65, 'btn_fee_rate_write', '费率配置维护', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'fee_rate:write', 'POST:/api/v1/admin/fee-rate', NOW(), NOW()),
(241, 41, 'btn_system_config_write', '系统配置维护', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'system_config:write', 'POST:/api/v1/admin/system-configs', NOW(), NOW()),
(251, 51, 'btn_role_create', '新增角色', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'role:create', 'POST:/api/v1/admin/roles', NOW(), NOW()),
(252, 51, 'btn_role_edit', '编辑角色', 'BUTTON', NULL, NULL, 2, 1, 'ACTIVE', 'role:edit', 'PUT:/api/v1/admin/roles/*', NOW(), NOW()),
(253, 51, 'btn_role_delete', '删除角色', 'BUTTON', NULL, NULL, 3, 1, 'ACTIVE', 'role:delete', 'DELETE:/api/v1/admin/roles/*', NOW(), NOW()),
(254, 51, 'btn_role_assign_menu', '分配权限', 'BUTTON', NULL, NULL, 4, 1, 'ACTIVE', 'role:assign_menu', 'POST:/api/v1/admin/roles/*/menus', NOW(), NOW()),
(255, 52, 'btn_menu_create', '新增菜单', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'menu:create', 'POST:/api/v1/admin/menus', NOW(), NOW()),
(256, 52, 'btn_menu_edit', '编辑菜单', 'BUTTON', NULL, NULL, 2, 1, 'ACTIVE', 'menu:edit', 'PUT:/api/v1/admin/menus/*', NOW(), NOW()),
(257, 52, 'btn_menu_delete', '删除菜单', 'BUTTON', NULL, NULL, 3, 1, 'ACTIVE', 'menu:delete', 'DELETE:/api/v1/admin/menus/*', NOW(), NOW()),
(258, 53, 'btn_user_create', '新增用户', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'user:create', 'POST:/api/v1/admin/users', NOW(), NOW()),
(259, 53, 'btn_user_edit', '编辑用户', 'BUTTON', NULL, NULL, 2, 1, 'ACTIVE', 'user:edit', 'PUT:/api/v1/admin/users/*', NOW(), NOW()),
(260, 53, 'btn_user_reset_password', '重置密码', 'BUTTON', NULL, NULL, 3, 1, 'ACTIVE', 'user:reset_password', 'PUT:/api/v1/admin/users/*/reset-password', NOW(), NOW()),
(261, 69, 'btn_data_isolation_remediate', '隔离治理修复', 'BUTTON', NULL, NULL, 1, 1, 'ACTIVE', 'data_isolation:remediate', 'PUT:/api/v1/admin/data-isolation/checks/*/remediation', NOW(), NOW());

INSERT INTO admin_sys_role_menus (role_id, menu_id, created_at) SELECT 1, id, NOW() FROM admin_sys_menus;
INSERT INTO admin_sys_role_menus (role_id, menu_id, created_at) SELECT 2, id, NOW() FROM admin_sys_menus WHERE id NOT IN (51, 52, 53, 54, 64);
INSERT INTO admin_sys_role_menus (role_id, menu_id, created_at) SELECT 3, id, NOW() FROM admin_sys_menus WHERE id IN (1, 2, 3, 4, 101, 104, 10, 11, 12, 13, 60, 61, 62, 63, 310, 311, 313, 232, 301, 302, 303, 304, 305, 307);
INSERT INTO admin_sys_role_menus (role_id, menu_id, created_at) SELECT 4, id, NOW() FROM admin_sys_menus WHERE id IN (1, 2, 10, 11, 30, 31, 32, 64, 68, 103, 67, 65, 66);

INSERT INTO admin_sys_users (id, username, password, nickname, phone, email, status, created_at, updated_at) VALUES
(1, 'sys_admin', @pwd, '系统管理员', '13800001111', 'sys_admin@demo.local', 'ACTIVE', NOW(), NOW()),
(2, 'sys_operator', @pwd, '运营小王', '13800002222', 'ops@demo.local', 'ACTIVE', NOW(), NOW()),
(3, 'sys_auditor', @pwd, '审计（禁用演示）', '13800003333', 'audit@demo.local', 'DISABLED', NOW(), NOW());

INSERT INTO admin_sys_user_roles (user_id, role_id, created_at) VALUES (1, 1, NOW()), (2, 2, NOW()), (3, 4, NOW()), (4, 2, NOW()), (5, 2, NOW());

ALTER TABLE admin_channels AUTO_INCREMENT = 100;
ALTER TABLE admin_merchants AUTO_INCREMENT = 100;
ALTER TABLE admin_payment_accounts AUTO_INCREMENT = 100;
ALTER TABLE admin_payment_methods AUTO_INCREMENT = 100;
ALTER TABLE admin_merchant_payment_methods AUTO_INCREMENT = 100;
ALTER TABLE admin_merchant_payment_routes AUTO_INCREMENT = 100;
ALTER TABLE admin_channel_routes AUTO_INCREMENT = 100;
ALTER TABLE admin_risk_rules AUTO_INCREMENT = 100;
ALTER TABLE admin_system_configs AUTO_INCREMENT = 100;
-- 站内通知 demo 数据
INSERT INTO admin_notifications (recipient_user_id, merchant_id, biz_type, biz_key, title, summary, link, read_status, created_at) VALUES
(1, 'M100001', 'REFUND_APPROVAL', 'REF-20260524-001', '退款 REF-20260524-001 等待审批', '商户 M100001 发起退款 ¥158.00，请尽快处理', '/admin/refunds?status=REFUNDING', 0, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(1, 'M100001', 'REFUND_APPROVAL', 'REF-20260524-002', '退款 REF-20260524-002 等待审批', '商户 M100001 发起退款 ¥320.50', '/admin/refunds?status=REFUNDING', 0, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(2, 'M100001', 'REFUND_APPROVAL', 'REF-20260524-001', '退款 REF-20260524-001 等待审批', '商户 M100001 发起退款 ¥158.00，请尽快处理', '/admin/refunds?status=REFUNDING', 0, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(1, 'M100001', 'CHURN_OVERDUE', 'CHURN-1001', '流失预警超时：M100001 超48小时未跟进', '商户 M100001 的流失预警已超过 48 小时未处理', '/admin/churn-alerts?status=pending', 0, DATE_SUB(NOW(), INTERVAL 50 HOUR)),
(2, 'M100001', 'CHURN_OVERDUE', 'CHURN-1001', '流失预警超时：M100001 超48小时未跟进', '商户 M100001 的流失预警已超过 48 小时未处理', '/admin/churn-alerts?status=pending', 1, DATE_SUB(NOW(), INTERVAL 50 HOUR)),
(1, NULL, 'EXPORT_COMPLETED', 'EXPORT-20260525-001', '导出任务完成', '交易报表导出已完成，点击下载', '/admin/export/download/EXPORT-20260525-001', 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1, NULL, 'EXPORT_FAILED', 'EXPORT-20260525-002', '导出任务失败', '商户 M100002 的对账报表导出失败：数据量超限', '/admin/export', 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(1, 'M100001', 'RECON_DIFF', 'RECON-TASK-20260524', '对账发现 5 笔差异', '对账任务 RECON-TASK-20260524 完成，发现 5 笔差异需处理', '/admin/reconcile/tasks/RECON-TASK-20260524/diffs', 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(2, 'M100001', 'RECON_DIFF', 'RECON-TASK-20260524', '对账发现 5 笔差异', '对账任务 RECON-TASK-20260524 完成，发现 5 笔差异需处理', '/admin/reconcile/tasks/RECON-TASK-20260524/diffs', 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1, 'M100002', 'WEBHOOK_FAILURE', 'WEBHOOK-M100002-EP1', '商户 M100002 回调连续失败', 'webhook 端点连续失败 5 次，请检查商户配置', '/admin/merchant-notifies?merchantId=M100002', 0, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(1, 'M100001', 'REFUND_APPROVAL', 'REF-20260523-003', '退款 REF-20260523-003 等待审批', '商户 M100001 发起退款 ¥88.00', '/admin/refunds?status=REFUNDING', 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(1, NULL, 'EXPORT_COMPLETED', 'EXPORT-20260523-003', '导出任务完成', '月度汇总报表导出完成', '/admin/export/download/EXPORT-20260523-003', 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(2, 'M100002', 'WEBHOOK_FAILURE', 'WEBHOOK-M100002-EP1', '商户 M100002 回调连续失败', 'webhook 端点连续失败 5 次', '/admin/merchant-notifies?merchantId=M100002', 0, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(1, 'M100001', 'CHURN_OVERDUE', 'CHURN-1002', '流失预警超时：M100001 连续下降', '商户交易量连续 5 天下降，预警已超时', '/admin/churn-alerts?status=pending', 0, DATE_SUB(NOW(), INTERVAL 72 HOUR)),
(2, 'M100001', 'REFUND_APPROVAL', 'REF-20260524-002', '退款 REF-20260524-002 等待审批', '商户 M100001 发起退款 ¥320.50', '/admin/refunds?status=REFUNDING', 0, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(1, 'M100002', 'RECON_DIFF', 'RECON-TASK-20260523', '对账发现 3 笔差异', '对账任务完成，发现 3 笔差异', '/admin/reconcile/tasks/RECON-TASK-20260523/diffs', 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(1, NULL, 'EXPORT_COMPLETED', 'EXPORT-20260522-001', '导出任务完成', '渠道分析报表已就绪', '/admin/export/download/EXPORT-20260522-001', 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2, NULL, 'EXPORT_COMPLETED', 'EXPORT-20260522-001', '导出任务完成', '渠道分析报表已就绪', '/admin/export/download/EXPORT-20260522-001', 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(1, 'M100001', 'REFUND_APPROVAL', 'REF-20260522-004', '退款 REF-20260522-004 等待审批', '商户 M100001 发起退款 ¥42.00', '/admin/refunds?status=REFUNDING', 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(1, 'M100002', 'WEBHOOK_FAILURE', 'WEBHOOK-M100002-EP2', '商户 M100002 第二端点回调失败', '备用 webhook 端点也连续失败', '/admin/merchant-notifies?merchantId=M100002', 0, DATE_SUB(NOW(), INTERVAL 12 HOUR));

ALTER TABLE admin_notifications AUTO_INCREMENT = 100;

ALTER TABLE admin_users AUTO_INCREMENT = 100;
ALTER TABLE admin_sys_roles AUTO_INCREMENT = 100;
ALTER TABLE admin_sys_menus AUTO_INCREMENT = 300;
ALTER TABLE admin_sys_users AUTO_INCREMENT = 100;
