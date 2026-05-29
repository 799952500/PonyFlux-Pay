# Tasks: 对账差异处置工作流升级

**Input**: Design documents from `/specs/013-recon-diff-workflow/`  
**Prerequisites**: `plan.md`（required）, `spec.md`（required）, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

**Organization**: 任务按用户故事分组（US1→US5），每个故事可独立验收。严格遵循 PonyFlux-Pay 模块边界与宪法约束。

## Phase 1: Setup（文档与脚手架对齐）

**Purpose**: 让实现任务具备一致入口与验收口径

- [x] T001 对齐并锁定接口契约文档 `specs/013-recon-diff-workflow/contracts/recon-diff-workflow-api.md`
- [x] T002 对齐并锁定数据模型语义 `specs/013-recon-diff-workflow/data-model.md`
- [x] T003 对齐 quickstart 演示路径并补充必要说明 `specs/013-recon-diff-workflow/quickstart.md`

---

## Phase 2: Foundational（阻断性前置条件）

**Purpose**: 所有用户故事共享的数据库/权限/基础 DTO/聚合骨架。  
**⚠️ CRITICAL**: 本阶段完成前，不开始任何 US 研发。

- [x] T004 创建 Flyway 增量迁移（新表 + 索引 + 约束）`payflow-admin-server/src/main/resources/db/migration/admin/V10__recon_diff_workflow.sql`
- [x] T005 [P] 更新全量 schema DDL（追加 recon_* 新表）`sql/schema/payflow_admin.sql`
- [x] T006 [P] 更新 demo seed（SLA 默认规则 + 示例订阅 + 示例差异/账龄数据）`sql/seed/payflow_admin_seed.sql`

- [x] T007 [P] 新增工单相关 DTO（请求/响应）`payflow-admin-server/src/main/java/com/payflow/admin/dto/recon/ReconDiffWorkItemDTO.java`
- [x] T008 [P] 新增指派/认领/推进/终态请求 DTO `payflow-admin-server/src/main/java/com/payflow/admin/dto/recon/ReconDiffAssignRequest.java`
- [x] T009 [P] 新增 SLA 规则 DTO `payflow-admin-server/src/main/java/com/payflow/admin/dto/recon/ReconDiffSlaRuleDTO.java`
- [x] T010 [P] 新增聚合看板 DTO `payflow-admin-server/src/main/java/com/payflow/admin/dto/recon/ReconDiffAggregationDTO.java`
- [x] T011 [P] 新增报告订阅 DTO `payflow-admin-server/src/main/java/com/payflow/admin/dto/recon/ReconReportSubscriptionDTO.java`

- [x] T012 [P] 新增工单扩展实体 `payflow-admin-server/src/main/java/com/payflow/admin/entity/recon/ReconDiffAssignmentEntity.java`
- [x] T013 [P] 新增 SLA 规则实体 `payflow-admin-server/src/main/java/com/payflow/admin/entity/recon/ReconDiffSlaRuleEntity.java`
- [x] T014 [P] 新增报告订阅实体 `payflow-admin-server/src/main/java/com/payflow/admin/entity/recon/ReconReportSubscriptionEntity.java`
- [x] T015 [P] 新增报告快照实体 `payflow-admin-server/src/main/java/com/payflow/admin/entity/recon/ReconReportSnapshotEntity.java`
- [x] T016 [P]（可选兜底）新增预聚合快照实体 `payflow-admin-server/src/main/java/com/payflow/admin/entity/recon/ReconAggregationSnapshotEntity.java`

- [x] T017 [P] 新增工单扩展 Mapper `payflow-admin-server/src/main/java/com/payflow/admin/mapper/recon/ReconDiffAssignmentEntityMapper.java`
- [x] T018 [P] 新增 SLA 规则 Mapper `payflow-admin-server/src/main/java/com/payflow/admin/mapper/recon/ReconDiffSlaRuleEntityMapper.java`
- [x] T019 [P] 新增订阅 Mapper `payflow-admin-server/src/main/java/com/payflow/admin/mapper/recon/ReconReportSubscriptionEntityMapper.java`
- [x] T020 [P] 新增报告快照 Mapper `payflow-admin-server/src/main/java/com/payflow/admin/mapper/recon/ReconReportSnapshotEntityMapper.java`
- [x] T021 [P]（可选兜底）新增预聚合快照 Mapper `payflow-admin-server/src/main/java/com/payflow/admin/mapper/recon/ReconAggregationSnapshotEntityMapper.java`

- [x] T022 [P] 扩展通知类型枚举（新增 recon 工作流通知类型）`payflow-admin-server/src/main/java/com/payflow/admin/enums/NotificationTypeEnum.java`
- [x] T023 [P] 更新权限/按钮种子（新增 perm：`recon:diff:assign`/`recon:diff:escalate`/`recon:report:subscribe` 等）`payflow-admin-server/src/main/resources/db/migration/admin/V10__recon_diff_workflow.sql`

