-- 银联支付方式种子数据：新增 UNION_H5 和 UNION_QR
-- 前置条件：channels 表中已有 UNION_PAY 渠道记录（id 需一致）

-- UNION_H5（若尚不存在才插入）
INSERT IGNORE INTO `payment_methods` (
  `method_code`, `method_name`, `channel_id`, `app_id`, `app_secret`, `mch_id`, `mch_key`,
  `cert_path`, `cert_password`, `config_json`, `enabled`, `priority`, `description`, `created_at`, `updated_at`
)
SELECT 'UNION_H5', '银联云闪付H5', c.id,
       NULL, NULL, NULL, NULL,
       NULL, NULL, '{"payType":"H5"}',
       1, 80, '银联云闪付 H5 支付', NOW(), NOW()
FROM `channels` c WHERE c.`channel_code` = 'UNION_PAY'
LIMIT 1;

-- UNION_QR
INSERT IGNORE INTO `payment_methods` (
  `method_code`, `method_name`, `channel_id`, `app_id`, `app_secret`, `mch_id`, `mch_key`,
  `cert_path`, `cert_password`, `config_json`, `enabled`, `priority`, `description`, `created_at`, `updated_at`
)
SELECT 'UNION_QR', '银联扫码支付', c.id,
       NULL, NULL, NULL, NULL,
       NULL, NULL, '{"payType":"QR"}',
       1, 70, '银联扫码支付（用户云闪付 App 扫码）', NOW(), NOW()
FROM `channels` c WHERE c.`channel_code` = 'UNION_PAY'
LIMIT 1;
