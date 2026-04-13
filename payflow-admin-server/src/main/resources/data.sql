-- Initial Admin Users (passwords are BCrypt hashed)
INSERT INTO admin_users (username, password, role, nickname, status) VALUES
    ('admin', '$2a$10$.odPcJ6YZKWO9BybsR7BHemz0ziebG5UPUicpvCYV37bVxTcuIl/K', 'SUPER_ADMIN', '系统管理员', 'ACTIVE'),
    ('finance', '$2a$10$OLjGMGycC55ShwisGLQ2DeN1.EMGxRPQMnWvTlNp4d/bMGpcORxY6', 'FINANCE', '财务专员', 'ACTIVE'),
    ('risk', '$2a$10$tIfhmNm4xHO5jqYojWQAxOV.FtKwO6VwnlW3VmpNj1cCVhw1hroUG', 'RISK', '风控专员', 'ACTIVE');

-- Mock Channels
INSERT INTO channels (channel_code, channel_name, enabled, priority) VALUES
    ('Alipay', '支付宝', TRUE, 1),
    ('WeChatPay', '微信支付', TRUE, 2),
    ('UnionPay', '银联支付', TRUE, 3),
    ('CreditCard', '信用卡', FALSE, 4);

-- Mock Risk Rules
INSERT INTO risk_rules (rule_code, rule_name, rule_type, threshold, action, enabled) VALUES
    ('MAX_AMOUNT', '单笔最大金额', 'AMOUNT', 50000.00, 'REJECT', TRUE),
    ('DAILY_LIMIT', '单日累计金额', 'AMOUNT', 200000.00, 'REJECT', TRUE),
    ('HIGH_RISK_IP', '高风险IP拦截', 'IP', NULL, 'REVIEW', FALSE);

-- Mock Merchants
INSERT INTO merchants (merchant_id, merchant_name, status) VALUES
    ('M001', '测试商户001', 'ACTIVE'),
    ('M002', '测试商户002', 'ACTIVE'),
    ('M003', '测试商户003', 'SUSPENDED');
