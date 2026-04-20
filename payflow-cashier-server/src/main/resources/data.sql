-- ============================================================
-- PayFlow 收银台初始化数据
-- ============================================================

-- 默认测试商户（密码：123456，MD5 32位小写）
-- appSecret：商户签名密钥，用于 HMAC-SHA256 签名验证
INSERT INTO merchants (merchant_id, merchant_name, password, app_secret, status, contact, phone, description) VALUES
    ('M2024040001', 'XX科技旗舰店', 'e10adc3949ba59abbe56e057f20f883e', '4f3c2b1a0e9d8c7b6a5f4e3d2c1b0a9f', 'ACTIVE', '张经理', '13800138000', '默认测试商户')
ON DUPLICATE KEY UPDATE merchant_name=VALUES(merchant_name), app_secret=VALUES(app_secret);

-- ============================================================
-- 支付渠道初始化数据
-- ============================================================

-- 渠道：微信支付、支付宝、银联
INSERT INTO pay_channels (channel_code, channel_name, icon_url, status, sort_weight, description) VALUES
    ('wechat_pay', '微信支付', '/icons/wechat_pay.png', 'ENABLED', 100, '微信支付WAP/Web/小程序'),
    ('alipay', '支付宝', '/icons/alipay.png', 'ENABLED', 90, '支付宝WAP/Web'),
    ('union_pay', '银联', '/icons/union_pay.png', 'ENABLED', 80, '银联云闪付')
ON DUPLICATE KEY UPDATE channel_name=VALUES(channel_name), icon_url=VALUES(icon_url);

-- ============================================================
-- 渠道账户初始化数据
-- ============================================================

-- 微信账户1（channel_id=1）
INSERT INTO pay_channel_accounts (channel_id, account_code, account_name, channel_config, status, remark) VALUES
    (1, 'WX_ACC_001', '微信账户1', '{"appId":"wx1234567890abcdef","appSecret":"abcdef1234567890","mchId":"1234567890","apiKey":"微信API密钥","notifyUrl":"https://pay.example.com/callback/wechat"}', 'ENABLED', '主账户')
ON DUPLICATE KEY UPDATE account_name=VALUES(account_name), channel_config=VALUES(channel_config);

-- 微信账户2（channel_id=1）
INSERT INTO pay_channel_accounts (channel_id, account_code, account_name, channel_config, status, remark) VALUES
    (1, 'WX_ACC_002', '微信账户2', '{"appId":"wxabcdef1234567890","appSecret":"1234567890abcdef","mchId":"0987654321","apiKey":"微信API密钥2","notifyUrl":"https://pay2.example.com/callback/wechat"}', 'ENABLED', '备用账户')
ON DUPLICATE KEY UPDATE account_name=VALUES(account_name), channel_config=VALUES(channel_config);

-- 支付宝账户（channel_id=2）
INSERT INTO pay_channel_accounts (channel_id, account_code, account_name, channel_config, status, remark) VALUES
    (2, 'ALI_ACC_001', '支付宝账户', '{"appId":"2021001234567890","appSecret":"abcdefghijklmnopqrst","mchId":"","apiKey":"支付宝API密钥","notifyUrl":"https://pay.example.com/callback/alipay"}', 'ENABLED', '主账户')
ON DUPLICATE KEY UPDATE account_name=VALUES(account_name), channel_config=VALUES(channel_config);

-- ============================================================
-- 商户路由初始化数据
-- ============================================================

-- 商户 M2024040001 → 微信账户1（优先级高）
INSERT INTO pay_channel_merchant_routes (channel_account_id, merchant_id, enabled, priority) VALUES
    (1, 'M2024040001', TRUE, 100)
ON DUPLICATE KEY UPDATE enabled=VALUES(enabled), priority=VALUES(priority);

-- 商户 M2024040001 → 微信账户2（优先级低）
INSERT INTO pay_channel_merchant_routes (channel_account_id, merchant_id, enabled, priority) VALUES
    (2, 'M2024040001', TRUE, 50)
ON DUPLICATE KEY UPDATE enabled=VALUES(enabled), priority=VALUES(priority);

-- 商户 M2024040001 → 支付宝账户
INSERT INTO pay_channel_merchant_routes (channel_account_id, merchant_id, enabled, priority) VALUES
    (3, 'M2024040001', TRUE, 100)
ON DUPLICATE KEY UPDATE enabled=VALUES(enabled), priority=VALUES(priority);
