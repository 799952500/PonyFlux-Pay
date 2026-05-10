-- 收银库：支付记录增加收款账户编码，供对账按支付账号汇总本地实收
USE payflow_cashier;

ALTER TABLE cashier_payments
  ADD COLUMN account_code VARCHAR(64) DEFAULT NULL COMMENT '收款渠道账户编码（与 cashier_channel_accounts.account_code 一致）' AFTER pay_channel;

CREATE INDEX idx_cashier_payments_acct_status_date ON cashier_payments (account_code, status, updated_at);
