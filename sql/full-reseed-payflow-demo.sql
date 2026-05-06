-- =============================================================================
-- PonyFlux Pay — 演示环境全量初始化（MySQL 8）
--
-- 作用：
--   1）清空两库业务数据（保留表结构）
--   2）按关联关系插入一批连贯的演示数据，覆盖管理后台各列表页 / 仪表盘 / 通知摘要
--
-- 【在 JetBrains DataGrip 中执行】（不用命令行）
--
-- 1）菜单 File → Open，打开本文件 full-reseed-payflow-demo.sql（或从左侧项目树双击）。
-- 2）编辑器右上角「数据源」下拉框：选中你的 MySQL 连接（主机/端口/用户名与平时连库一致即可）。
-- 3）确认该登录用户对 payflow_cashier、payflow_admin 两个库均有 DDL+DML 权限（至少要能 TRUNCATE/INSERT）。
-- 4）执行整份脚本（任选其一）：
--      • 编辑器内右键 → Run '<文件名>'（运行整个文件）；
--      • 或顶部绿色三角「Execute」旁选择执行整个脚本（勿只选中一行就 Run，否则会只跑一句）。
-- 5）脚本内含 USE payflow_cashier / USE payflow_admin，会在同一连接里切换库；若报错「unknown database」，
--      请先在控制台执行下方两条 CREATE DATABASE（选中后 Ctrl+Enter 单段执行即可）。
--
-- 提示：若 DataGrip 提示不允许多语句，请在数据源 Properties → Options / Advanced 中确认允许多语句执行（MySQL 一般默认即可）。
--
-- 【为何日志里大量「0 行受到影响」？】这是正常现象，不是没执行成功。
--   • SET NAMES / USE 库名 / SET FOREIGN_KEY_CHECKS：不改表数据，客户端通常报 0 行。
--   • TRUNCATE：清空表，MySQL JDBC/DataGrip 往往也显示 0 行（表已被清空）。
--   • ALTER TABLE（含 AUTO_INCREMENT、MODIFY 列）：改的是表结构，一般显示 0 行。
--   • 真正写入数据时请看 INSERT 后面的数字：批处理模式可能把多句合并显示为「23 行」「130 行」等，
--     表示这一批里共有这么多行被写入（不是只有一句 INSERT）。
--   可在执行后自行验证：SELECT COUNT(*) FROM payflow_cashier.cashier_merchants;
--                       SELECT COUNT(*) FROM payflow_admin.admin_users;
--
-- 【如何用命令行执行】（可选）把 mysql 命令里的用户名改成你的 MySQL 账号（常见为 root）
--
-- Linux / macOS（项目在仓库根目录时）：
--   mysql -h127.0.0.1 -P3306 -uroot -p < sql/full-reseed-payflow-demo.sql
--
-- Windows CMD（先 cd 到仓库根目录，路径含中文请保持引号）：
--   mysql -h127.0.0.1 -P3306 -uroot -p < sql\full-reseed-payflow-demo.sql
--
-- Windows PowerShell（推荐用 Get-Content 管道，避免重定向编码问题）：
--   Get-Content -Raw "sql\full-reseed-payflow-demo.sql" | mysql -h127.0.0.1 -P3306 -uroot -p
--
-- 说明：
--   -u root       ：MySQL 登录用户名，若你用别的账号（如 payflow）则写成 -upayflow
--   -p             ：回车后输入该用户的密码；也可写成 -p你的密码（不推荐，会留在命令历史中）
--   -h / -P        ：主机与端口，本地默认可省略为 mysql -uroot -p < ...
--
-- 执行前请确认已创建数据库：
--   CREATE DATABASE IF NOT EXISTS payflow_admin   DEFAULT CHARACTER SET utf8mb4;
--   CREATE DATABASE IF NOT EXISTS payflow_cashier DEFAULT CHARACTER SET utf8mb4;
--
-- 默认库名与 application.yml 一致：payflow_admin、payflow_cashier
--
-- 【重要】表名与 payflow-admin-server 实体一致：
--   payflow_admin：channels, merchants, payment_methods, merchant_payment_methods,
--       payment_accounts, merchant_payment_routes, admin_channel_routes,
--       risk_rules, admin_system_configs, admin_users, admin_audit_logs,
--       sys_roles, sys_menus, sys_role_menus, sys_users, sys_user_roles
--
-- 若你的库曾执行「物理表改名为 admin_*」且未创建同名 VIEW，
-- 请先将下面脚本中的上述表名批量替换为 admin_channels、admin_merchants…
-- =============================================================================

SET NAMES utf8mb4;

