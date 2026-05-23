# Tasks: 商户回调记录查询

**Input**: Design documents from `/specs/010-merchant-callback-query/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/api-contracts.md, quickstart.md

**Tests**: 规格与宪法要求覆盖写库单测、查询隔离测试、quickstart 走查；UI 变更后按需 Playwright。包含测试任务。

**Organization**: 按用户故事分组；写库在 Foundational，US1/US2 可先后端再前端，US3 依赖查询 API。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行（不同文件、无未完成依赖）
- **[Story]**: US1–US4 对应 spec.md 用户故事

---

## Phase 1: Setup（共享基础设施）

**Purpose**: DDL、种子数据、契约矩阵基线。

- [X] T001 创建商户回调表增量迁移 `sql/migrations/2026-05-22_merchant_notify_tables.sql`（`cashier_merchant_notify`、`cashier_merchant_notify_attempt`）
- [X] T002 更新全量 DDL `sql/schema/payflow_cashier.sql` 同步上述两张表
- [X] T003 [P] 在 `sql/seed/payflow_cashier_seed.sql` 为演示订单插入回调汇总与多条明细（含 FAILED 重试样例）
- [X] T004 [P] 在 `sql/seed/payflow_admin_seed.sql` 新增菜单 `merchant_notifies`、角色菜单权限（FINANCE/ADMIN）
- [X] T005 [P] 在 `docs/CONTRACT_MATRIX.md` 预留商户回调查询 API 与路由条目

---

## Phase 2: Foundational（阻断性前置条件）

**Purpose**: 实体、Mapper、写库服务；所有用户故事依赖本阶段。

**CRITICAL**: 完成前不得开始用户故事。

- [X] T006 [P] 创建汇总实体 `payflow-cashier-server/src/main/java/com/payflow/cashier/entity/MerchantNotify.java`
- [X] T007 [P] 创建明细实体 `payflow-cashier-server/src/main/java/com/payflow/cashier/entity/MerchantNotifyAttempt.java`
- [X] T008 [P] 创建汇总 Mapper `payflow-cashier-server/src/main/java/com/payflow/cashier/mapper/MerchantNotifyMapper.java`
- [X] T009 [P] 创建明细 Mapper `payflow-cashier-server/src/main/java/com/payflow/cashier/mapper/MerchantNotifyAttemptMapper.java`
- [X] T010 [P] 创建 admin 读模型汇总实体 `payflow-admin-server/src/main/java/com/payflow/admin/entity/cashier/MerchantNotify.java`
- [X] T011 [P] 创建 admin 读模型明细实体 `payflow-admin-server/src/main/java/com/payflow/admin/entity/cashier/MerchantNotifyAttempt.java`
- [X] T012 [P] 创建 admin 汇总 Mapper `payflow-admin-server/src/main/java/com/payflow/admin/mapper/cashier/MerchantNotifyMapper.java`
- [X] T013 [P] 创建 admin 明细 Mapper `payflow-admin-server/src/main/java/com/payflow/admin/mapper/cashier/MerchantNotifyAttemptMapper.java`
- [X] T014 在 cashier 数据源配置中注册新 Mapper Bean `payflow-admin-server/src/main/java/com/payflow/admin/config/CashierDataSourceConfig.java`（若项目使用该配置类）
- [X] T015 实现写库服务接口 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/MerchantNotifyRecordService.java`
- [X] T016 实现写库服务（upsert 汇总、插入明细、状态机、32KB 截断、NOT_CONFIGURED）`payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/MerchantNotifyRecordServiceImpl.java`
- [X] T017 在 `payflow-cashier-server/src/main/java/com/payflow/cashier/consumer/MerchantNotifyWorker.java` 集成写库（HTTP 前后记录、notify_type 判定、失败原因分类）
- [X] T018 [P] 编写写库服务单元测试 `payflow-cashier-server/src/test/java/com/payflow/cashier/service/MerchantNotifyRecordServiceTest.java`

**Checkpoint**: 商户回调投递可持久化；可用 SQL 或单测验证汇总/明细写入。

---

## Phase 3: User Story 1 - 按订单排查商户回调是否成功 (Priority: P1) MVP

**Goal**: 通过平台订单号或商户订单号查询回调汇总，展示类型、地址、汇总状态、最近尝试时间与累计次数，并对比订单状态。

**Independent Test**: 演示订单支付后，调用 `GET /api/v1/admin/merchant-notifies/by-order/{orderId}` 或带 `orderId`/`merchantOrderNo` 的列表接口，返回正确汇总；未配置回调地址时为 `NOT_CONFIGURED`。

### Tests for User Story 1

