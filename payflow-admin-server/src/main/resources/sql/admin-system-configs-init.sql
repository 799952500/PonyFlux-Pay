-- ================================================
-- PonyFlux-Pay 系统配置表 DDL + 初始数据
-- ================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table: admin_system_configs 系统配置表
-- ----------------------------
DROP TABLE IF EXISTS `admin_system_configs`;
CREATE TABLE `admin_system_configs` (
  `id`            BIGINT AUTO_INCREMENT PRIMARY KEY,
  `config_key`    VARCHAR(128) NOT NULL UNIQUE COMMENT '配置键（英文唯一标识）',
  `config_value`  TEXT         NOT NULL COMMENT '配置值',
  `value_type`    VARCHAR(32)  NOT NULL DEFAULT 'STRING' COMMENT '类型：STRING/NUMBER/BOOLEAN/JSON',
  `category`     VARCHAR(64)  NOT NULL COMMENT '分类：payment/risk/fee/system',
  `description`   VARCHAR(512)          DEFAULT '' COMMENT '描述',
  `sort_order`   INT          DEFAULT 0 COMMENT '排序',
  `status`       TINYINT      DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `created_at`   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  `updated_at`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_category` (`category`),
  INDEX `idx_status` (`status`),
  INDEX `idx_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- ----------------------------
-- 插入示例数据（5条）
-- ----------------------------
INSERT INTO `admin_system_configs` (`config_key`, `config_value`, `value_type`, `category`, `description`, `sort_order`, `status`) VALUES
  ('max_refund_rate',         '0.5',   'NUMBER',  'risk',    '单笔最大退款比例',          1, 1),
  ('min_payment_amount',       '0.1',   'NUMBER',  'payment', '最小支付金额(元)',          2, 1),
  ('order_timeout_minutes',    '30',    'NUMBER',  'payment', '订单超时时间(分钟)',        3, 1),
  ('enable_whitelist',        'true',  'BOOLEAN', 'risk',    '是否启用白名单',            4, 1),
  ('channel_fee_alipay',      '0.006', 'NUMBER',  'fee',     '支付宝通道费率',            5, 1);

SET FOREIGN_KEY_CHECKS = 1;