/* ========================= 一、收银台库 payflow_cashier ========================= */
USE payflow_cashier;

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE cashier_refunds;
TRUNCATE TABLE cashier_payments;
TRUNCATE TABLE cashier_orders;
TRUNCATE TABLE cashier_channel_merchant_routes;
TRUNCATE TABLE cashier_channel_accounts;
TRUNCATE TABLE cashier_channels;
TRUNCATE TABLE cashier_merchants;

SET FOREIGN_KEY_CHECKS = 1;

-- 商户（收银台侧）
INSERT INTO cashier_merchants (
  merchant_id, merchant_name, password, app_secret, status,
  allowed_channels, allowed_pay_methods, contact, phone, email, description,
  created_at, updated_at
) VALUES
('M100001', '演示旗舰店', NULL, 'demo_app_secret_m100001_32chars_hex01', 'ACTIVE',
 'WECHAT_PAY,ALIPAY', 'WECHAT_APP,WECHAT_H5,ALIPAY_APP,ALIPAY_WAP',
 '张演示', '13800138001', 'demo@m100001.example.com', '演示主商户',
 NOW(), NOW()),
('M100002', '演示分店', NULL, 'demo_app_secret_m100002_32chars_hex02', 'ACTIVE',
 'WECHAT_PAY', 'WECHAT_H5',
 '李演示', '13800138002', 'demo@m100002.example.com', '演示副商户',
 NOW(), NOW());

-- 渠道（收银台侧展示用）
INSERT INTO cashier_channels (
  channel_code, channel_name, icon_url, status, sort_weight, description, created_at, updated_at
) VALUES
('WECHAT_PAY', '微信支付', '/icons/wechat.svg', 'ENABLED', 100, '微信', NOW(), NOW()),
('ALIPAY', '支付宝', '/icons/alipay.svg', 'ENABLED', 90, '支付宝', NOW(), NOW()),
('UNION_PAY', '银联', '/icons/union.svg', 'ENABLED', 80, '银联', NOW(), NOW());

-- 渠道账户（channel_id 对应上表 id=1,2,3）
INSERT INTO cashier_channel_accounts (
  channel_id, account_code, account_name, channel_config, status, remark, created_at, updated_at
) VALUES
(1, 'CASHIER_WX_001', '微信主账户', '{"env":"sandbox"}', 'ENABLED', '演示', NOW(), NOW()),
(2, 'CASHIER_ALI_001', '支付宝主账户', '{"env":"sandbox"}', 'ENABLED', '演示', NOW(), NOW()),
(3, 'CASHIER_UNION_001', '银联主账户', '{"env":"sandbox"}', 'ENABLED', '演示', NOW(), NOW());

-- 商户 ↔ 渠道账户路由
INSERT INTO cashier_channel_merchant_routes (
  channel_account_id, merchant_id, enabled, priority, created_at, updated_at
) VALUES
(1, 'M100001', 1, 100, NOW(), NOW()),
(2, 'M100001', 1, 90, NOW(), NOW()),
(1, 'M100002', 1, 100, NOW(), NOW());

-- 订单（含「今日」已支付，便于仪表盘有数；渠道字段用于分布图）
INSERT INTO cashier_orders (
  id, order_id, merchant_id, merchant_order_no, amount, currency, pay_amount,
  subject, body, attach, channel, status,
  notify_url, merchant_notify_url, return_url, success_url, fail_url,
  expire_time, pay_time, created_at, updated_at, notify_status, notify_retry_count
) VALUES
(200001, 'ORD-DEMO-202602040001', 'M100001', 'MPO202602040001', 19900, 'CNY', 19900,
 '演示商品 A', 'SKU-A', NULL, 'WECHAT_PAY', 'PAID',
 'https://demo.example.com/notify', 'https://demo.example.com/mch/cb', NULL,
 'https://demo.example.com/ok', 'https://demo.example.com/fail',
 DATE_ADD(NOW(), INTERVAL 2 HOUR), NOW(), NOW(), NOW(), 'SUCCESS', 1),
(200002, 'ORD-DEMO-202602040002', 'M100001', 'MPO202602040002', 8800, 'CNY', NULL,
 '演示商品 B', 'SKU-B', NULL, 'ALIPAY', 'CREATED',
 'https://demo.example.com/notify', NULL, NULL, NULL, NULL,
 DATE_ADD(NOW(), INTERVAL 30 MINUTE), NULL, NOW(), NOW(), 'PENDING', 0),