**Checkpoint**: 新表可建、DTO/Entity/Mapper 编译通过、权限种子具备可演示入口。

---

## Phase 3: User Story 1 - 差异工单化（Priority: P1）🎯 MVP

**Goal**: 差异具备负责人 + 状态机 + 审计留痕；支持自动派单、认领、改派、开始处理、终态处置、留言；通知新负责人。  
**Independent Test**: 见 `specs/013-recon-diff-workflow/spec.md`（US1 Independent Test）。

### Tests for US1（spec 明确要求）

- [x] T024 [P] [US1] 单元测试：工单状态机迁移合法性 `payflow-admin-server/src/test/java/com/payflow/admin/service/recon/ReconDiffWorkflowServiceTest.java`
- [x] T025 [P] [US1] 集成测试：认领/改派/处置链路 + 审计落库 `payflow-admin-server/src/test/java/com/payflow/admin/controller/AdminReconWorkItemIT.java`
- [x] T026 [P] [US1] Playwright：我的工单列表/改派/处置（最小闭环）`payflow-admin-client/e2e/recon-work-items.spec.ts`

### Implementation for US1

- [x] T027 [P] [US1] 新增工单状态枚举（workflow_status）`payflow-admin-server/src/main/java/com/payflow/admin/enums/recon/ReconDiffWorkflowStatusEnum.java`
- [x] T028 [US1] 实现工单编排服务（派单/认领/改派/开始处理/终态/留言）`payflow-admin-server/src/main/java/com/payflow/admin/service/recon/ReconDiffWorkflowService.java`
- [x] T029 [US1] 在对账任务完成后触发“差异自动派单”（不改 recon-server）`payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminReconController.java`
- [x] T030 [US1] 扩展对账 Controller：工单列表/详情/认领/指派/开始/完成/留言 `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminReconController.java`
- [x] T031 [US1] 扩展查询服务：work-items 列表与详情组装（diff+assignment+audits）`payflow-admin-server/src/main/java/com/payflow/admin/service/AdminReconQueryService.java`
- [x] T032 [US1] 审计写入统一封装（ASSIGN/REASSIGN/START_PROGRESS/COMMENT/COMPLETE）`payflow-admin-server/src/main/java/com/payflow/admin/service/recon/ReconAuditService.java`
- [x] T033 [US1] 前端 API：封装 work-items 与动作接口 `payflow-admin-client/src/api/admin.ts`
- [x] T034 [US1] 前端页面：我的差异工单列表 `payflow-admin-client/src/pages/admin/reconcile/work-items.vue`
- [x] T035 [US1] 前端页面：工单详情（含审计时间线、留言、动作按钮）`payflow-admin-client/src/pages/admin/reconcile/work-item-detail.vue`
- [x] T036 [US1] 更新对账菜单路由与入口（若需新增子页）`payflow-admin-client/src/pages/admin/reconcile/index.vue`
- [x] T037 [US1] 更新接口对照表 `docs/CONTRACT_MATRIX.md`

**Checkpoint**: US1 可独立演示：生成差异→自动派单→改派通知→处置终态→审计可追溯。

---

## Phase 4: User Story 2 - SLA 监控与超时自动升级（Priority: P1）

**Goal**: SLA 规则可配置；due-soon 提醒去重；overdue 自动升级并通知角色；支持临时关闭自动升级但保留提醒。  
**Independent Test**: 见 `specs/013-recon-diff-workflow/spec.md`（US2 Independent Test）。

### Tests for US2

- [x] T038 [P] [US2] 单元测试：SLA 计算与 due-soon 阈值计算 `payflow-admin-server/src/test/java/com/payflow/admin/service/recon/ReconSlaServiceTest.java`
- [x] T039 [P] [US2] 集成测试：due-soon 去重 + overdue 升级只触发一次 `payflow-admin-server/src/test/java/com/payflow/admin/task/ReconSlaScanTaskIT.java`
- [x] T040 [P] [US2] Playwright：SLA 规则页配置 + 列表显示 dueAt/overdue `payflow-admin-client/e2e/recon-sla.spec.ts`

### Implementation for US2

