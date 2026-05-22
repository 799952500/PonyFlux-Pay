-- =============================================================================
-- PonyFlux Pay — 收银台库 payflow_cashier 演示数据
-- 覆盖：订单/支付/退款/渠道/安全审计；日期贴近「今天」便于仪表盘
-- =============================================================================
USE payflow_cashier;
SET NAMES utf8mb4;

INSERT INTO cashier_merchants (
  merchant_id, merchant_name, password, app_secret, status,
  allowed_channels, allowed_pay_methods, contact, phone, email, description,
  created_at, updated_at
) VALUES
('M100001', '星云零售旗舰店', NULL, 'demo_app_secret_m100001_32chars_hex01', 'ACTIVE',
 'WECHAT_PAY,ALIPAY,UNION_PAY', 'WECHAT_APP,WECHAT_H5,ALIPAY_APP,ALIPAY_WAP,UNION_QR',
 '张星云', '13800138001', 'ops@m100001.demo', '主营 3C 数码，日单量较高',
 NOW(), NOW()),
('M100002', '蓝海餐饮连锁', NULL, 'demo_app_secret_m100002_32chars_hex02', 'ACTIVE',
 'WECHAT_PAY,ALIPAY', 'WECHAT_H5,ALIPAY_WAP',
 '李蓝海', '13800138002', 'finance@m100002.demo', '门店扫码点餐',
 NOW(), NOW()),
('M100003', '青禾教育培训', NULL, 'demo_app_secret_m100003_32chars_hex03', 'ACTIVE',
 'WECHAT_PAY', 'WECHAT_APP,WECHAT_H5',
 '王青禾', '13800138003', 'admin@m100003.demo', '课程报名在线支付',
 DATE_SUB(NOW(), INTERVAL 30 DAY), NOW());

INSERT INTO cashier_channels (
  channel_code, channel_name, icon_url, fee_rate, status, sort_weight, description, created_at, updated_at
) VALUES
('WECHAT_PAY', '微信支付', '/icons/wechat.svg', 0.0060, 'ENABLED', 100, '微信', NOW(), NOW()),
('ALIPAY', '支付宝', '/icons/alipay.svg', 0.0055, 'ENABLED', 90, '支付宝', NOW(), NOW()),
('UNION_PAY', '银联云闪付', '/icons/union.svg', 0.0050, 'ENABLED', 80, '银联', NOW(), NOW());

INSERT INTO cashier_channel_accounts (
  channel_id, account_code, account_name, channel_config, status, remark, created_at, updated_at
) VALUES
(1, 'ACC-WX-001', '微信华东主账户', '{"env":"sandbox","region":"east"}', 'ENABLED', '演示', NOW(), NOW()),
(2, 'ACC-ALI-001', '支付宝华东主账户', '{"env":"sandbox"}', 'ENABLED', '演示', NOW(), NOW()),
(3, 'ACC-UNION-001', '银联华东主账户', '{"env":"sandbox"}', 'ENABLED', '演示', NOW(), NOW());

INSERT INTO cashier_channel_merchant_routes (
  channel_account_id, merchant_id, enabled, priority, created_at, updated_at
) VALUES
(1, 'M100001', 1, 100, NOW(), NOW()),
(2, 'M100001', 1, 90, NOW(), NOW()),
(3, 'M100001', 1, 80, NOW(), NOW()),
(1, 'M100002', 1, 100, NOW(), NOW()),
(2, 'M100002', 1, 90, NOW(), NOW()),
(1, 'M100003', 1, 100, NOW(), NOW());

INSERT INTO cashier_orders (
  order_id, merchant_id, merchant_order_no, amount, currency, pay_amount,
  subject, body, channel, status,
  notify_url, merchant_notify_url, expire_time, pay_time, created_at, updated_at, notify_status, notify_retry_count
) VALUES
('ORD-20260518-0001', 'M100001', 'MPO-240518-001', 29900, 'CNY', 29900,
 'iPhone 保护壳套装', 'SKU-3C-001', 'WECHAT_PAY', 'PAID',
 'https://demo.payflow.local/notify', 'https://m100001.demo/callback',
 DATE_ADD(NOW(), INTERVAL 2 HOUR), NOW(), DATE_SUB(NOW(), INTERVAL 2 HOUR), NOW(), 'SUCCESS', 1),