(200003, 'ORD-DEMO-202602040003', 'M100002', 'MPO202602040003', 50000, 'CNY', 50000,
 '演示商品 C', 'SKU-C', NULL, 'ALIPAY', 'SUCCESS',
 'https://demo.example.com/notify', NULL, NULL, NULL, NULL,
 DATE_ADD(NOW(), INTERVAL 1 HOUR), NOW(), DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 'SUCCESS', 2),
(200004, 'ORD-DEMO-202602040004', 'M100002', 'MPO202602040004', 12000, 'CNY', NULL,
 '演示商品 D', 'SKU-D', NULL, 'WECHAT_PAY', 'EXPIRED',
 NULL, NULL, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), 'PENDING', 0),
(200005, 'ORD-DEMO-202602040005', 'M100001', 'MPO202602040005', 6600, 'CNY', NULL,
 '演示商品 E', 'SKU-E', NULL, 'WECHAT_PAY', 'PAYING',
 NULL, NULL, NULL, NULL, NULL,
 DATE_ADD(NOW(), INTERVAL 15 MINUTE), NULL, NOW(), NOW(), 'PENDING', 0);

-- 支付流水（与订单关联）
INSERT INTO cashier_payments (
  id, payment_id, order_id, pay_channel, pay_method, channel_transaction_id, amount, status, created_at, updated_at
) VALUES
(300001, 'PAY-DEMO-001', 'ORD-DEMO-202602040001', 'WECHAT_PAY', 'WECHAT_APP', 'WX_TXN_DEMO_001', 19900, 'SUCCESS', NOW(), NOW()),
(300002, 'PAY-DEMO-002', 'ORD-DEMO-202602040003', 'ALIPAY', 'ALIPAY_APP', 'ALI_TXN_DEMO_002', 50000, 'SUCCESS', DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
(300003, 'PAY-DEMO-003', 'ORD-DEMO-202602040005', 'WECHAT_PAY', 'WECHAT_H5', NULL, 6600, 'PROCESSING', NOW(), NOW());

-- 退款（含 REFUNDING 以便「通知中心」有待办笔数）
INSERT INTO cashier_refunds (
  refund_id, payment_id, order_id, pay_channel, refund_amount, reason, status,
  channel_refund_no, merchant_refund_no, created_at, updated_at
) VALUES
('REF-DEMO-PENDING-01', 'PAY-DEMO-001', 'ORD-DEMO-202602040001', 'WECHAT_PAY', 9900,
 '用户申请半价退款演示', 'REFUNDING', NULL, 'MRN-DEMO-001', NOW(), NOW()),
('REF-DEMO-PENDING-02', 'PAY-DEMO-002', 'ORD-DEMO-202602040003', 'ALIPAY', 10000,
 '重复支付退款演示', 'REFUNDING', NULL, 'MRN-DEMO-002', NOW(), NOW()),
('REF-DEMO-DONE-01', 'PAY-DEMO-002', 'ORD-DEMO-202602040003', 'ALIPAY', 5000,
 '部分退款已完成演示', 'REFUNDED', 'ALI_REFUND_X1', 'MRN-DEMO-003',
 DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
('REF-DEMO-FAIL-01', 'PAY-DEMO-001', 'ORD-DEMO-202602040001', 'WECHAT_PAY', 19900,
 '全额退款失败演示', 'FAILED', NULL, 'MRN-DEMO-004',
 DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY));

-- 收银台库自增偏移（必须在 USE payflow_admin 之前执行：当前库仍为 payflow_cashier）
ALTER TABLE cashier_merchants AUTO_INCREMENT = 100;
ALTER TABLE cashier_channels AUTO_INCREMENT = 100;
ALTER TABLE cashier_channel_accounts AUTO_INCREMENT = 100;
ALTER TABLE cashier_channel_merchant_routes AUTO_INCREMENT = 100;
ALTER TABLE cashier_orders AUTO_INCREMENT = 300000;
ALTER TABLE cashier_payments AUTO_INCREMENT = 400000;
ALTER TABLE cashier_refunds AUTO_INCREMENT = 500000;


/* ========================= 二、运营库 payflow_admin ========================= */
USE payflow_admin;

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE sys_user_roles;
TRUNCATE TABLE sys_role_menus;
TRUNCATE TABLE recon_handler_audit;
TRUNCATE TABLE recon_diff;
TRUNCATE TABLE recon_bill_record;
TRUNCATE TABLE recon_task;
TRUNCATE TABLE admin_audit_logs;
TRUNCATE TABLE admin_channel_routes;
TRUNCATE TABLE merchant_payment_routes;
TRUNCATE TABLE merchant_payment_methods;
TRUNCATE TABLE payment_methods;
TRUNCATE TABLE payment_accounts;
TRUNCATE TABLE merchants;
TRUNCATE TABLE channels;
TRUNCATE TABLE risk_rules;
TRUNCATE TABLE admin_system_configs;
TRUNCATE TABLE sys_menus;
TRUNCATE TABLE sys_roles;
TRUNCATE TABLE sys_users;
TRUNCATE TABLE admin_users;

SET FOREIGN_KEY_CHECKS = 1;

-- 与实体一致：merchant_payment_methods.merchant_id 存商户号（字符串）
ALTER TABLE merchant_payment_methods MODIFY COLUMN merchant_id VARCHAR(64) NOT NULL COMMENT '商户号';

-- ---------------------------------------------------------------------------
-- 登录账号说明：
--   admin_users：管理端 JWT 登录（AuthController），用户名 admin，密码 admin123
--   sys_users：「用户管理」页面数据，同样便于演示登录体系外账号
-- BCrypt(admin123) — 与仓库 sql/admin/data.sql 一致
-- ---------------------------------------------------------------------------
INSERT INTO admin_users (
  id, username, password, role, nickname, status, data_merchant_ids, created_at, updated_at
) VALUES
(1, 'admin', '$2a$10$N.zmdr9k7uOCQb376CWq7uI6EBDlO1R2gRiQDsvbfmS5W3j3M5a0q',
 'SUPER_ADMIN', '超级管理员', 'ACTIVE', NULL, NOW(), NOW()),
(2, 'finance_demo', '$2a$10$N.zmdr9k7uOCQb376CWq7uI6EBDlO1R2gRiQDsvbfmS5W3j3M5a0q',
 'FINANCE', '演示财务', 'ACTIVE', 'M100001', NOW(), NOW());

-- 渠道（运营配置；若表含 icon 列可自行 ALTER 补充）
INSERT INTO channels (
  id, channel_code, channel_name, channel_type, api_url, api_key, enabled, priority, description, created_at, updated_at
) VALUES
(1, 'WECHAT_PAY', '微信支付', 'WECHAT', 'https://api.mch.weixin.qq.com', 'demo-wechat-key', 1, 100, '演示微信渠道', NOW(), NOW()),
(2, 'ALIPAY', '支付宝', 'ALIPAY', 'https://openapi.alipay.com', 'demo-alipay-key', 1, 90, '演示支付宝渠道', NOW(), NOW()),
(3, 'UNION_PAY', '银联', 'UNION', 'https://gateway.example.com/union', 'demo-union-key', 1, 80, '演示银联渠道', NOW(), NOW());

-- 商户（运营侧，merchant_id 与收银台订单对齐）
INSERT INTO merchants (
  id, merchant_id, merchant_name, merchant_key, callback_url, notify_url, commission_rate, status, created_at, updated_at
) VALUES
(1, 'M100001', '演示旗舰店', 'merchant_key_demo_m100001_xxxxxxxx', 'https://demo.example.com/cb', 'https://demo.example.com/notify', 0.0060, 'ACTIVE', NOW(), NOW()),
(2, 'M100002', '演示分店', 'merchant_key_demo_m100002_xxxxxxxx', 'https://demo2.example.com/cb', 'https://demo2.example.com/notify', 0.0055, 'ACTIVE', NOW(), NOW());

-- 支付账号池
INSERT INTO payment_accounts (
  id, channel_id, account_code, account_name, app_id, app_secret, mch_id, mch_key,
  cert_path, cert_password, config_json, enabled, priority, description, created_at, updated_at
) VALUES
(1, 1, 'ACC-WX-001', '微信收款账号001', 'wx_demo_appid', 'wx_demo_secret', 'wx_mch_001', 'wx_mch_key_demo',
 NULL, NULL, '{"pool":"A"}', 1, 100, '演示微信账号', NOW(), NOW()),
(2, 2, 'ACC-ALI-001', '支付宝收款账号001', 'ali_demo_appid', 'ali_demo_secret', 'ali_pid_001', 'ali_key_demo',
 NULL, NULL, '{"pool":"B"}', 1, 100, '演示支付宝账号', NOW(), NOW()),
(3, 2, 'ACC-ALI-002', '支付宝收款账号002', 'ali_demo_appid_2', 'ali_demo_secret_2', 'ali_pid_002', 'ali_key_demo_2',
 NULL, NULL, '{"pool":"C"}', 1, 90, '演示支付宝备用账号', NOW(), NOW());

-- 支付方式（挂在渠道下；与 admin-data.sql 列一致，不含 status 时可省略）
INSERT INTO payment_methods (
  id, method_code, method_name, channel_id, app_id, app_secret, mch_id, mch_key,
  cert_path, cert_password, config_json, enabled, priority, description, created_at, updated_at
) VALUES
(1, 'WECHAT_APP', '微信 App 支付', 1, 'wx_demo_appid', 'wx_demo_secret', 'wx_mch_001', 'wx_mch_key_demo',
 NULL, NULL, '{"tradeType":"APP"}', 1, 100, '演示', NOW(), NOW()),
(2, 'WECHAT_H5', '微信 H5 支付', 1, 'wx_demo_appid', 'wx_demo_secret', 'wx_mch_001', 'wx_mch_key_demo',
 NULL, NULL, '{"tradeType":"MWEB"}', 1, 90, '演示', NOW(), NOW()),
(3, 'ALIPAY_APP', '支付宝 App 支付', 2, 'ali_demo_appid', 'ali_demo_secret', 'ali_pid_001', 'ali_key_demo',
 NULL, NULL, '{"payType":"APP"}', 1, 100, '演示', NOW(), NOW()),
(4, 'ALIPAY_WAP', '支付宝 WAP', 2, 'ali_demo_appid', 'ali_demo_secret', 'ali_pid_001', 'ali_key_demo',
 NULL, NULL, '{"payWay":"WAP"}', 1, 90, '演示', NOW(), NOW());

-- 商户开通支付方式
INSERT INTO merchant_payment_methods (
  id, merchant_id, payment_method_id, enabled, priority, custom_config_json, created_at, updated_at
) VALUES
(1, 'M100001', 1, 1, 100, NULL, NOW(), NOW()),
(2, 'M100001', 2, 1, 90, NULL, NOW(), NOW()),
(3, 'M100001', 3, 1, 100, NULL, NOW(), NOW()),
(4, 'M100002', 2, 1, 90, NULL, NOW(), NOW()),
(5, 'M100002', 4, 1, 90, NULL, NOW(), NOW());

-- 商户支付路由表若仍为旧结构（无 client_scopes），先加列再 INSERT，避免 1054 Unknown column
-- （列已存在时下方语句执行为 SELECT 1，可重复跑本脚本）
SET @sql_mpr = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'merchant_payment_routes'
        AND COLUMN_NAME = 'client_scopes'
    ),
    'SELECT 1',
    'ALTER TABLE merchant_payment_routes ADD COLUMN `client_scopes` VARCHAR(64) NOT NULL DEFAULT ''PC,H5,APP'' COMMENT ''终端可见：PC,H5,APP 逗号分隔'' AFTER `priority`'
  )
);
PREPARE stmt_mpr FROM @sql_mpr;
EXECUTE stmt_mpr;
DEALLOCATE PREPARE stmt_mpr;

