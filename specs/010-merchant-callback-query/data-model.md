# Data Model: 商户回调记录查询

## 存储归属

- **数据库**: `payflow_cashier`（交易库）
- **表前缀**: `cashier_`（宪法 III）
- **与既有表关系**: 关联 `cashier_orders.order_id`；不修改 `cashier_callback_logs`（渠道入站）

---

## 商户回调汇总（cashier_merchant_notify）

**Purpose**: 一笔订单在某一回调类型下的整体通知状态（汇总视图）。

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK AI | 主键 |
| `notify_id` | VARCHAR(64) UK | 业务主键，如 `MN202605220001` |
| `order_id` | VARCHAR(64) | 平台订单号 |
| `merchant_id` | VARCHAR(64) | 商户号 |
| `merchant_order_no` | VARCHAR(128) | 商户订单号（冗余便于检索） |
| `notify_type` | VARCHAR(16) | `PAYMENT` / `REFUND` |
| `notify_url` | VARCHAR(512) | 目标回调地址快照 |
| `summary_status` | VARCHAR(16) | 见状态机 |
| `attempt_count` | INT | 累计尝试次数 |
| `last_attempt_at` | DATETIME | 最近一次尝试时间 |
| `last_fail_reason` | VARCHAR(256) | 最近失败原因摘要 |
| `last_response_preview` | VARCHAR(512) | 最近响应预览（截断） |
| `order_status_snapshot` | VARCHAR(16) | 写入时订单状态快照 |
| `notify_payload_status` | VARCHAR(16) | 通知报文中的业务状态（如 ext1） |
| `created_at` | DATETIME | 创建时间 |
| `updated_at` | DATETIME | 更新时间 |

**唯一约束**: `uk_order_notify_type` (`order_id`, `notify_type`)

**索引**:
- `idx_merchant_id` (`merchant_id`)
- `idx_summary_status` (`summary_status`)
- `idx_last_attempt_at` (`last_attempt_at`)
- `idx_merchant_order_no` (`merchant_id`, `merchant_order_no`)

### 汇总状态机（summary_status）

```text
NOT_CONFIGURED  → 未配置 merchant_notify_url，未发起 HTTP
PENDING         → 已入队/待首次投递（可选，MQ 发送后可置位）
IN_PROGRESS     → 已发出 HTTP，尚未得到成功判定
SUCCESS         → 至少一次尝试被判定成功且为最终成功
FAILED          → 已达最大重试仍失败，或明确失败且无待重试
```

**状态流转规则**:
- 创建汇总：MQ 触发或首次进入 Worker 时 upsert。
- 无回调地址 → `NOT_CONFIGURED`，`attempt_count=0`。
- 每次 HTTP 前 → `IN_PROGRESS`；成功后 → `SUCCESS`；失败且将重试 → 保持 `IN_PROGRESS` 并递增 `attempt_count`；失败且无重试 → `FAILED`。
- `attempt_count` 与明细表记录数一致（每次 HTTP 一条明细）。

---

## 商户回调明细（cashier_merchant_notify_attempt）

**Purpose**: 单次向商户发起的 HTTP 通知尝试记录。

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK AI | 主键 |
| `notify_id` | VARCHAR(64) | 关联汇总 `notify_id` |
| `attempt_no` | INT | 尝试序号，从 1 递增 |
| `request_params` | TEXT | 请求参数 JSON |
| `response_body` | TEXT | 商户响应原文 |
| `http_status` | INT | HTTP 状态码（可空，异常时为空） |
| `result_status` | VARCHAR(16) | `SUCCESS` / `FAILED` / `IN_PROGRESS` |
| `fail_reason_type` | VARCHAR(32) | `TIMEOUT` / `HTTP_ERROR` / `RESPONSE_NOT_SUCCESS` / `SIGN_SKIPPED` / `UNKNOWN` |
| `fail_reason_detail` | VARCHAR(512) | 失败详情 |
| `duration_ms` | INT | 耗时毫秒 |
| `truncated` | TINYINT(1) | 报文是否被截断 |
| `created_at` | DATETIME | 尝试时间 |

**唯一约束**: `uk_notify_attempt` (`notify_id`, `attempt_no`)

**索引**: `idx_notify_id` (`notify_id`)

---

## 回调类型（notify_type）

| 值 | 判定条件 | 说明 |
|----|----------|------|
| `PAYMENT` | `MqMessage.refundId == null` | 支付结果通知 |
| `REFUND` | `MqMessage.refundId != null` | 退款结果通知 |

---

## 实体关系

```text
cashier_orders (1) ──< (N) cashier_merchant_notify   [按 notify_type 最多 2 条常见]
cashier_merchant_notify (1) ──< (N) cashier_merchant_notify_attempt
```

---

## 校验规则

- `merchant_id` 必须与 `cashier_orders.merchant_id` 一致。
- 明细 `attempt_no` 必须连续递增，与汇总 `attempt_count` 终态一致。
- 查询必须带商户授权范围（`AdminRequestContext.merchantScope`）。
- 跨商户访问：返回 403 或业务拒绝码，不暴露目标是否存在。
- 请求/响应 JSON 应用层最大 32KB，超出设置 `truncated=1`。

---

## Admin 侧读模型（无新表）

- Entity/Mapper 置于 `payflow-admin-server` 的 `entity.cashier` 包（与 `Order` 相同）。
- 列表 DTO 额外 join 或二次查询 `cashier_orders.status` 用于 FR-013 对比展示。

---

## 种子与迁移

- DDL: `sql/schema/payflow_cashier.sql` 增量 + `sql/migrations/2026-05-22_merchant_notify_tables.sql`
- Seed: `sql/seed/payflow_cashier_seed.sql` 为演示订单补充 1～2 条汇总及多条明细
- 菜单: `sql/seed/payflow_admin_seed.sql` 增加菜单与角色权限