- [X] T019 [P] [US1] 编写按订单查询集成测试 `payflow-admin-server/src/test/java/com/payflow/admin/service/MerchantNotifyQueryServiceTest.java`
- [X] T020 [P] [US1] 编写商户隔离测试（跨商户 orderId 拒绝）`payflow-admin-server/src/test/java/com/payflow/admin/service/MerchantNotifyMerchantIsolationTest.java`

### Implementation for User Story 1

- [X] T021 [US1] 实现查询服务 `payflow-admin-server/src/main/java/com/payflow/admin/service/MerchantNotifyQueryService.java`
- [X] T022 [US1] 实现查询服务实现类（分页、by-order、merchantScope、join 订单状态）`payflow-admin-server/src/main/java/com/payflow/admin/service/impl/MerchantNotifyQueryServiceImpl.java`
- [X] T023 [US1] 实现控制器（`GET /merchant-notifies` 支持 orderId/merchantOrderNo；`GET /merchant-notifies/by-order/{orderId}`）`payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminMerchantNotifyController.java`
- [X] T024 [US1] 更新契约矩阵 US1 接口说明 `docs/CONTRACT_MATRIX.md`

**Checkpoint**: 仅通过 API（curl/Postman）即可完成 US1 验收，无需列表页。

---

## Phase 4: User Story 2 - 查看每次回调尝试的出入参 (Priority: P1)

**Goal**: 详情展示全部尝试明细，含请求/响应、结果、耗时；敏感字段脱敏。

**Independent Test**: `GET /api/v1/admin/merchant-notifies/{notifyId}` 返回 `summary` + 有序 `attempts`；`sign` 为掩码；条数与 `attempt_count` 一致。

### Tests for User Story 2

- [X] T025 [P] [US2] 编写脱敏工具单元测试 `payflow-admin-server/src/test/java/com/payflow/admin/kit/MerchantNotifyMaskKitTest.java`

### Implementation for User Story 2

- [X] T026 [P] [US2] 实现请求/响应脱敏工具 `payflow-admin-server/src/main/java/com/payflow/admin/kit/MerchantNotifyMaskKit.java`
- [X] T027 [US2] 在 `MerchantNotifyQueryServiceImpl.java` 实现 `getDetail(notifyId, scope)` 并应用脱敏
- [X] T028 [US2] 在 `AdminMerchantNotifyController.java` 增加 `GET /{notifyId}` 详情端点
- [X] T029 [US2] 创建详情抽屉组件 `payflow-admin-client/src/components/merchant-notifies/DetailDrawer.vue`（汇总 + 明细时间线、脱敏展示、truncated 提示）
- [X] T030 [US2] 创建 API 模块 `payflow-admin-client/src/api/merchantNotify.ts`（`getDetail`、`getByOrder`）
- [X] T031 [US2] 更新 `docs/CONTRACT_MATRIX.md` 详情接口与响应字段

**Checkpoint**: 可通过临时页面或 Storybook 式挂载 DetailDrawer + notifyId 完成 US2；列表页非必须。

---

## Phase 5: User Story 3 - 在后台菜单中检索与筛选回调记录 (Priority: P2)

**Goal**: 「交易与订单 → 商户回调记录」列表页，支持商户/时间/状态/类型筛选，点击进入详情。

**Independent Test**: 访问 `/admin/merchant-notifies`，筛选 `summaryStatus=FAILED`，分页正确，点击行打开 T029 详情抽屉。

### Implementation for User Story 3

- [X] T032 [US3] 完善 `MerchantNotifyQueryServiceImpl.java` 列表筛选（merchantId、notifyType、summaryStatus、startTime/endTime、size≤100）
- [X] T033 [US3] 创建列表页 `payflow-admin-client/src/pages/admin/merchant-notifies/index.vue`（`data-table` + stripe、筛选表单、分页）
- [X] T034 [US3] 在 `payflow-admin-client/src/api/merchantNotify.ts` 增加 `listMerchantNotifies`
- [X] T035 [US3] 注册路由 `payflow-admin-client/src/router/index.ts`（`merchant-notifies`）
- [X] T036 [US3] 商户管理员自动限定 `merchantId` 筛选（读取 `stores/admin.ts` 授权范围）
- [X] T037 [US3] 列表行操作打开 `DetailDrawer.vue` 并传入 `notifyId`
- [X] T038 [US3] 更新 `docs/CONTRACT_MATRIX.md` 列表接口与前端路由

**Checkpoint**: US1+US2+US3 形成完整后台排障闭环。

---

## Phase 6: User Story 4 - 从订单上下文跳转查看回调 (Priority: P3)