SET @sql_ampr = (
  SELECT IF(
    EXISTS(
      SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'admin_merchant_payment_routes'
        AND COLUMN_NAME = 'client_scopes'
    ),
    'SELECT 1',
    IF(
      EXISTS(
        SELECT 1 FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'admin_merchant_payment_routes'
          AND TABLE_TYPE = 'BASE TABLE'
      ),
      'ALTER TABLE admin_merchant_payment_routes ADD COLUMN `client_scopes` VARCHAR(64) NOT NULL DEFAULT ''PC,H5,APP'' COMMENT ''终端可见：PC,H5,APP 逗号分隔'' AFTER `priority`',
      'SELECT 1'
    )
  )
);
PREPARE stmt_ampr FROM @sql_ampr;
EXECUTE stmt_ampr;
DEALLOCATE PREPARE stmt_ampr;

-- 商户支付路由（方式 + 账号）
INSERT INTO merchant_payment_routes (
  id, merchant_id, payment_method_id, payment_account_id, enabled, priority, client_scopes, created_at, updated_at
) VALUES
(1, 'M100001', 1, 1, 1, 100, 'PC,APP', NOW(), NOW()),
(2, 'M100001', 2, 1, 1, 90, 'H5', NOW(), NOW()),
(3, 'M100001', 3, 2, 1, 100, 'PC,H5,APP', NOW(), NOW()),
(4, 'M100002', 4, 3, 1, 80, 'PC,H5,APP', NOW(), NOW());

