-- 银联渠道占位数据（与 PayChannelAccountRegistry.normalizeChannelCode 中 union_pay 一致）
INSERT IGNORE INTO `cashier_channels` (`channel_code`, `channel_name`, `icon_url`, `status`, `sort_weight`, `description`, `created_at`, `updated_at`)
VALUES ('union_pay', '银联/云闪付', NULL, 'ENABLED', 5, '云闪付H5占位，需配置 union_pay 渠道账户与商户路由', NOW(), NOW());