**Goal**: 订单详情一键查看该订单商户回调，减少复制订单号。

**Independent Test**: 在订单详情点击「查看商户回调」，打开回调详情或列表并带 `orderId` 预填且数据一致。

### Implementation for User Story 4

- [X] T039 [US4] 在 `payflow-admin-client/src/components/orders/OrderDetailPanel.vue` 增加「查看商户回调」入口
- [X] T040 [US4] 跳转逻辑：优先 `getByOrder` 打开 `DetailDrawer`；多类型时展示选择或默认 PAYMENT `payflow-admin-client/src/pages/admin/merchant-notifies/index.vue` 或 Panel 内嵌
- [X] T041 [US4] 更新 `docs/CONTRACT_MATRIX.md` 订单页跳转说明

**Checkpoint**: US4 为体验增强，可在 MVP（US1–US3）之后交付。

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: 文档、编译、E2E、日志闭环、合规章节。

- [X] T042 [P] 运行 `mvn -B -pl payflow-cashier-server,payflow-admin-server -DskipTests compile` 确保编译通过
- [X] T043 执行 `python scripts/install_demo_db.py` 并重跑写库/查询相关单测
- [X] T044 按 `specs/010-merchant-callback-query/quickstart.md` 完成手工验收并记录结果
- [X] T045 [P] 按需补充 Playwright 场景 `payflow-admin-client/tests/merchant-notifies.spec.ts`（菜单可见、列表筛选、详情打开）
- [X] T046 启动 admin/cashier 服务走查支付回调链路，确认后台日志无未处理异常（`[商户回调]`、写库失败）
- [X] T047 [P] 核对 Constitution Check 九项在实现中均满足（模块边界、cashier_ 前缀、脱敏、merchantScope）
- [X] T048 将 `specs/010-merchant-callback-query/checklists/requirements.md` 中实现相关项勾选（若实现阶段有偏差则回写 spec/plan）

---

## Dependencies & Execution Order

### Phase Dependencies

```text
Phase 1 Setup
    ↓
Phase 2 Foundational（阻断）
    ↓
Phase 3 US1 ──┬── Phase 4 US2（依赖 US1 查询服务，可与 US2 后端并行）
              ↓
Phase 5 US3（依赖 US1 列表 API + US2 详情组件）
    ↓
Phase 6 US4（依赖 US3 或至少 US2 DetailDrawer + by-order API）
    ↓
Phase 7 Polish
```

### User Story Dependencies

| 故事 | 依赖 | 说明 |
|------|------|------|
| US1 | Foundational | MVP：API 按订单查汇总 |
| US2 | US1 查询服务 | 详情 + 脱敏 + DetailDrawer |
| US3 | US1、US2 | 列表页 + 菜单 |
| US4 | US2、US3（推荐） | 订单页跳转 |

### Parallel Opportunities

**Phase 1**: T003 ∥ T004 ∥ T005  
**Phase 2**: T006–T013 实体/Mapper 可并行；T018 待 T016 完成后  
**Phase 3**: T019 ∥ T020（测试）可并行；T021→T022→T023 串行  
**Phase 4**: T026 ∥ T025；T029 ∥ T030（前端）在 T028 后  
**Phase 5**: T033–T035 前端可与 T032 后端并行（契约已定后）

---

## Parallel Example: Foundational

```bash
# 并行创建实体与 Mapper（不同文件）：
T006 MerchantNotify.java (cashier)
T007 MerchantNotifyAttempt.java (cashier)
T010 MerchantNotify.java (admin)
T011 MerchantNotifyAttempt.java (admin)
T008–T009、T012–T013 Mapper 文件
```

---

## Implementation Strategy

### MVP First（推荐范围）

1. Phase 1 + Phase 2（写库可用）
2. Phase 3 US1（按订单 API 查询）
3. Phase 4 US2（详情 + 脱敏 + DetailDrawer）
4. **STOP**：用 quickstart 步骤 1–2、5 验证；可演示排障价值

### 完整交付

5. Phase 5 US3（列表菜单）
6. Phase 6 US4（订单跳转，可选）
7. Phase 7 Polish

### 任务统计

| 阶段 | 任务数 |
|------|--------|
| Phase 1 Setup | 5 |
| Phase 2 Foundational | 13 |
| Phase 3 US1 | 6 |
| Phase 4 US2 | 7 |
| Phase 5 US3 | 7 |
| Phase 6 US4 | 3 |
| Phase 7 Polish | 7 |
| **合计** | **48** |

**Suggested MVP scope**: T001–T024（Setup + Foundational + US1），约 24 项。  
**Full feature**: T001–T048。
