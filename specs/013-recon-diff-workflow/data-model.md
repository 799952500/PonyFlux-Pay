# Data Model: 对账差异处置工作流升级

**Created**: 2026-05-28  
**Feature**: [spec.md](spec.md)

> 说明：本文件描述“业务实体与字段语义”，用于指导 Phase 2 任务拆解与实现。物理 DDL（字段类型、索引细节）将在实现阶段通过 Flyway 迁移落地，并同步更新 `sql/schema/payflow_admin.sql` 与 `sql/seed/payflow_admin_seed.sql`。

## 1) ReconDiff（已有）

来源：对账引擎比对生成（见 `docs/reconciliation.md`）。本特性不修改其既有字段语义。

关键字段（已存在，摘录）：
- **id**：差异主键（工单扩展表使用 `diff_id` 关联）
- **task_id**：对账任务 ID
- **diff_type**：`CHANNEL_ONLY` / `LOCAL_ONLY` / `AMOUNT_MISMATCH` / `STATUS_MISMATCH`
- **handle_status**：`PENDING` / `PROCESSED` / `IGNORED`（保持兼容；工单终态时同步）
- **merchant_id**（若已存在则复用；若不存在则由关联结果补齐到扩展表）：用于 `merchantScope` 隔离口径
- **amount / channel_amount / local_amount**（视实际表结构）：用于差异金额统计
- **created_at**：差异产生时间（账龄与 SLA 起点）

## 2) ReconDiffAssignment（新增，一对一扩展）

目的：将差异升级为“工单”，承载责任人与过程状态。

关系：
- 与 `recon_diff`：**1:1**（`recon_diff_assignment.diff_id` 唯一）

关键字段：
- **id**：自增主键
- **diff_id**：关联 `recon_diff.id`（唯一）
- **merchant_id**：用于强制隔离过滤（优先直接存储，避免跨表取值导致口径不稳）
- **assignee_id**：当前负责人（管理员用户 ID / 用户名，按现有系统用户体系决定）
- **assigned_at**：指派时间
- **workflow_status**：工单状态机
  - `UNASSIGNED`：未指派池
  - `ASSIGNED`：已指派待处理
  - `IN_PROGRESS`：处理中
  - `ESCALATED`：已升级（仍可继续处理）
  - `PROCESSED`：已处理终态
  - `IGNORED`：已忽略终态
  - `ACCEPTED_LOSS`：挂账终态
- **due_at**：SLA 截止时间（可空）
- **escalated_at**：升级时间（可空）
- **escalated_to_role**：升级目标角色（默认 `recon:manage`）
- **last_reminded_at**：最近一次“临近超时提醒”发送时间（用于去重）
- **processed_at**：进入终态时间（可空）
- **created_at / updated_at**：审计字段

约束/规则：
- `diff_id` 唯一；同一差异不能对应多个工单。
- 终态（`PROCESSED/IGNORED/ACCEPTED_LOSS`）后禁止再次变更状态（除 `RISK` / `SUPER_ADMIN` 允许回滚的管理动作）。
- 所有状态变更必须写入 `recon_handler_audit`。

## 3) ReconDiffSlaRule（新增）

目的：按差异类型配置 SLA 与升级策略。

关键字段：
- **id**：自增主键
- **diff_type**：差异类型（唯一）
- **enabled**：是否启用 SLA（禁用时 `due_at` 为空，不参与升级）
- **sla_hours**：SLA 时长（小时）
- **due_soon_ratio**：临近超时提醒阈值（默认 0.2）
- **escalate_to_role**：升级角色（默认 `recon:manage`）
- **updated_by / updated_at**

默认值（用于 seed）：
- `AMOUNT_MISMATCH = 24h`
- `STATUS_MISMATCH = 12h`
- `CHANNEL_ONLY = 8h`
- `LOCAL_ONLY = 8h`

## 4) ReconAggregationSnapshot（新增，可选兜底）

目的：当看板在线聚合性能不足时按日预聚合。

关键字段：
- **id**：自增主键
- **stat_date**：统计日期（按差异创建日期）
- **merchant_id**：统计口径隔离
- **channel**：渠道
- **diff_type**：差异类型
- **diff_count**：差异笔数
- **diff_amount**：差异金额（按业务口径汇总）
- **processed_count / ignored_count / accepted_loss_count**：处置分布（可选）
- **sla_met_count / sla_total_count**：SLA 达成口径（可选）

说明：
- 若引入该表，必须保证“报告快照/看板”共用同一聚合服务，避免双口径漂移。

## 5) ReconReportSubscription（新增）

目的：支持用户订阅日报/周报。

关键字段：
- **id**：自增主键
- **subscriber_id**：订阅者（管理员用户）
- **report_type**：`DAILY` / `WEEKLY`
- **scope**：`OWNED`（我负责的）/ `ALL_AUTHORIZED`（授权范围全部）
- **enabled**
- **last_sent_at**：去重/追踪
- **created_at / updated_at**

约束：
- 同一用户 + report_type 唯一（避免重复订阅）。

## 6) ReconReportSnapshot（新增）

目的：每次推送生成“快照”，保证可追溯与口径一致。

关键字段：
- **id**：自增主键
- **snapshot_id**：业务唯一标识（用于通知 `bizKey` 去重）
- **subscriber_id**
- **report_type**
- **period_start / period_end**
- **payload_json**：报告内容（结构化，供前端渲染）
- **generated_at**

说明：
- `payload_json` 建议包含：新增差异、处置分布、SLA 达成率、长尾指标、TOP3 渠道、TOP3 商户、以及“口径说明”。

## 7) ReconHandlerAudit（已有，需扩展动作枚举口径）

目的：所有关键动作留痕，满足合规审计。

需要覆盖的 action（口径扩展）：
- `ASSIGN` / `REASSIGN`
- `START_PROGRESS`
- `ESCALATE`
- `COMMENT`
- `PROCESSED` / `IGNORED` / `ACCEPT_LOSS`
- `ROLLBACK`