('ORD-20260518-0002', 'M100001', 'MPO-240518-002', 15800, 'CNY', NULL,
 '无线充电器', 'SKU-3C-002', 'ALIPAY', 'CREATED',
 'https://demo.payflow.local/notify', NULL,
 DATE_ADD(NOW(), INTERVAL 30 MINUTE), NULL, DATE_SUB(NOW(), INTERVAL 1 HOUR), NOW(), 'PENDING', 0),
('ORD-20260518-0003', 'M100002', 'MPO-240518-003', 8800, 'CNY', 8800,
 '双人套餐', 'MEAL-A2', 'ALIPAY', 'PAID',
 NULL, 'https://m100002.demo/callback',
 DATE_ADD(NOW(), INTERVAL 1 HOUR), NOW(), DATE_SUB(NOW(), INTERVAL 3 HOUR), NOW(), 'SUCCESS', 2),
('ORD-20260517-0004', 'M100002', 'MPO-240517-004', 4200, 'CNY', NULL,
 '下午茶券', 'MEAL-T4', 'WECHAT_PAY', 'EXPIRED',
 NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL,
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 'PENDING', 0),
('ORD-20260517-0005', 'M100001', 'MPO-240517-005', 128800, 'CNY', 128800,
 '笔记本电脑', 'SKU-3C-099', 'UNION_PAY', 'PAID',
 NULL, NULL, DATE_ADD(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 1 DAY),
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 'SUCCESS', 1),
('ORD-20260516-0006', 'M100003', 'MPO-240516-006', 19900, 'CNY', 19900,
 'Python 进阶班', 'COURSE-PY-02', 'WECHAT_PAY', 'PAID',
 NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY),
 DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 'SUCCESS', 1),
('ORD-20260516-0007', 'M100001', 'MPO-240516-007', 6600, 'CNY', NULL,
 '数据线', 'SKU-3C-010', 'WECHAT_PAY', 'PAYING',
 NULL, NULL, DATE_ADD(NOW(), INTERVAL 15 MINUTE), NULL, NOW(), NOW(), 'PENDING', 0),
('ORD-20260515-0008', 'M100002', 'MPO-240515-008', 25600, 'CNY', 25600,
 '团建宴席', 'MEAL-B8', 'ALIPAY', 'PAID',
 NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 3 DAY),
 DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), 'SUCCESS', 1),
('ORD-20260514-0009', 'M100001', 'MPO-240514-009', 990, 'CNY', NULL,
 '手机贴膜', 'SKU-3C-011', 'WECHAT_PAY', 'FAILED',
 NULL, NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), 'PENDING', 0),
('ORD-20260513-0010', 'M100003', 'MPO-240513-010', 49900, 'CNY', 49900,
 '架构师训练营', 'COURSE-ARCH-01', 'WECHAT_PAY', 'PAID',
 NULL, NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY),
 DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY), 'SUCCESS', 1);

