-- 015-cashier-i18n: 支付方式三语 + 订单展示语言

-- admin: 支付方式多语言列
ALTER TABLE `admin_payment_methods`
  ADD COLUMN `method_name_zh_cn` VARCHAR(128) NULL COMMENT '展示名-简体' AFTER `method_name`,
  ADD COLUMN `method_name_zh_tw` VARCHAR(128) NULL COMMENT '展示名-繁体' AFTER `method_name_zh_cn`,
  ADD COLUMN `method_name_en` VARCHAR(128) NULL COMMENT '展示名-英文' AFTER `method_name_zh_tw`,
  ADD COLUMN `description_zh_cn` VARCHAR(512) NULL COMMENT '描述-简体' AFTER `description`,
  ADD COLUMN `description_zh_tw` VARCHAR(512) NULL COMMENT '描述-繁体' AFTER `description_zh_cn`,
  ADD COLUMN `description_en` VARCHAR(512) NULL COMMENT '描述-英文' AFTER `description_zh_tw`;

UPDATE `admin_payment_methods`
SET
  `method_name_zh_cn` = COALESCE(`method_name_zh_cn`, `method_name`),
  `method_name_zh_tw` = COALESCE(`method_name_zh_tw`, `method_name`),
  `method_name_en` = COALESCE(`method_name_en`, `method_name`),
  `description_zh_cn` = COALESCE(`description_zh_cn`, COALESCE(`description`, '')),
  `description_zh_tw` = COALESCE(`description_zh_tw`, COALESCE(`description`, '')),
  `description_en` = COALESCE(`description_en`, COALESCE(`description`, ''))
WHERE `method_name` IS NOT NULL;

ALTER TABLE `admin_payment_methods`
  MODIFY COLUMN `method_name_zh_cn` VARCHAR(128) NOT NULL,
  MODIFY COLUMN `method_name_zh_tw` VARCHAR(128) NOT NULL,
  MODIFY COLUMN `method_name_en` VARCHAR(128) NOT NULL,
  MODIFY COLUMN `description_zh_cn` VARCHAR(512) NOT NULL DEFAULT '',
  MODIFY COLUMN `description_zh_tw` VARCHAR(512) NOT NULL DEFAULT '',
  MODIFY COLUMN `description_en` VARCHAR(512) NOT NULL DEFAULT '';

-- cashier: 订单展示语言
ALTER TABLE `cashier_orders`
  ADD COLUMN `display_language` VARCHAR(16) NOT NULL DEFAULT 'zh-CN' COMMENT '收银台展示语言' AFTER `channel`;

UPDATE `cashier_orders`
SET `display_language` = 'zh-CN'
WHERE `display_language` IS NULL OR `display_language` = '';
