-- ============================================================
-- PayFlow 收银台初始化数据
-- ============================================================

-- 默认测试商户（密码：123456，MD5 32位小写）
-- appSecret：商户签名密钥，用于 HMAC-SHA256 签名验证
INSERT INTO merchants (merchant_id, merchant_name, password, app_secret, status, contact, phone, description) VALUES
    ('M2024040001', 'XX科技旗舰店', 'e10adc3949ba59abbe56e057f20f883e', '4f3c2b1a0e9d8c7b6a5f4e3d2c1b0a9f', 'ACTIVE', '张经理', '13800138000', '默认测试商户')
ON DUPLICATE KEY UPDATE merchant_name=VALUES(merchant_name), app_secret=VALUES(app_secret);