INSERT INTO cashier_payments (
  payment_id, order_id, pay_channel, account_code, pay_method, channel_transaction_id, amount, status, created_at, updated_at
) VALUES
('PAY-20260518-001', 'ORD-20260518-0001', 'WECHAT_PAY', 'CASHIER_WX_001', 'WECHAT_APP', '4200001234567890', 29900, 'SUCCESS', DATE_SUB(NOW(), INTERVAL 2 HOUR), NOW()),
('PAY-20260518-002', 'ORD-20260518-0003', 'ALIPAY', 'CASHIER_ALI_001', 'ALIPAY_WAP', '2026051822001234567', 8800, 'SUCCESS', DATE_SUB(NOW(), INTERVAL 3 HOUR), NOW()),
('PAY-20260517-001', 'ORD-20260517-0005', 'UNION_PAY', 'CASHIER_UNION_001', 'UNION_QR', '6226098877665544', 128800, 'SUCCESS', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
('PAY-20260516-001', 'ORD-20260516-0006', 'WECHAT_PAY', 'CASHIER_WX_001', 'WECHAT_H5', '4200009876543210', 19900, 'SUCCESS', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
('PAY-20260518-003', 'ORD-20260516-0007', 'WECHAT_PAY', 'CASHIER_WX_001', 'WECHAT_H5', NULL, 6600, 'PROCESSING', NOW(), NOW()),
('PAY-20260515-001', 'ORD-20260515-0008', 'ALIPAY', 'CASHIER_ALI_001', 'ALIPAY_APP', '2026051514309988776', 25600, 'SUCCESS', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
('PAY-20260513-001', 'ORD-20260513-0010', 'WECHAT_PAY', 'CASHIER_WX_001', 'WECHAT_APP', '4200005555666677', 49900, 'SUCCESS', DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY));

INSERT INTO cashier_refunds (
  refund_id, payment_id, order_id, pay_channel, refund_amount, reason, status,
  channel_refund_no, merchant_refund_no, created_at, updated_at
) VALUES
('REF-20260518-001', 'PAY-20260518-001', 'ORD-20260518-0001', 'WECHAT_PAY', 9900,
 '用户申请：商品瑕疵半价退款', 'REFUNDING', NULL, 'MRN-240518-001', NOW(), NOW()),
('REF-20260518-002', 'PAY-20260517-001', 'ORD-20260517-0005', 'UNION_PAY', 28800,
 '用户误购，申请部分退款', 'REFUNDING', NULL, 'MRN-240518-002', NOW(), NOW()),
('REF-20260517-001', 'PAY-20260518-002', 'ORD-20260518-0003', 'ALIPAY', 4400,
 '重复扣款', 'REFUNDED', '2026051722009988', 'MRN-240517-001',
 DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
('REF-20260510-001', 'PAY-20260518-001', 'ORD-20260518-0001', 'WECHAT_PAY', 29900,
 '全额退款失败（渠道拒绝）', 'FAILED', NULL, 'MRN-240510-001',
 DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY)),
('REF-20260512-001', 'PAY-20260515-001', 'ORD-20260515-0008', 'ALIPAY', 5600,
 '部分菜品退单', 'REFUNDED', '2026051222007766', 'MRN-240512-001',
 DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY)),
('REF-20260513-001', 'PAY-20260513-001', 'ORD-20260513-0010', 'WECHAT_PAY', 9900,
 '课程改签差价退回', 'REFUNDED', '2026051322001133', 'MRN-240513-001',
 DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY));

INSERT INTO cashier_security_audit (
  merchant_id, target_merchant_id, auth_mode, http_method, request_path,
  resource_type, resource_id, client_ip, outcome, reason_code, reason_detail, created_at
) VALUES
('M100002', 'M100001', 'JWT', 'GET', '/api/v1/cashier/orders/ORD-20260518-0001',
 'ORDER', 'ORD-20260518-0001', '10.12.0.15', 'DENIED', '5102', '资源不属于当前商户', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
('M100001', 'M100003', 'HMAC', 'POST', '/api/v1/cashier/payments',
 'ORDER', NULL, '203.0.113.8', 'DENIED', '5101', 'merchantId 与认证主体不一致', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
('M100003', NULL, 'JWT', 'GET', '/api/v1/cashier/orders/ORD-20260516-0006',
 'ORDER', 'ORD-20260516-0006', '192.168.1.100', 'DENIED', '5102', '资源不属于当前商户', DATE_SUB(NOW(), INTERVAL 1 DAY)),
('M100001', 'M100001', 'HMAC', 'GET', '/api/v1/cashier/orders/not-exist',
 'ORDER', 'ORD-NO-SUCH', '10.12.0.20', 'DENIED', '5103', '资源不存在', DATE_SUB(NOW(), INTERVAL 2 DAY)),
('M100002', 'M100001', 'JWT', 'POST', '/api/v1/cashier/refunds',
 'REFUND', NULL, '10.12.0.22', 'DENIED', '5101', 'merchantId 与认证主体不一致', DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
('M100001', 'M100002', 'JWT', 'GET', '/api/v1/cashier/refunds/REF-20260517-001',
 'REFUND', 'REF-20260517-001', '10.12.0.23', 'DENIED', '5102', '资源不属于当前商户', DATE_SUB(NOW(), INTERVAL 25 MINUTE)),
('M100002', 'M100001', 'HMAC', 'POST', '/api/v1/cashier/payments',
 'PAYMENT', 'PAY-20260518-001', '203.0.113.9', 'DENIED', '5102', '支付记录不属于当前商户', DATE_SUB(NOW(), INTERVAL 15 MINUTE));
