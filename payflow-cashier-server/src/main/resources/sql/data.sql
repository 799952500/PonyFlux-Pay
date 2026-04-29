-- ================================================
-- PonyFlux-Pay 收银台测试数据 (MySQL 8)
-- ================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 商户数据
-- ----------------------------
INSERT INTO `cashier_merchants` (`merchant_id`, `merchant_name`, `password`, `app_secret`, `status`, `allowed_channels`, `allowed_pay_methods`, `contact`, `phone`, `email`, `description`, `created_at`, `updated_at`) VALUES
('M2024040001', 'XX科技旗舰店', NULL, '4f3c2b1a0e9d8c7b6a5f4e3d2c1b0a9f', 'ACTIVE', 'WECHAT_PAY,ALIPAY,UNION_PAY', 'WECHAT_APP,WECHAT_H5,ALIPAY_APP,ALIPAY_WAP', '张经理', '13800138001', 'xxtech@example.com', 'XX科技有限公司官方旗舰店', NOW(), NOW()),
('M10002', '测试商户2', NULL, 'abcdef1234567890fedcba0987654321', 'ACTIVE', 'WECHAT_PAY,ALIPAY', 'WECHAT_H5,ALIPAY_WAP', '李测试', '13900139002', 'test2@example.com', '测试商户', NOW(), NOW());

-- ----------------------------
-- 支付渠道数据
-- ----------------------------
INSERT INTO `cashier_channels` (`channel_code`, `channel_name`, `icon_url`, `status`, `sort_weight`, `description`, `created_at`, `updated_at`) VALUES
('wechat_pay', '微信支付', '/icons/wechat.svg', 'ENABLED', 100, '腾讯微信支付', NOW(), NOW()),
('alipay', '支付宝', '/icons/alipay.svg', 'ENABLED', 90, '阿里巴巴支付宝', NOW(), NOW()),
('union_pay', '银联支付', '/icons/union.svg', 'ENABLED', 80, '中国银联', NOW(), NOW());

-- ----------------------------
-- 渠道账户数据
-- ----------------------------
INSERT INTO `cashier_channel_accounts` (`channel_id`, `account_code`, `account_name`, `channel_config`, `status`, `remark`, `created_at`, `updated_at`) VALUES
(1, 'WECHAT_ACC_001', '微信支付主账户', '{"appId":"wx1234567888aaa","mchId":"1234567890","apiKey":"wechat_key_demo_xxxxxxx","certPath":"/cert/wechat/apiclient_cert.p12","certPassword":"xxxxxx"}', 'ENABLED', '生产环境主账户', NOW(), NOW()),
(2, 'ALIPAY_ACC_001', '支付宝主账户', '{"appId":"2021001122334455","privateKey":"MIIEvQIBADAN...","alipayPublicKey":"MIIBIjANBgk..."}', 'ENABLED', '生产环境主账户', NOW(), NOW()),
(3, 'UNION_ACC_001', '银联账户', '{"merId":"898340183928374","signCertPath":"/cert/union.pfx","signCertPassword":"xxxxxx"}', 'ENABLED', '生产环境主账户', NOW(), NOW());

-- ----------------------------
-- 商户渠道路由数据
-- ----------------------------
INSERT INTO `cashier_channel_merchant_routes` (`channel_account_id`, `merchant_id`, `enabled`, `priority`, `created_at`, `updated_at`) VALUES
(1, 'M2024040001', 1, 100, NOW(), NOW()),
(2, 'M2024040001', 1, 90, NOW(), NOW()),
(3, 'M2024040001', 1, 80, NOW(), NOW()),
(1, 'M10002', 1, 100, NOW(), NOW()),
(2, 'M10002', 1, 90, NOW(), NOW());