-- 渠道路由（渠道级：商户 + 渠道 + 账号）
INSERT INTO admin_channel_routes (
  id, merchant_id, channel_id, payment_account_id, enabled, priority, description, created_at, updated_at
) VALUES
(1, 'M100001', 1, 1, 1, 10, '演示旗舰走微信主账号', NOW(), NOW()),
(2, 'M100001', 2, 2, 1, 20, '演示旗舰走支付宝主账号', NOW(), NOW()),
(3, 'M100002', 2, 3, 1, 30, '演示分店走支付宝备用账号', NOW(), NOW());

-- 风控规则
INSERT INTO risk_rules (
  id, rule_code, rule_name, rule_type, threshold, unit, action, enabled, description, created_at, updated_at
) VALUES
(1, 'RISK_AMT_SINGLE', '单笔限额', 'AMOUNT_SINGLE', 500000.00, 'CNY_FEN', 'REJECT', 1, '单笔超过 5000 元拒绝', NOW(), NOW()),
(2, 'RISK_AMT_DAILY', '单日累计限额', 'AMOUNT_DAILY', 5000000.00, 'CNY_FEN', 'REVIEW', 1, '单日累计超过 5 万元人工复核', NOW(), NOW()),
(3, 'RISK_IP_LIMIT', '同 IP 频次', 'IP_LIMIT', 50.00, 'TIMES_PER_HOUR', 'REJECT', 1, '单 IP 每小时下单上限演示', NOW(), NOW());

