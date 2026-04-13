-- ============================================================
-- PayFlow 收银台初始化数据
-- ============================================================

-- 默认测试商户（密码：123456，MD5 32位小写）
INSERT INTO merchants (merchant_id, merchant_name, password, status, contact, phone, description) VALUES
    ('M2024040001', 'XX科技旗舰店', 'e10adc3949ba59abbe56e057f20f883e', 'ACTIVE', '张经理', '13800138000', '默认测试商户')
ON DUPLICATE KEY UPDATE merchant_name=VALUES(merchant_name);
