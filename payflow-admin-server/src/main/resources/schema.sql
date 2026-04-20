-- Admin Users Table
CREATE TABLE IF NOT EXISTS admin_users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'ADMIN',
    nickname VARCHAR(64),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Orders Table (mock)
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL UNIQUE,
    merchant_id VARCHAR(64) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Refunds Table (mock)
CREATE TABLE IF NOT EXISTS refunds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    refund_id VARCHAR(64) NOT NULL UNIQUE,
    order_id VARCHAR(64) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    reason VARCHAR(255),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========== 支付渠道表 ==========
-- 渠道基本信息（接口地址、密钥、开关）
CREATE TABLE IF NOT EXISTS channels (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_code VARCHAR(32) NOT NULL UNIQUE,
    channel_name VARCHAR(64) NOT NULL,
    channel_type VARCHAR(16) NOT NULL,  -- WECHAT/ALIPAY/UNION/CARD
    api_url VARCHAR(512),             -- 渠道API地址
    api_key VARCHAR(256),              -- 渠道密钥/公钥
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT DEFAULT 0,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ========== 支付方式表 ==========
-- 每个渠道下的具体支付方式配置（appId、appSecret、mchId等）
CREATE TABLE IF NOT EXISTS payment_methods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    method_code VARCHAR(32) NOT NULL UNIQUE,
    method_name VARCHAR(64) NOT NULL,
    channel_id BIGINT NOT NULL,                          -- 所属渠道
    app_id VARCHAR(128),                                -- 应用ID
    app_secret VARCHAR(256),                             -- 应用密钥
    mch_id VARCHAR(64),                                -- 商户号
    mch_key VARCHAR(256),                              -- 商户密钥
    cert_path VARCHAR(512),                             -- 证书路径
    cert_password VARCHAR(128),                         -- 证书密码
    config_json TEXT,                                  -- 扩展配置JSON
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT DEFAULT 0,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (channel_id) REFERENCES channels(id)
);

-- ========== 商户表 ==========
-- 商户基本信息（商户密钥、回调地址）
CREATE TABLE IF NOT EXISTS merchants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id VARCHAR(64) NOT NULL UNIQUE,
    merchant_name VARCHAR(128) NOT NULL,
    merchant_key VARCHAR(128) NOT NULL,               -- 商户密钥（用于签名）
    callback_url VARCHAR(512),                        -- 支付结果回调地址
    notify_url VARCHAR(512),                         -- 通知地址
    commission_rate DECIMAL(5,2) DEFAULT 0.00,          -- 手续费分成比例
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ========== 商户支付方式关联表 ==========
-- 商户开通了哪些支付方式
CREATE TABLE IF NOT EXISTS merchant_payment_methods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id BIGINT NOT NULL,
    payment_method_id BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT DEFAULT 0,                          -- 商户级别的优先级
    custom_config_json TEXT,                          -- 商户自定义配置
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (merchant_id) REFERENCES merchants(id),
    FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id),
    UNIQUE KEY uk_merchant_method (merchant_id, payment_method_id)
);

-- ========== 风控规则表 ==========
CREATE TABLE IF NOT EXISTS risk_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_code VARCHAR(64) NOT NULL UNIQUE,
    rule_name VARCHAR(128) NOT NULL,
    rule_type VARCHAR(32) NOT NULL,                    -- AMOUNT/IP/FREQUENCY
    threshold DECIMAL(12, 2),
    action VARCHAR(16) NOT NULL DEFAULT 'REJECT',     -- REJECT/REVIEW/PASS
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);