-- 系统配置（设置页按分类展示）
INSERT INTO admin_system_configs (
  id, config_key, config_value, value_type, category, description, sort_order, status, created_at, updated_at
) VALUES
(1, 'payment.timeout_minutes', '30', 'NUMBER', 'payment', '下单支付超时（分钟）', 10, 1, NOW(), NOW()),
(2, 'payment.notify_retry_max', '5', 'NUMBER', 'payment', '异步通知最大重试次数', 20, 1, NOW(), NOW()),
(3, 'risk.enabled', 'true', 'BOOLEAN', 'risk', '是否启用风控拦截', 10, 1, NOW(), NOW()),
(4, 'fee.platform_rate', '0.0025', 'NUMBER', 'fee', '平台基础费率（小数）', 10, 1, NOW(), NOW()),
(5, 'system.site_name', 'PonyFlux Pay 演示环境', 'STRING', 'system', '站点展示名称', 10, 1, NOW(), NOW()),
(6, 'system.support_email', 'support@demo.example.com', 'STRING', 'system', '支持邮箱', 20, 1, NOW(), NOW());

-- 审计日志
INSERT INTO admin_audit_logs (
  username, action, resource_path, detail, client_ip, created_at
) VALUES
('admin', 'POST', '/api/v1/admin/auth/login', '登录成功', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
('admin', 'PUT', '/api/v1/admin/channels/1', '更新渠道：微信支付', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
('finance_demo', 'GET', '/api/v1/admin/orders', '查询订单列表', '10.0.0.8', DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
('admin', 'POST', '/api/v1/admin/refunds/REF-DEMO-PENDING-01/reject', '拒绝退款演示', '127.0.0.1', DATE_SUB(NOW(), INTERVAL 10 MINUTE)),
('admin', 'GET', '/api/v1/admin/audit-logs', '查询审计日志', '127.0.0.1', NOW());

-- 对账演示（recon_* 与业务同库 payflow_admin）
INSERT INTO recon_task (
  task_id, channel, account_code, bill_date, bill_type, status,
  file_object_key, file_size,
  bill_total_count, bill_total_amount, local_total_count, local_total_amount,
  diff_count, elapsed_ms, error_msg, triggered_by, xxl_log_id, created_at, updated_at
) VALUES
('RECON-DEMO-001', 'alipay', 'CASHIER_ALI_001', '2020-01-01', 'trade', 'SUCCESS',
 NULL, NULL,
 2, 50000, 1, 19900,
 2, 3200, NULL, 'MANUAL', NULL, NOW(), NOW()),
('RECON-DEMO-FAIL-01', 'wxpay', 'CASHIER_WX_001', '2020-01-02', 'trade', 'FAIL',
 NULL, NULL,
 NULL, NULL, NULL, NULL,
 0, NULL, '演示：拉取账单失败（未配置真实渠道密钥）', 'XXL_JOB', NULL, NOW(), NOW());

INSERT INTO recon_bill_record (
  task_id, channel, channel_trade_no, out_trade_no, amount_fen, refund_fen,
  channel_status, finish_time, raw_line, parse_error, created_at
) VALUES
('RECON-DEMO-001', 'alipay', 'ALI_TXN_DEMO_002', 'ORD-DEMO-202602040003', 50001, 0, 'SUCCESS', NOW(), 'demo,csv,line', 0, NOW()),
('RECON-DEMO-001', 'alipay', 'ALI_ONLY_999', NULL, 100, 0, 'SUCCESS', NOW(), 'demo', 0, NOW());

INSERT INTO recon_diff (
  task_id, diff_type, channel_trade_no, local_order_id,
  channel_amount, local_amount, channel_status, local_status,
  handle_status, handle_remark, handled_by, handled_at, created_at
) VALUES
('RECON-DEMO-001', 'AMOUNT_MISMATCH', 'ALI_TXN_DEMO_002', 'ORD-DEMO-202602040003', 50001, 50000, 'SUCCESS', 'SUCCESS', 'PENDING', NULL, NULL, NULL, NOW()),
('RECON-DEMO-001', 'CHANNEL_ONLY', 'ALI_ONLY_999', NULL, 100, NULL, 'SUCCESS', NULL, 'PENDING', NULL, NULL, NULL, NOW());

-- RBAC：角色
INSERT INTO sys_roles (id, role_code, role_name, description, status, created_at, updated_at) VALUES
(1, 'SUPER_ADMIN', '超级管理员', '全量菜单', 'ACTIVE', NOW(), NOW()),
(2, 'ADMIN', '管理员', '业务管理', 'ACTIVE', NOW(), NOW()),
(3, 'FINANCE', '财务', '订单与退款', 'ACTIVE', NOW(), NOW()),
(4, 'RISK', '风控', '风控规则', 'ACTIVE', NOW(), NOW());

-- RBAC：菜单（覆盖路由表中全部后台页面）
INSERT INTO sys_menus (
  id, parent_id, menu_code, menu_name, menu_type, path, icon, sort_order, visible, status, created_at, updated_at
) VALUES
(1, NULL, 'grp_workspace', '工作台', 'MENU', NULL, '🏠', 1, 1, 'ACTIVE', NOW(), NOW()),
(2, 1, 'dashboard', '数据概览', 'MENU', '/admin/dashboard', NULL, 1, 1, 'ACTIVE', NOW(), NOW()),
(3, 1, 'notifications', '通知中心', 'MENU', '/admin/notifications', NULL, 2, 1, 'ACTIVE', NOW(), NOW()),
(4, 1, 'search', '全局搜索', 'MENU', '/admin/search', NULL, 3, 1, 'ACTIVE', NOW(), NOW()),
(10, NULL, 'grp_trade', '交易', 'MENU', NULL, '📋', 2, 1, 'ACTIVE', NOW(), NOW()),
(11, 10, 'orders', '订单管理', 'MENU', '/admin/orders', NULL, 1, 1, 'ACTIVE', NOW(), NOW()),
(12, 10, 'refunds', '退款管理', 'MENU', '/admin/refunds', NULL, 2, 1, 'ACTIVE', NOW(), NOW()),
(13, 10, 'reconcile', '资金对账', 'MENU', '/admin/reconcile', NULL, 3, 1, 'ACTIVE', NOW(), NOW()),
(20, NULL, 'grp_channel', '渠道与支付', 'MENU', NULL, '💳', 3, 1, 'ACTIVE', NOW(), NOW()),
(21, 20, 'channels', '渠道管理', 'MENU', '/admin/channels', NULL, 1, 1, 'ACTIVE', NOW(), NOW()),
(22, 20, 'channel_routes', '支付路由', 'MENU', '/admin/channel-routes', NULL, 2, 1, 'ACTIVE', NOW(), NOW()),
(23, 20, 'merchant_payments', '商户支付方式', 'MENU', '/admin/merchant-payments', NULL, 3, 1, 'ACTIVE', NOW(), NOW()),
(24, 20, 'payment_methods', '支付方式', 'MENU', '/admin/payment-methods', NULL, 4, 1, 'ACTIVE', NOW(), NOW()),
(25, 20, 'payment_accounts', '支付账号', 'MENU', '/admin/payment-accounts', NULL, 5, 1, 'ACTIVE', NOW(), NOW()),
(30, NULL, 'grp_merchant', '商户与风控', 'MENU', NULL, '🏪', 4, 1, 'ACTIVE', NOW(), NOW()),
(31, 30, 'merchants', '商户管理', 'MENU', '/admin/merchants', NULL, 1, 1, 'ACTIVE', NOW(), NOW()),
(32, 30, 'risk', '风控配置', 'MENU', '/admin/risk', NULL, 2, 1, 'ACTIVE', NOW(), NOW()),
(40, NULL, 'grp_system', '系统', 'MENU', NULL, '⚙️', 5, 1, 'ACTIVE', NOW(), NOW()),
(41, 40, 'settings', '系统设置', 'MENU', '/admin/settings', NULL, 1, 1, 'ACTIVE', NOW(), NOW()),
(42, 40, 'dicts', '数据字典', 'MENU', '/admin/dicts', NULL, 2, 1, 'ACTIVE', NOW(), NOW()),
(50, NULL, 'grp_admin', '权限运维', 'MENU', NULL, '🔧', 6, 1, 'ACTIVE', NOW(), NOW()),
(51, 50, 'roles', '角色管理', 'MENU', '/admin/roles', NULL, 1, 1, 'ACTIVE', NOW(), NOW()),
(52, 50, 'menus', '菜单管理', 'MENU', '/admin/menus', NULL, 2, 1, 'ACTIVE', NOW(), NOW()),
(53, 50, 'users', '用户管理', 'MENU', '/admin/users', NULL, 3, 1, 'ACTIVE', NOW(), NOW()),
(54, 50, 'audit_logs', '操作日志', 'MENU', '/admin/audit-logs', NULL, 4, 1, 'ACTIVE', NOW(), NOW());

-- 超级管理员 ← 全部菜单
INSERT INTO sys_role_menus (role_id, menu_id, created_at)
SELECT 1, id, NOW() FROM sys_menus;

-- 管理员：除「权限运维」分组（菜单 id 50–54）外全选
INSERT INTO sys_role_menus (role_id, menu_id, created_at)
SELECT 2, id, NOW() FROM sys_menus WHERE id NOT BETWEEN 50 AND 54;

-- 财务：工作台 + 交易
INSERT INTO sys_role_menus (role_id, menu_id, created_at)
SELECT 3, id, NOW() FROM sys_menus WHERE id IN (1,2,3,4,10,11,12,13);

-- 风控：工作台 + 商户与风控 + 订单列表
INSERT INTO sys_role_menus (role_id, menu_id, created_at)
SELECT 4, id, NOW() FROM sys_menus WHERE id IN (1,2,10,11,30,31,32);

-- 系统用户（用户管理页）
INSERT INTO sys_users (
  id, username, password, nickname, phone, email, status, created_at, updated_at
) VALUES
(1, 'sys_admin', '$2a$10$N.zmdr9k7uOCQb376CWq7uI6EBDlO1R2gRiQDsvbfmS5W3j3M5a0q',
 '系统管理员', '13800001111', 'sys_admin@demo.local', 'ACTIVE', NOW(), NOW()),
(2, 'sys_operator', '$2a$10$N.zmdr9k7uOCQb376CWq7uI6EBDlO1R2gRiQDsvbfmS5W3j3M5a0q',
 '运营小王', '13800002222', 'ops@demo.local', 'ACTIVE', NOW(), NOW()),
(3, 'sys_auditor', '$2a$10$N.zmdr9k7uOCQb376CWq7uI6EBDlO1R2gRiQDsvbfmS5W3j3M5a0q',
 '审计（禁用演示）', '13800003333', 'audit@demo.local', 'DISABLED', NOW(), NOW());

INSERT INTO sys_user_roles (user_id, role_id, created_at) VALUES
(1, 1, NOW()),
(2, 2, NOW()),
(3, 4, NOW());

-- 自增偏移：避免后续界面自增与固定 id 冲突
ALTER TABLE channels AUTO_INCREMENT = 100;
ALTER TABLE merchants AUTO_INCREMENT = 100;
ALTER TABLE payment_accounts AUTO_INCREMENT = 100;
ALTER TABLE payment_methods AUTO_INCREMENT = 100;
ALTER TABLE merchant_payment_methods AUTO_INCREMENT = 100;
ALTER TABLE merchant_payment_routes AUTO_INCREMENT = 100;
ALTER TABLE admin_channel_routes AUTO_INCREMENT = 100;
ALTER TABLE risk_rules AUTO_INCREMENT = 100;
ALTER TABLE admin_system_configs AUTO_INCREMENT = 100;
ALTER TABLE admin_users AUTO_INCREMENT = 100;
ALTER TABLE sys_roles AUTO_INCREMENT = 100;
ALTER TABLE sys_menus AUTO_INCREMENT = 100;
ALTER TABLE sys_users AUTO_INCREMENT = 100;

-- =============================================================================
-- 完成
--
-- 【管理后台 JWT 登录】（表 admin_users，用于 /login）
--   用户名 admin          密码 admin123    角色 SUPER_ADMIN（全数据权限）
--   用户名 finance_demo   密码 admin123    角色 FINANCE（仅商户号 M100001）
--
-- 【用户管理页 sys_users】（与上不同表，用于「用户管理」菜单演示）
--   用户名 sys_admin      密码 admin123    绑定超级管理员角色
--   用户名 sys_operator   密码 admin123    绑定管理员角色
--   用户名 sys_auditor    密码 admin123    状态 DISABLED（禁用演示）
--
-- 【通知中心】待退款笔数应为 2（两条 REFUNDING）
-- =============================================================================