-- ----------------------------
-- 订单数据
-- ----------------------------
INSERT INTO `cashier_orders` (`order_id`, `merchant_id`, `merchant_order_no`, `amount`, `currency`, `pay_amount`, `subject`, `body`, `attach`, `channel`, `status`, `notify_url`, `merchant_notify_url`, `return_url`, `success_url`, `fail_url`, `expire_time`, `pay_time`, `created_at`, `updated_at`, `notify_status`, `notify_retry_count`) VALUES
('ORD20260420001', 'M2024040001', 'M2024040001_ORG001', 10000, 'CNY', NULL, 'iPhone 16 Pro 256GB', '苹果旗舰手机', NULL, 'WECHAT_PAY', 'CREATED', 'https://example.com/notify', 'https://example.com/merchant/callback', 'https://example.com/return', 'https://example.com/success', 'https://example.com/fail', DATE_ADD(NOW(), INTERVAL 30 MINUTE), NULL, NOW(), NOW(), 'PENDING', 0),
('ORD20260420002', 'M2024040001', 'M2024040001_ORG002', 5000, 'CNY', 5000, 'AirPods Pro 2', '苹果蓝牙耳机', NULL, 'ALIPAY', 'PAID', 'https://example.com/notify', 'https://example.com/merchant/callback', 'https://example.com/return', 'https://example.com/success', 'https://example.com/fail', DATE_ADD(NOW(), INTERVAL 30 MINUTE), NOW(), NOW(), NOW(), 'SUCCESS', 1),
('ORD20260420003', 'M2024040001', 'M2024040001_ORG003', 20000, 'CNY', NULL, 'MacBook Air M3', '苹果笔记本', NULL, 'WECHAT_PAY', 'EXPIRED', 'https://example.com/notify', 'https://example.com/merchant/callback', 'https://example.com/return', 'https://example.com/success', 'https://example.com/fail', DATE_SUB(NOW(), INTERVAL 1 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR), 'PENDING', 0),
('ORD20260420004', 'M10002', 'M10002_ORG001', 3000, 'CNY', 3000, '测试商品A', '测试商品描述', NULL, 'WECHAT_PAY', 'PAID', 'https://test.com/notify', 'https://test.com/callback', NULL, 'https://test.com/success', 'https://test.com/fail', DATE_ADD(NOW(), INTERVAL 30 MINUTE), NOW(), NOW(), NOW(), 'SUCCESS', 2),
('ORD20260420005', 'M10002', 'M10002_ORG002', 8000, 'CNY', NULL, '测试商品B', '测试商品描述B', NULL, 'ALIPAY', 'PAYING', 'https://test.com/notify', 'https://test.com/callback', NULL, 'https://test.com/success', 'https://test.com/fail', DATE_ADD(NOW(), INTERVAL 30 MINUTE), NULL, NOW(), NOW(), 'PENDING', 0);

-- ----------------------------
-- 支付记录数据
-- ----------------------------
INSERT INTO `cashier_payments` (`payment_id`, `order_id`, `pay_channel`, `pay_method`, `channel_transaction_id`, `amount`, `status`, `created_at`, `updated_at`) VALUES
('PAY20260420002A', 'ORD20260420002', 'ALIPAY', 'ALIPAY_WAP', '20260420220014123456789012', 5000, 'SUCCESS', NOW(), NOW()),
('PAY20260420003A', 'ORD20260420003', 'WECHAT_PAY', 'WECHAT_H5', NULL, 20000, 'FAILED', DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR)),
('PAY20260420004A', 'ORD20260420004', 'WECHAT_PAY', 'WECHAT_APP', 'WX20260420123456789012345', 3000, 'SUCCESS', NOW(), NOW()),
('PAY20260420005A', 'ORD20260420005', 'ALIPAY', 'ALIPAY_APP', '20260420220014987654321098', 8000, 'PROCESSING', NOW(), NOW());

-- ----------------------------
-- 风控规则数据
-- ----------------------------
INSERT INTO `cashier_risk_rules` (`rule_code`, `rule_name`, `rule_type`, `threshold`, `unit`, `action`, `enabled`, `description`, `created_at`, `updated_at`) VALUES
('MAX_AMOUNT_SINGLE', '单笔最大金额', 'AMOUNT_SINGLE', 50000.00, '元', 'REJECT', 1, '单笔超过阈值直接拒绝', NOW(), NOW()),
('MAX_AMOUNT_DAILY', '单日累计金额', 'AMOUNT_DAILY', 200000.00, '元', 'REJECT', 1, '当日累计超过阈值直接拒绝', NOW(), NOW()),
('HIGH_RISK_IP', '高风险IP拦截', 'IP_LIMIT', NULL, NULL, 'REVIEW', 0, '命中高风险IP进入人工审核', NOW(), NOW());