- [x] T041 [US2] 实现 SLA 规则管理服务（CRUD + 不溯及既往规则）`payflow-admin-server/src/main/java/com/payflow/admin/service/recon/ReconSlaService.java`
- [x] T042 [US2] 扩展 Controller：SLA 规则查询/保存端点 `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminReconController.java`
- [x] T043 [US2] 新增 SLA 扫描任务（due-soon/overdue）`payflow-admin-server/src/main/java/com/payflow/admin/task/ReconSlaScanTask.java`
- [x] T044 [US2] 将 due_at 写入策略接入工单创建/派单流程 `payflow-admin-server/src/main/java/com/payflow/admin/service/recon/ReconDiffWorkflowService.java`
- [x] T045 [US2] 前端 API：SLA 规则接口封装 `payflow-admin-client/src/api/admin.ts`
- [x] T046 [US2] 前端页面：SLA 规则管理页 `payflow-admin-client/src/pages/admin/reconcile/sla-rules.vue`
- [x] T047 [US2] 更新接口对照表 `docs/CONTRACT_MATRIX.md`

**Checkpoint**: US2 可独立演示：1 分钟 SLA 演示 → 提醒 → 自动升级 → 列表标红。

---

## Phase 5: User Story 3 - 差异自动归因看板（Priority: P2）

**Goal**: 提供矩阵/趋势/TOPN/SLA 统计，并支持下钻到工单列表；满足 30 天≤1 万条差异 P95 ≤2 秒。  
**Independent Test**: 见 `specs/013-recon-diff-workflow/spec.md`（US3 Independent Test）。

### Tests for US3

- [x] T048 [P] [US3] 集成测试：聚合矩阵/趋势口径与 SQL 结果一致 `payflow-admin-server/src/test/java/com/payflow/admin/service/recon/ReconAggregationServiceIT.java`
- [x] T049 [P] [US3] Playwright：看板渲染 + 点击矩阵下钻到列表 `payflow-admin-client/e2e/recon-dashboard.spec.ts`

### Implementation for US3

- [x] T050 [US3] 实现聚合查询服务（矩阵/趋势/TOPN/SLA stats）`payflow-admin-server/src/main/java/com/payflow/admin/service/recon/ReconAggregationService.java`
- [x] T051 [US3] 扩展 Controller：看板聚合端点 `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminReconController.java`
- [x] T052 [US3]（性能兜底）实现预聚合快照任务（每日 02:00）`payflow-admin-server/src/main/java/com/payflow/admin/task/ReconAggregationSnapshotTask.java`
- [x] T053 [US3] 前端 API：看板端点封装 `payflow-admin-client/src/api/admin.ts`
- [x] T054 [US3] 前端页面：差异归因看板（ECharts）`payflow-admin-client/src/pages/admin/reconcile/insights-dashboard.vue`
- [x] T055 [US3] 前端下钻：看板跳转 work-items 并携带过滤器 `payflow-admin-client/src/pages/admin/reconcile/work-items.vue`
- [x] T056 [US3] 更新接口对照表 `docs/CONTRACT_MATRIX.md`

---

## Phase 6: User Story 4 - 长尾差异追踪（Priority: P2）

**Goal**: 账龄 bucket 汇总 + 长尾列表过滤 + 批量挂账（ACCEPTED_LOSS）+ 每日摘要通知。  
**Independent Test**: 见 `specs/013-recon-diff-workflow/spec.md`（US4 Independent Test）。

### Tests for US4

- [x] T057 [P] [US4] 集成测试：bucket 统计与挂账终态行为 `payflow-admin-server/src/test/java/com/payflow/admin/service/recon/ReconLongTailServiceIT.java`
- [x] T058 [P] [US4] Playwright：长尾页 bucket 展示 + 批量挂账 `payflow-admin-client/e2e/recon-long-tail.spec.ts`

### Implementation for US4

- [x] T059 [US4] 实现长尾统计服务（bucket/maxAge）`payflow-admin-server/src/main/java/com/payflow/admin/service/recon/ReconLongTailService.java`
- [x] T060 [US4] 扩展 Controller：长尾 summary 与批量挂账端点 `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminReconController.java`
- [x] T061 [US4] 新增长尾摘要推送任务（09:00）`payflow-admin-server/src/main/java/com/payflow/admin/task/ReconLongTailDigestTask.java`
- [x] T062 [US4] 前端 API：长尾 summary/挂账接口封装 `payflow-admin-client/src/api/admin.ts`
- [x] T063 [US4] 前端页面：长尾追踪页 `payflow-admin-client/src/pages/admin/reconcile/long-tail.vue`
- [x] T064 [US4] 更新接口对照表 `docs/CONTRACT_MATRIX.md`

---

## Phase 7: User Story 5 - 对账日报 / 周报订阅（Priority: P3）

**Goal**: 支持订阅/取消订阅；定时生成快照并推送通知；报告详情页与看板口径一致。  
**Independent Test**: 见 `specs/013-recon-diff-workflow/spec.md`（US5 Independent Test）。

### Tests for US5

