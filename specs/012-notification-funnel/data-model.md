# Data Model: 通知中心与支付漏斗真实化

**Feature**: `012-notification-funnel`
**Date**: 2026-05-26

## 新增实体

### admin_notifications（站内通知）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 主键 |
| `recipient_user_id` | BIGINT | NOT NULL | 接收人（`admin_sys_users.id`） |
| `merchant_id` | VARCHAR(64) | NULL | 商户隔离字段；全局通知为空 |
| `biz_type` | VARCHAR(32) | NOT NULL | 通知业务类型枚举 |
| `biz_key` | VARCHAR(128) | NOT NULL | 业务唯一键，用于幂等去重 |
| `title` | VARCHAR(256) | NOT NULL | 通知标题 |
| `summary` | VARCHAR(512) | NULL | 正文摘要 |
| `link` | VARCHAR(512) | NULL | 跳转 URL（相对路径） |
| `read_status` | TINYINT | NOT NULL, DEFAULT 0 | 0=未读, 1=已读 |
| `read_at` | DATETIME | NULL | 标记已读时间 |
| `created_at` | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**:

| 索引名 | 列 | 类型 | 用途 |
|--------|---|------|------|
| `uk_notification_biz` | `(biz_type, biz_key, recipient_user_id)` | UNIQUE | 幂等去重（同类型 + 同业务键 + 同接收人不重复） |
| `idx_notification_recipient` | `(recipient_user_id, read_status, created_at DESC)` | NORMAL | 通知列表查询（按接收人 + 读取状态 + 时间排序） |
| `idx_notification_merchant` | `(merchant_id)` | NORMAL | 商户隔离过滤 |
| `idx_notification_cleanup` | `(read_status, created_at)` | NORMAL | 90 天清理任务 |

**状态流转**: 无状态机，仅 `read_status`: 0 (未读) → 1 (已读)，不可逆。

---

### NotificationType 枚举（Java enum，非数据库表）

| 枚举值 | 说明 | 图标 | 默认跳转 |
|--------|------|------|----------|
| `REFUND_APPROVAL` | 退款待审批 | Warning | `/admin/refunds?status=REFUNDING` |
| `CHURN_OVERDUE` | 流失预警超时 | Clock | `/admin/churn-alerts?status=pending` |
| `EXPORT_COMPLETED` | 导出完成 | Download | `/admin/export/download/{taskId}` |
| `EXPORT_FAILED` | 导出失败 | CloseBold | `/admin/export` |
| `RECON_DIFF` | 对账差异 | DataAnalysis | `/admin/reconcile/tasks/{taskId}/diffs` |
| `WEBHOOK_FAILURE` | 回调失败 | Connection | `/admin/merchant-notifies?merchantId={merchantId}` |
| `SYSTEM_ANNOUNCEMENT` | 系统公告（预留） | Notification | — |

---

## 修改实体

### cashier_orders（仅查询，不修改表结构）

漏斗聚合利用现有字段：

| 字段 | 漏斗用途 |
|------|----------|
| `status` | GROUP BY 统计各阶段数量 |
| `channel` | 渠道筛选 |
| `merchant_id` | 商户筛选 + 隔离 |
| `created_at` | 时间范围过滤 |

漏斗阶段映射（基于当前 status，利用状态单向递进）：

| 漏斗阶段 | 统计口径 | 说明 |
|----------|----------|------|
| CREATED | `COUNT(*)` WHERE created_at BETWEEN ? AND ? | 时间范围内全部创建 |
| PAYING | `COUNT(*)` WHERE status IN ('PAYING','PAID','SUCCESS') | 曾进入支付流程（含已完成） |
| PAID | `COUNT(*)` WHERE status IN ('PAID','SUCCESS') | 支付成功 |
| FAILED | `COUNT(*)` WHERE status = 'FAILED' | 支付失败 |
| CLOSED | `COUNT(*)` WHERE status = 'CLOSED' | 超时关单 |
| EXPIRED | `COUNT(*)` WHERE status = 'EXPIRED' | 过期 |

---

## DTO（非持久化）

### NotificationDTO

```text
id: Long              → 通知 ID
bizType: String       → 业务类型枚举值
title: String         → 标题
summary: String       → 正文摘要
link: String          → 跳转 URL
readStatus: Integer   → 0=未读, 1=已读
createdAt: String     → ISO 8601 时间
```

### FunnelResult

```text
dateFrom: String          → 查询开始日期
dateTo: String            → 查询结束日期
stages:
  - name: "CREATED"
    count: Long           → 创建订单数
    rate: null            → 第一阶段无转化率
  - name: "PAYING"
    count: Long           → 进入支付数
    rate: Double          → PAYING / CREATED * 100
  - name: "PAID"
    count: Long           → 支付成功数
    rate: Double          → PAID / PAYING * 100
overallConversionRate: Double  → PAID / CREATED * 100
lossBreakdown:
  - name: "FAILED"
    count: Long
    percentage: Double    → FAILED / CREATED * 100
  - name: "CLOSED"
    count: Long
    percentage: Double
  - name: "EXPIRED"
    count: Long
    percentage: Double
```

---

## 关系图

```text
admin_sys_users (1) ←—— (N) admin_notifications
                              ↑ biz_key 关联到:
                              ├── cashier_refunds.refund_id
                              ├── admin_churn_alert.id
                              ├── export_task_id (内存)
                              ├── recon_task.task_id
                              └── admin_merchant_webhook_endpoint.id

cashier_orders ——→ FunnelService (只读聚合，不写入)
```
