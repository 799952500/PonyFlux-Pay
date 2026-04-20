-- Initial Admin Users (passwords are BCrypt hashed)
INSERT INTO admin_users (username, password, role, nickname, status) VALUES
    ('admin', '$2b$10$WckTgmf79epjQOPKCBWFUuGtTCT7ccht7E1fWxskJPpYwlsRkOK7O', 'SUPER_ADMIN', '系统管理员', 'ACTIVE'),
    ('finance', '$2a$10$OLjGMGycC55ShwisGLQ2DeN1.EMGxRPQMnWvTlNp4d/bMGpcORxY6', 'FINANCE', '财务专员', 'ACTIVE'),
    ('risk', '$2a$10$tIfhmNm4xHO5jqYojWQAxOV.FtKwO6VwnlW3VmpNj1cCVhw1hroUG', 'RISK', '风控专员', 'ACTIVE');

-- Mock Channels
INSERT INTO channels (channel_code, channel_name, channel_type, enabled, priority, api_url, api_key, description) VALUES
    ('Alipay', '支付宝', 'ALIPAY', TRUE, 1, 'https://openapi.alipay.com/gateway.do', 'alipay_prod_key_placeholder', '支付宝官方网关通道'),
    ('WeChatPay', '微信支付', 'WECHAT', TRUE, 2, 'https://api.mch.weixin.qq.com/pay/unifiedorder', 'wechat_prod_key_placeholder', '微信支付官方网关通道'),
    ('UnionPay', '银联支付', 'UNION', TRUE, 3, 'https://gateway.95516.com/gateway/api/frontTransReq.do', 'union_prod_key_placeholder', '银联全渠道网关'),
    ('CreditCard', '信用卡', 'CARD', FALSE, 4, 'https://gateway.example.com/card/pay', 'card_prod_key_placeholder', '银行卡快捷支付通道');

-- Mock Risk Rules
INSERT INTO risk_rules (rule_code, rule_name, rule_type, threshold, action, enabled) VALUES
    ('MAX_AMOUNT', '单笔最大金额', 'AMOUNT', 50000.00, 'REJECT', TRUE),
    ('DAILY_LIMIT', '单日累计金额', 'AMOUNT', 200000.00, 'REJECT', TRUE),
    ('HIGH_RISK_IP', '高风险IP拦截', 'IP', NULL, 'REVIEW', FALSE);

-- Mock Merchants
INSERT INTO merchants (merchant_id, merchant_name, merchant_key, callback_url, notify_url, commission_rate, status) VALUES
    ('M001', '测试商户001', 'm001_secret_key_placeholder', 'https://m001.example.com/callback', 'https://m001.example.com/notify', 0.30, 'ACTIVE'),
    ('M002', '测试商户002', 'm002_secret_key_placeholder', 'https://m002.example.com/callback', 'https://m002.example.com/notify', 0.25, 'ACTIVE'),
    ('M003', '测试商户003', 'm003_secret_key_placeholder', 'https://m003.example.com/callback', 'https://m003.example.com/notify', 0.20, 'SUSPENDED');

-- Mock Payment Methods (one per channel)
INSERT INTO payment_methods (method_code, method_name, channel_id, app_id, app_secret, mch_id, mch_key, enabled, priority) VALUES
    ('ALIPAY_WAP', '支付宝手机网站支付', 1, '2021001234567890', 'alipay_app_secret_placeholder', '208800000000001', 'alipay_mch_key_placeholder', TRUE, 1),
    ('ALIPAY_QR', '支付宝扫码支付', 1, '2021001234567890', 'alipay_app_secret_placeholder', '208800000000001', 'alipay_mch_key_placeholder', TRUE, 2),
    ('WECHAT_JSAPI', '微信 JSAPI 支付', 2, 'wx1234567890abcdef', 'wechat_app_secret_placeholder', '1500000001', 'wechat_mch_key_placeholder', TRUE, 1),
    ('WECHAT_NATIVE', '微信 Native 支付', 2, 'wx1234567890abcdef', 'wechat_app_secret_placeholder', '1500000001', 'wechat_mch_key_placeholder', TRUE, 2),
    ('UNION_QR', '银联二维码支付', 3, 'union_app_placeholder', 'union_app_secret_placeholder', '898340183988885', 'union_mch_key_placeholder', TRUE, 1);

-- Mock Merchant-PaymentMethod Bindings
INSERT INTO merchant_payment_methods (merchant_id, payment_method_id, enabled, priority) VALUES
    (1, 1, TRUE, 1),
    (1, 2, TRUE, 2),
    (1, 3, TRUE, 1),
    (1, 4, TRUE, 2),
    (1, 5, TRUE, 3),
    (2, 1, TRUE, 1),
    (2, 3, TRUE, 1),
    (2, 5, TRUE, 2);