- [x] T065 [P] [US5] 集成测试：订阅去重 + 快照生成口径一致 `payflow-admin-server/src/test/java/com/payflow/admin/service/recon/ReconReportServiceIT.java`
- [x] T066 [P] [US5] Playwright：订阅周报 + 收到通知后查看报告详情 `payflow-admin-client/e2e/recon-report-subscription.spec.ts`

### Implementation for US5

- [x] T067 [US5] 实现订阅与快照服务（payload_json 结构化）`payflow-admin-server/src/main/java/com/payflow/admin/service/recon/ReconReportService.java`
- [x] T068 [US5] 扩展 Controller：订阅 CRUD 与报告详情端点 `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminReconController.java`
- [x] T069 [US5] 新增报告调度任务（每日/每周）`payflow-admin-server/src/main/java/com/payflow/admin/task/ReconReportScheduleTask.java`
- [x] T070 [US5] 前端 API：订阅/报告详情接口封装 `payflow-admin-client/src/api/admin.ts`
- [x] T071 [US5] 前端页面：个人报告订阅页 `payflow-admin-client/src/pages/admin/preferences.vue`
- [x] T072 [US5] 前端页面：报告详情页（从通知中心点击进入）`payflow-admin-client/src/pages/admin/reconcile/report-detail.vue`
- [x] T073 [US5] 更新接口对照表 `docs/CONTRACT_MATRIX.md`

---

## Phase 8: Polish & Cross-Cutting Concerns（收尾与门禁）

**Purpose**: 跨 US 的一致性、性能、安全、文档与演示可用性。

- [x] T074 [P] 补齐/校准通知 icon 与默认跳转 link（确保前端展示一致）`payflow-admin-server/src/main/java/com/payflow/admin/enums/NotificationTypeEnum.java`
- [x] T075 统一参数校验与分页上限（size ≤500）`payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminReconController.java`
- [x] T076 统一 merchantScope 过滤覆盖所有新增查询（含聚合与报告）`payflow-admin-server/src/main/java/com/payflow/admin/service/AdminReconQueryService.java`
- [x] T077 性能压测：看板 30 天≤1 万条差异 P95 ≤2s（必要时启用预聚合）`payflow-admin-server/src/main/java/com/payflow/admin/service/recon/ReconAggregationService.java`
- [x] T078 运行 quickstart 验证全流程（联调演示）`specs/013-recon-diff-workflow/quickstart.md`
- [x] T079 按需执行 Playwright E2E 并确保后台日志无阻断错误 `payflow-admin-client/playwright.config.ts`
- [x] T080 更新最终接口矩阵（去重、补齐所有新增端点）`docs/CONTRACT_MATRIX.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**：无依赖
- **Foundational (Phase 2)**：依赖 Setup
- **User Stories (Phase 3+)**：全部依赖 Foundational
- **Polish (Phase 8)**：依赖 US1–US5

### User Story Dependencies

- **US1**：无其他故事依赖（MVP）
- **US2**：依赖 US1（需要已有工单与派单流程承载 due_at/升级）
- **US3**：依赖 US1（需要 work-items 作为下钻目标；聚合口径可复用 assignment/终态）
- **US4**：依赖 US1 + US2（长尾判断依赖终态与（可选）SLA 字段；最小可只依赖 US1）
- **US5**：依赖 US3（报告指标口径与看板一致；报告详情页复用聚合服务）

---

## Parallel Opportunities

- **Foundational 并行**：DTO/Entity/Mapper/枚举/DDL 更新可并行（T005–T022 多为不同文件）。
- **后端/前端并行**：每个 US 内部可以前端页面与后端端点并行推进（以契约文档为准）。
- **E2E 并行**：Playwright 用例可在接口稳定后并行补齐（US1/US2/US4/US5）。

---

## Parallel Example: US1

```bash
# 并行创建（不同文件、无依赖）
Task: "创建 DTO payflow-admin-server/.../dto/recon/ReconDiffWorkItemDTO.java"
Task: "创建实体 payflow-admin-server/.../entity/recon/ReconDiffAssignmentEntity.java"
Task: "创建 Mapper payflow-admin-server/.../mapper/recon/ReconDiffAssignmentEntityMapper.java"

# 并行前端准备
Task: "封装 API payflow-admin-client/src/api/admin.ts"
Task: "工单列表页 payflow-admin-client/src/pages/admin/reconcile/work-items.vue"
```

---

## Implementation Strategy

### MVP First（只做 US1）

1. Phase 1 + Phase 2 完成（DDL/DTO/Entity/Mapper/权限）
2. 完成 US1（工单化闭环）
3. 按 spec 的 Independent Test + Playwright（US1）验证并观测后台日志无阻断错误

### Incremental Delivery

US1 → US2（SLA）→ US3（看板）→ US4（长尾）→ US5（订阅报告），每一步都可独立演示并回归不破坏前序功能。

