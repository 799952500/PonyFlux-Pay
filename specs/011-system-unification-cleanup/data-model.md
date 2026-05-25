# Data Model: 表前缀统一与命名映射

**Feature**: `011-system-unification-cleanup` | **Date**: 2026-05-23

本文档定义表名映射（旧 → 新 → 库）及前缀校验规则。实施时以本表为验收清单。

## 前缀校验规则

```sql
-- payflow_admin: 允许 admin_ 与 recon_
SELECT TABLE_NAME FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'payflow_admin'
  AND TABLE_TYPE = 'BASE TABLE'
  AND TABLE_NAME NOT REGEXP '^(admin_|recon_)';

-- payflow_cashier: 仅允许 cashier_
SELECT TABLE_NAME FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'payflow_cashier'
  AND TABLE_TYPE = 'BASE TABLE'
  AND TABLE_NAME NOT LIKE 'cashier_%';
```

预期结果：两查询均 **0 行**。

## payflow_admin 表名映射

| 旧表名 | 新表名 | 备注 |
|--------|--------|------|
| `sys_roles` | `admin_sys_roles` | RBAC |
| `sys_menus` | `admin_sys_menus` | RBAC |
| `sys_role_menus` | `admin_sys_role_menus` | RBAC |
| `sys_users` | `admin_sys_users` | RBAC 登录用户 |
| `sys_user_roles` | `admin_sys_user_roles` | RBAC |
| `risk_rules` | `admin_risk_rules` | 风控规则 |
| `cashier_risk_blacklist` | `admin_risk_blacklist` | 库归属 admin，纠正前缀 |
| `merchant_application` | `admin_merchant_application` | 进件 |
| `merchant_contract` | `admin_merchant_contract` | 进件 |
| `merchant_open_app` | `admin_merchant_open_app` | 进件 |
| `merchant_webhook_endpoint` | `admin_merchant_webhook_endpoint` | 配置域；**删除 cashier 库副本** |
| `webhook_delivery_log` | — | **从 admin 库删除**（权威在 cashier） |
| `payment_link` | `admin_payment_link` | 支付链接 |
| `recon_routing_decision_log` | `admin_routing_decision_log` | 非对账语义，改 admin_ |

**已合规（保持不变）**: `admin_*`、`recon_*` 全系表。

## payflow_cashier 表名映射

| 旧表名 | 新表名 | 备注 |
|--------|--------|------|
| `merchant_webhook_endpoint` | — | **从 cashier 库删除**（权威在 admin） |
| `webhook_delivery_log` | `cashier_webhook_delivery_log` | 事务投递日志 |

**已合规**: `cashier_merchants`、`cashier_orders`、`cashier_payments`、`cashier_refunds`、`cashier_merchant_notify` 等。

## 实体 @TableName 与 schema 对齐（代码层）

以下实体当前 `@TableName` 与 `sql/schema` 不一致，须同步为新表名：

| 实体类（示意） | 当前 @TableName | 目标 @TableName |
|----------------|-----------------|-----------------|
| `Merchant` | `merchants` | `admin_merchants` |
| `Channel` | `channels` | `admin_channels` |
| `PaymentMethod` | `payment_methods` | `admin_payment_methods` |
| `MerchantPaymentMethod` | `merchant_payment_methods` | `admin_merchant_payment_methods` |
| `PaymentAccount` | `payment_accounts` | `admin_payment_accounts` |
| `MerchantPaymentRoute` | `merchant_payment_routes` | `admin_merchant_payment_routes` |
| `Sys*` 系列 | `sys_*` | `admin_sys_*` |
| `RiskRule` | `risk_rules` | `admin_risk_rules` |
| `MerchantWebhookEndpoint` (cashier) | `merchant_webhook_endpoint` | 迁至 admin 模块 → `admin_merchant_webhook_endpoint` |
| `WebhookDeliveryLog` (cashier) | `webhook_delivery_log` | `cashier_webhook_delivery_log` |

> 完整清单在实施阶段用 `rg '@TableName'` 生成并勾选。

## 演示数据（seed）

| 文件 | 职责 |
|------|------|
| `sql/seed/payflow_admin_seed.sql` | admin + recon + admin_sys RBAC + 运营演示 |
| `sql/seed/payflow_cashier_seed.sql` | 订单/支付/退款/回调样本 |
| `sql/seed/payflow_cashier_merchant_notify_demo.sql` | 可合并入主 cashier seed（可选） |

原 `install_demo_db.py` 引用的 7 个 `sql/migrations/*.sql` 内容须**内联**进上述 schema/seed，不再作为安装步骤。

## 关系与约束（不变）

- 外键、索引、逻辑删除、`@Version` 字段语义不因改名改变。
- 跨库禁止 JOIN（宪法 III）；admin 读 cashier 仍通过 cashier 数据源 Mapper。
