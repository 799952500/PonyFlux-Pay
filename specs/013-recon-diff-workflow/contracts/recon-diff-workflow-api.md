# API Contract: 对账差异处置工作流（admin-server）

**Created**: 2026-05-28  
**Feature**: [../spec.md](../spec.md)

约定：
- baseURL：`/api/v1`
- 返回结构：`{ code: number, message: string, data: any }`
- 认证：沿用 admin-server JWT（`Authorization: Bearer <token>`）
- 数据隔离：所有查询必须应用 `merchantScope`
- 分页：`page` 从 1 开始；`size` 默认 20，最大 500

## 1) 工单列表与详情

### GET `/admin/reconcile/diffs/work-items`

**用途**：差异工单列表（我的/全部/未指派/已升级/长尾等通过过滤器组合实现）

**Query**：
- `billDate`（可选，YYYY-MM-DD）
- `channel`（可选）
- `diffType`（可选，枚举）
- `workflowStatus`（可选，枚举）
- `onlyMine`（可选，boolean）
- `onlyUnassigned`（可选，boolean）
- `onlyOverdue`（可选，boolean）
- `ageBucket`（可选，`LT_1D`/`D1_3`/`D3_7`/`D7_30`/`GT_30`）
- `page` / `size`

**Data**：
- `list`: WorkItem[]
- `total`: number
- `page`: number
- `size`: number

WorkItem（字段示意）：
- `diffId`
- `taskId`
- `merchantId`
- `channel`
- `diffType`
- `handleStatus`
- `workflowStatus`
- `assigneeId`
- `dueAt`
- `escalatedAt`
- `createdAt`
- `amountSummary`（用于列表展示的摘要字段）

### GET `/admin/reconcile/diffs/{diffId}`

**用途**：工单详情（差异原始信息 + 工单扩展字段 + 审计记录）

**Data**：
- `diff`: ReconDiff
- `assignment`: ReconDiffAssignment
- `audits`: ReconHandlerAudit[]

## 2) 工单动作（指派/认领/推进/终态）

### POST `/admin/reconcile/diffs/{diffId}/claim`

**权限**：`recon:diff:assign`

**用途**：认领未指派工单

**Body**：空

### POST `/admin/reconcile/diffs/{diffId}/assign`

**权限**：`recon:diff:assign`

**Body**：
- `assigneeId`（必填）
- `remark`（可选）

### POST `/admin/reconcile/diffs/{diffId}/start`

**权限**：`recon:diff:handle`

**Body**：
- `remark`（可选）

### POST `/admin/reconcile/diffs/{diffId}/complete`

**权限**：`recon:diff:handle`

**Body**：
- `action`（必填，`PROCESSED`/`IGNORED`/`ACCEPTED_LOSS`）
- `remark`（必填：PROCESSED/IGNORED ≥ 10 字；ACCEPTED_LOSS ≥ 20 字）

> 兼容：可继续保留/复用现有 `POST /admin/reconcile/diffs/{id}/handle`，但新前端页面优先使用本组新接口，以承载工单状态机语义。

### POST `/admin/reconcile/diffs/{diffId}/comment`

**权限**：`recon:diff:handle`

**Body**：
- `content`（必填，≥ 5 字）

## 3) SLA 规则

### GET `/admin/reconcile/sla-rules`

**用途**：查询 SLA 规则列表

### PUT `/admin/reconcile/sla-rules/{diffType}`

**权限**：`recon:manage`

**Body**：
- `enabled`（必填）
- `slaHours`（enabled=true 时必填）
- `escalateToRole`（可选，默认 `recon:manage`）
- `dueSoonRatio`（可选，默认 0.2）

## 4) 归因看板（聚合）

### GET `/admin/reconcile/aggregation/dashboard`

**Query**：
- `dateFrom`（必填）
- `dateTo`（必填）
- `groupBy`（可选，`DAY`/`WEEK`，默认 DAY）
- `channel`（可选）
- `diffType`（可选）

**Data**：
- `matrix`: Array<{ channel, diffType, diffCount, diffAmount }>
- `trend`: Array<{ period, diffCount, diffAmount }>
- `topMerchants`: Array<{ merchantId, diffCount, diffAmount }>
- `topAccounts`: Array<{ accountCode, diffCount, diffAmount }>
- `slaStats`: { avgHandleMinutes, slaMetRate, longTailRate, sample }

## 5) 长尾追踪

### GET `/admin/reconcile/long-tail/summary`

**Query**：
- `asOf`（可选，默认 today）

**Data**：
- `buckets`: Array<{ ageBucket, diffCount, diffAmount }>
- `maxAgeDays`

### POST `/admin/reconcile/long-tail/accept-loss`

**权限**：`recon:manage`

**Body**：
- `diffIds`（必填，1..50）
- `remark`（必填，≥ 20 字）

## 6) 报告订阅与快照

### GET `/admin/reconcile/subscriptions`

**权限**：`recon:report:subscribe`

### POST `/admin/reconcile/subscriptions`

**权限**：`recon:report:subscribe`

**Body**：
- `reportType`（`DAILY`/`WEEKLY`）
- `scope`（`OWNED`/`ALL_AUTHORIZED`）
- `enabled`（默认 true）

### DELETE `/admin/reconcile/subscriptions/{id}`

**权限**：`recon:report:subscribe`

### GET `/admin/reconcile/reports/{snapshotId}`

**权限**：`recon:report:subscribe`（且必须为订阅者本人或 `recon:manage`）

**Data**：
- `snapshotId`
- `reportType`
- `periodStart` / `periodEnd`
- `payload`（结构与看板口径一致）

