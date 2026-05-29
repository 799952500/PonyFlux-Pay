# Phase 0 Research: 对账差异处置工作流升级

**Created**: 2026-05-28  
**Feature**: [spec.md](spec.md)  
**Plan**: [plan.md](plan.md)

本阶段目标：将技术上下文中的潜在不确定点收敛为明确决策，并给出取舍理由与备选方案对比，确保 Phase 1 设计可以直接落地。

## Decision 1: 工单扩展采用“一对一扩展表”，不改动 `recon_diff` 既有结构

- **Decision**: 新增 `recon_diff_assignment`（以 `diff_id` 唯一约束与 `recon_diff.id` 一对一关联），承载 `assignee_id/workflow_status/due_at/escalated_*` 等字段；`recon_diff.handle_status` 保持兼容，但在工单化后由服务层进行口径映射（例如 `workflow_status=PROCESSED` 时同步 `handle_status=PROCESSED`）。
- **Rationale**:
  - 既有对账页面与接口依赖 `recon_diff`，直接改表容易引入兼容性风险；
  - 扩展表便于逐步灰度上线与回滚（可先只写扩展表，不影响旧逻辑）。
- **Alternatives considered**:
  - 直接在 `recon_diff` 增列：实现简单但影响面大、回滚成本高；
  - 新建“工单主表”并复制差异字段：会产生双写与口径漂移风险。

## Decision 2: 自动派单策略 = “轮询 + 商户范围匹配”，不引入工作量均衡/专长模型

- **Decision**: 派单候选人为具备 `recon:diff:handle` 权限且账号启用的用户；在候选人集合中按轮询分配；当差异携带 `merchant_id` 时，要求候选人可见该商户（满足 `merchantScope`）后才可被分配。
- **Rationale**:
  - 轮询易解释、可预测，且与现有 RBAC/商户隔离机制兼容；
  - 工作量均衡依赖实时指标（待处理量、处理时长）与更多埋点，不适合作为“升级现有功能”的首期交付。
- **Alternatives considered**:
  - “按当前待办最少”均衡：需要额外统计与并发控制；
  - “按差异类型专长”分配：需要人员标签体系（扩展范畴）。

## Decision 3: SLA 规则只对“新生成差异”生效；存量差异采用一次性初始化

- **Decision**:
  - `recon_diff_sla_rule` 的变更不回写历史工单 `due_at`；
  - 上线时对存量 PENDING 差异做一次初始化：写入扩展表、计算 `due_at`；为避免大量噪音，第一周仅开启“临近超时提醒”，第二周开启“自动升级”。
- **Rationale**:
  - “规则回溯”会导致历史工单 due_at 抖动，产生不可预期的升级风暴；
  - 首周仅提醒可让团队适应新流程，降低上线冲击。
- **Alternatives considered**:
  - 规则变更全量回溯：实现复杂且可解释性差；
  - 上线立刻自动升级：短期噪音大，影响信任。

## Decision 4: 超时升级对象 = 角色（默认 `recon:manage`），而非单个用户

- **Decision**: 升级通知发送给 `recon:manage` 角色全员；升级后工单仍保留原负责人，同时设置 `escalated_to_role` 便于后续统计与权限控制。
- **Rationale**:
  - 组织结构与值班安排会变，角色更稳定；
  - 角色广播减少单点风险（某个主管离线）。
- **Alternatives considered**:
  - 固定升级到某个用户：组织变动成本高、容易出现无人接收；
  - 逐级上报链：需要组织层级数据（扩展范畴）。

## Decision 5: 通知复用 012 通知中心，新增 `NotificationTypeEnum` 枚举值

- **Decision**: 在 `NotificationTypeEnum` 中新增 `RECON_DIFF_ASSIGNED/RECON_DIFF_DUE_SOON/RECON_DIFF_OVERDUE/RECON_DIFF_LONG_TAIL/RECON_DIFF_HIGH_VALUE/RECON_REPORT/RECON_DIFF_RECYCLED`；均走现有 `NotificationService` 的幂等 `bizKey` 机制。
- **Rationale**: 站内通知已是现成闭环入口；扩展枚举最小改动即可贯穿前端展示（icon/link）。
- **Alternatives considered**:
  - 复用单一 `RECON_DIFF` 类型并在内容中区分：前端筛选/统计困难；
  - 引入 MQ/邮件：属于扩展，不符合本期边界。

## Decision 6: 看板聚合优先“在线聚合”，必要时增加“按日预聚合快照”

- **Decision**:
  - 默认按过滤条件在线 `GROUP BY` 聚合；
  - 当范围扩大或数据量超过阈值（例如 > 1 万条差异）时，引入 `recon_aggregation_snapshot`（按日×渠道×类型×商户的粒度）加速。
- **Rationale**:
  - 现阶段 demo 规模可在线聚合；
  - 预聚合可作为性能兜底，不强制一开始就引入复杂链路。
- **Alternatives considered**:
  - 全量上来就做预聚合：迁移成本和理解成本更高；
  - 引入专门 OLAP：明显扩展。

