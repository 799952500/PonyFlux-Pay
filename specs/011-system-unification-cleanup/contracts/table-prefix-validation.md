# Contract: 表前缀校验

**Feature**: `011-system-unification-cleanup`

## 用途

安装或重构后，验证两库表名符合前缀策略（SC-001）。

## 校验 SQL

```sql
-- 应返回 0 行
SELECT 'admin_violation' AS kind, TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'payflow_admin'
  AND TABLE_TYPE = 'BASE TABLE'
  AND TABLE_NAME NOT REGEXP '^(admin_|recon_)';

-- 应返回 0 行
SELECT 'cashier_violation' AS kind, TABLE_NAME
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'payflow_cashier'
  AND TABLE_TYPE = 'BASE TABLE'
  AND TABLE_NAME NOT LIKE 'cashier_%';
```

## 应用代码校验

以下旧表名在 `*.java`、`*.vue`、`*.ts`（排除 `sql/migrations/` 归档）中应为 **0 命中**：

`sys_users`, `sys_roles`, `sys_menus`, `sys_role_menus`, `sys_user_roles`, `risk_rules`, `merchant_application`, `merchant_contract`, `merchant_open_app`, `payment_link`, `cashier_risk_blacklist`, `merchant_webhook_endpoint`, `webhook_delivery_log`（作为裸表名引用时）

## 通过标准

- 两条 SQL 校验均为 0 行
- `mvn -B -DskipTests compile` 成功
- 三服务启动 + quickstart 冒烟通过
