-- ============================================================
-- PayFlow 收银台数据库 Schema
-- ============================================================
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS callback_logs;
DROP TABLE IF EXISTS merchants;

-- ----------------------------
-- 1. 商户表 merchants
-- ----------------------------
CREATE TABLE IF NOT EXISTS merchants (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    merchant_id         VARCHAR(32) NOT NULL UNIQUE COMMENT '商户号',
    merchant_name       VARCHAR(128) NOT NULL COMMENT '商户名称',
    -- 登录密码（前端传输，未做签名校验，故单独存）
    password            VARCHAR(128) NOT NULL COMMENT '登录密码（MD5）',
    status              VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/INACTIVE/SUSPENDED',
    allowed_channels    TEXT COMMENT '允许的支付渠道（JSON数组）',
    allowed_pay_methods TEXT COMMENT '允许的支付方式（JSON数组）',
    contact             VARCHAR(64) COMMENT '联系人',
    phone               VARCHAR(32) COMMENT '联系电话',
    email               VARCHAR(128) COMMENT '联系邮箱',
    description         VARCHAR(256) COMMENT '商户描述',
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_merchant_status (status)
) COMMENT '商户表';

-- ----------------------------
-- 2. 订单表 orders
-- ----------------------------
CREATE TABLE IF NOT EXISTS orders (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id            VARCHAR(64) NOT NULL UNIQUE COMMENT '平台订单号',
    merchant_id         VARCHAR(64) NOT NULL COMMENT '商户号',
    merchant_order_no   VARCHAR(128) NOT NULL COMMENT '商户侧订单号',
    amount              BIGINT NOT NULL COMMENT '订单金额（分）',
    currency            VARCHAR(8) DEFAULT 'CNY' COMMENT '币种',
    pay_amount          BIGINT DEFAULT 0 COMMENT '实付金额（分）',
    subject             VARCHAR(256) NOT NULL COMMENT '订单标题',
    body                TEXT COMMENT '订单详情',
    attach              TEXT COMMENT '透传字段',
    channel             VARCHAR(32) NOT NULL COMMENT '支付渠道',
    status              VARCHAR(16) NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/PAYING/PAID/EXPIRED/FAILED',
    notify_url          VARCHAR(512) COMMENT '商户异步通知地址',
    return_url          VARCHAR(512) COMMENT '支付完成回跳地址',
    expire_time         TIMESTAMP COMMENT '过期时间',
    pay_time            TIMESTAMP COMMENT '支付时间',
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_orders_merchant (merchant_id),
    INDEX idx_orders_merchant_order (merchant_id, merchant_order_no),
    INDEX idx_orders_status (status)
) COMMENT '订单表';

-- ----------------------------
-- 3. 支付记录表 payments
-- ----------------------------
CREATE TABLE IF NOT EXISTS payments (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id              VARCHAR(64) NOT NULL UNIQUE COMMENT '支付记录ID',
    order_id                VARCHAR(64) NOT NULL COMMENT '关联订单号',
    pay_channel             VARCHAR(32) NOT NULL COMMENT '支付渠道',
    pay_method              VARCHAR(32) NOT NULL COMMENT '支付方式',
    channel_transaction_id  VARCHAR(128) COMMENT '第三方交易号',
    amount                  BIGINT NOT NULL COMMENT '支付金额（分）',
    status                  VARCHAR(16) NOT NULL DEFAULT 'PROCESSING' COMMENT 'PROCESSING/SUCCESS/FAILED',
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_payments_order_id (order_id),
    INDEX idx_payments_status (status)
) COMMENT '支付记录表';

-- ----------------------------
-- 4. 回调日志表 callback_logs
-- ----------------------------
CREATE TABLE IF NOT EXISTS callback_logs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        VARCHAR(64) COMMENT '关联订单号',
    channel         VARCHAR(32) COMMENT '渠道',
    transaction_id  VARCHAR(128) COMMENT '第三方交易流水号',
    raw_request     TEXT COMMENT '原始报文',
    sign_verified   BOOLEAN COMMENT '签名是否验证通过',
    process_status  VARCHAR(16) DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED',
    error_msg       TEXT COMMENT '错误信息',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at    TIMESTAMP COMMENT '处理完成时间'
) COMMENT '回调日志表';
