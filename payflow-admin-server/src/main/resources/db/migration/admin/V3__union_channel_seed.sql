-- 银联渠道占位数据（与 PayChannelAccountRegistry.normalizeChannelCode 中 union_pay 一致）
INSERT IGNORE INTO `admin_channels` (`channel_code`, `channel_name`, `icon`, `enabled`, `priority`, `description`, `created_at`, `updated_at`)
VALUES ('union_pay', '银联/云闪付', NULL, 1, 5, '云闪付H5占位，需配置 union_pay 渠道账户与商户路由', NOW(), NOW());
