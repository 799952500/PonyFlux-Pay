-- ================================================
-- PonyFlux-Pay 运营后台测试数据 (MySQL 8)
-- BCrypt 密码 admin123 → $2a$10$N.zmdr9k7uOCQb376CWq7uI6EBDlO1R2gRiQDsvbfmS5W3j3M5a0q
-- ================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 管理员账号
-- ----------------------------
INSERT INTO `admin_users` (`username`, `password`, `role`, `nickname`, `status`, `created_at`, `updated_at`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376CWq7uI6EBDlO1R2gRiQDsvbfmS5W3j3M5a0q', 'SUPER_ADMIN', '超级管理员', 'ACTIVE', NOW(), NOW()),
('finance01', '$2a$10$N.zmdr9k7uOCQb376CWq7uI6EBDlO1R2gRiQDsvbfmS5W3j3M5a0q', 'FINANCE', '财务小明', 'ACTIVE', NOW(), NOW()),
('risk01', '$2a$10$N.zmdr9k7uOCQb376CWq7uI6EBDlO1R2gRiQDsvbfmS5W3j3M5a0q', 'RISK', '风控小李', 'ACTIVE', NOW(), NOW());

-- ----------------------------
-- 支付渠道
-- ----------------------------
INSERT INTO `admin_channels` (`channel_code`, `channel_name`, `channel_type`, `api_url`, `api_key`, `enabled`, `priority`, `description`, `created_at`, `updated_at`) VALUES
('wechat_pay', '微信支付', 'WECHAT', 'https://api.mch.weixin.qq.com/pay/unifiedorder', 'wechat_api_key_demo_xxx', 1, 100, '腾讯微信支付', NOW(), NOW()),
('alipay', '支付宝', 'ALIPAY', 'https://openapi.alipay.com/gateway.do', 'alipay_api_key_demo_xxx', 1, 90, '阿里巴巴支付宝', NOW(), NOW()),
('union_pay', '银联支付', 'UNION', 'https://gateway.95516.com/gateway/api/frontTransReq.do', 'union_api_key_demo_xxx', 1, 80, '中国银联', NOW(), NOW()),
('bank_card', '银行卡', 'CARD', 'https://gateway.example.com', 'bank_api_key_demo_xxx', 1, 70, '银行卡快捷支付', NOW(), NOW());

-- ----------------------------
-- 商户
-- ----------------------------
INSERT INTO `admin_merchants` (`merchant_id`, `merchant_name`, `merchant_key`, `callback_url`, `notify_url`, `commission_rate`, `status`, `created_at`, `updated_at`) VALUES
('M2024040001', 'XX科技旗舰店', '4f3c2b1a0e9d8c7b6a5f4e3d2c1b0a9f', 'https://example.com/return', 'https://example.com/notify', 0.0060, 'ACTIVE', NOW(), NOW()),
('M10002', '测试商户2', 'abcdef1234567890fedcba0987654321', 'https://test.com/return', 'https://test.com/notify', 0.0050, 'ACTIVE', NOW(), NOW());

-- ----------------------------
-- 支付方式
-- ----------------------------
INSERT INTO `admin_payment_methods` (`method_code`, `method_name`, `channel_id`, `app_id`, `app_secret`, `mch_id`, `mch_key`, `config_json`, `enabled`, `priority`, `description`, `created_at`, `updated_at`) VALUES
('WECHAT_APP', '微信App支付', 1, 'wx1234567888aaa', 'wx_secret_xxxxxxx', '1234567890', 'wechat_mch_key_xxxxx', '{"tradeType":"APP"}', 1, 100, '微信APP支付', NOW(), NOW()),
('WECHAT_H5', '微信H5支付', 1, 'wx1234567888aaa', 'wx_secret_xxxxxxx', '1234567890', 'wechat_mch_key_xxxxx', '{"tradeType":"MWEB"}', 1, 90, '微信H5支付', NOW(), NOW()),
('WECHAT_NATIVE', '微信扫码支付', 1, 'wx1234567888aaa', 'wx_secret_xxxxxxx', '1234567890', 'wechat_mch_key_xxxxx', '{"tradeType":"NATIVE"}', 1, 80, '微信原生扫码支付', NOW(), NOW()),
('ALIPAY_APP', '支付宝App支付', 2, '2021001122334455', 'alipay_app_secret_xxx', '2021001122334455', 'alipay_mch_key_xxxxx', '{"payType":"ALIPAY_APP"}', 1, 100, '支付宝APP支付', NOW(), NOW()),
('ALIPAY_WAP', '支付宝H5支付', 2, '2021001122334455', 'alipay_app_secret_xxx', '2021001122334455', 'alipay_mch_key_xxxxx', '{"payWay":"ALIPAY_WAP"}', 1, 90, '支付宝H5支付', NOW(), NOW());

-- ----------------------------
-- 商户支付方式绑定
-- ----------------------------
INSERT INTO `admin_merchant_payment_methods` (`merchant_id`, `payment_method_id`, `enabled`, `priority`, `custom_config_json`, `created_at`, `updated_at`) VALUES
(1, 1, 1, 100, NULL, NOW(), NOW()),
(1, 2, 1, 90, NULL, NOW(), NOW()),
(1, 4, 1, 100, NULL, NOW(), NOW()),
(1, 5, 1, 90, NULL, NOW(), NOW()),
(2, 2, 1, 90, NULL, NOW(), NOW()),
(2, 5, 1, 90, NULL, NOW(), NOW());

-- ----------------------------
-- 支付账号（收款账户池）
-- ----------------------------
INSERT INTO `admin_payment_accounts` (`channel_id`, `account_code`, `account_name`, `app_id`, `app_secret`, `mch_id`, `mch_key`, `config_json`, `enabled`, `priority`, `description`, `created_at`, `updated_at`) VALUES
(1, '1001', '微信收款账户1001', 'wx_1001_app', 'wx_1001_secret', 'wx_mch_1001', 'wx_mch_key_1001', '{"accountNo":"1001"}', 1, 100, '示例微信账户', NOW(), NOW()),
(2, '2001', '支付宝收款账户2001', 'ali_2001_app', 'ali_2001_secret', 'ali_mch_2001', 'ali_mch_key_2001', '{"accountNo":"2001"}', 1, 100, '示例支付宝账户', NOW(), NOW()),
(2, '2002', '支付宝收款账户2002', 'ali_2002_app', 'ali_2002_secret', 'ali_mch_2002', 'ali_mch_key_2002', '{"accountNo":"2002"}', 1, 90, '示例支付宝账户', NOW(), NOW());

-- ----------------------------
-- 商户支付路由（方式+账号）
-- ----------------------------
INSERT INTO `admin_merchant_payment_routes` (`merchant_id`, `payment_method_id`, `payment_account_id`, `enabled`, `priority`, `created_at`, `updated_at`) VALUES
('M2024040001', 1, 1, 1, 100, NOW(), NOW()),
('M2024040001', 4, 2, 1, 100, NOW(), NOW()),
('M10002', 5, 3, 1, 100, NOW(), NOW());
