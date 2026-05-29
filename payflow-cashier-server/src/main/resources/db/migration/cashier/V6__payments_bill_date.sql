-- 可选：支付记录账单日列，便于对账半开区间查询走索引
ALTER TABLE cashier_payments
    ADD COLUMN bill_date DATE NULL COMMENT '账单日(对账/报表)' AFTER status;

UPDATE cashier_payments
SET bill_date = DATE(COALESCE(updated_at, created_at))
WHERE bill_date IS NULL;

CREATE INDEX idx_payments_bill_date_channel ON cashier_payments (bill_date, pay_channel);
