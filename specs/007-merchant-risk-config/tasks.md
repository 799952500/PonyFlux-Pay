# Tasks: 商户级风控配置

**Input**: Design documents from `/specs/007-merchant-risk-config/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: 本规格和 quickstart 明确要求覆盖权限边界、规则范围匹配、支付拦截和审计场景，因此包含后端与前端测试任务。

**Organization**: 任务按用户故事分组，每个故事可独立实施和测试。任务分组采用 PonyFlux-Pay 项目的 Maven 模块边界。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行执行（不同文件，无依赖）
- **[Story]**: 归属的用户故事（US1、US2、US3、US4）
- 任务描述必须包含精确的文件路径

---

## Phase 1: Setup（共享基础设施）

**Purpose**: 准备数据库、共享类型、接口契约和现有文档基线。

- [X] T001 创建商户级风控增量迁移脚本 `sql/migrations/2026-05-20_merchant_risk_config.sql`
- [X] T002 更新全量 Schema 中 `risk_rules`、新增作用范围/命中/审计表定义 `sql/schema/payflow_admin.sql`
- [X] T003 更新演示种子数据，补充平台全局、平台定向、商户自建风控示例 `sql/seed/payflow_admin_seed.sql`
- [X] T004 [P] 更新风控接口契约矩阵占位条目 `docs/CONTRACT_MATRIX.md`
- [X] T005 [P] 扩展前端风控类型定义 `payflow-admin-client/src/types/index.ts`

---

## Phase 2: Foundational（阻断性前置条件）

**Purpose**: 建立所有用户故事共享的数据模型、DTO、Mapper、Service 边界和运行时同步能力。

**CRITICAL**: 在本阶段完成之前，不得开始任何用户故事工作。

- [X] T006 扩展管理端风控规则实体字段 `payflow-admin-server/src/main/java/com/payflow/admin/entity/RiskRule.java`
- [X] T007 [P] 创建规则作用范围实体 `payflow-admin-server/src/main/java/com/payflow/admin/entity/RiskRuleMerchantScope.java`
- [X] T008 [P] 创建风控命中记录实体 `payflow-admin-server/src/main/java/com/payflow/admin/entity/RiskHitRecord.java`
- [X] T009 [P] 创建风控规则审计实体 `payflow-admin-server/src/main/java/com/payflow/admin/entity/RiskRuleAuditLog.java`
- [X] T010 [P] 创建管理端风控查询 DTO `payflow-admin-server/src/main/java/com/payflow/admin/dto/RiskRuleQueryRequest.java`
- [X] T011 [P] 创建风控规则保存 DTO `payflow-admin-server/src/main/java/com/payflow/admin/dto/RiskRuleUpsertRequest.java`
- [X] T012 [P] 创建风控状态切换 DTO `payflow-admin-server/src/main/java/com/payflow/admin/dto/RiskRuleStatusRequest.java`
- [X] T013 [P] 创建风控规则展示 VO `payflow-admin-server/src/main/java/com/payflow/admin/dto/RiskRuleVO.java`
- [X] T014 [P] 创建风控命中记录 VO `payflow-admin-server/src/main/java/com/payflow/admin/dto/RiskHitRecordVO.java`
- [X] T015 [P] 创建规则作用范围 Mapper `payflow-admin-server/src/main/java/com/payflow/admin/mapper/RiskRuleMerchantScopeMapper.java`
- [X] T016 [P] 创建风控命中记录 Mapper `payflow-admin-server/src/main/java/com/payflow/admin/mapper/RiskHitRecordMapper.java`
- [X] T017 [P] 创建风控规则审计 Mapper `payflow-admin-server/src/main/java/com/payflow/admin/mapper/RiskRuleAuditLogMapper.java`
- [X] T018 创建管理端风控服务接口 `payflow-admin-server/src/main/java/com/payflow/admin/service/RiskRuleAdminService.java`
- [X] T019 创建管理端风控服务实现骨架 `payflow-admin-server/src/main/java/com/payflow/admin/service/impl/RiskRuleAdminServiceImpl.java`
- [X] T020 创建风控规则审计服务 `payflow-admin-server/src/main/java/com/payflow/admin/service/RiskRuleAuditService.java`
- [X] T021 创建风控命中查询服务 `payflow-admin-server/src/main/java/com/payflow/admin/service/RiskHitRecordQueryService.java`
- [X] T022 扩展收银台风控规则实体字段 `payflow-cashier-server/src/main/java/com/payflow/cashier/entity/RiskRule.java`
- [X] T023 [P] 创建收银台规则作用范围实体 `payflow-cashier-server/src/main/java/com/payflow/cashier/entity/RiskRuleMerchantScope.java`
- [X] T024 [P] 创建收银台风控命中记录实体 `payflow-cashier-server/src/main/java/com/payflow/cashier/entity/RiskHitRecord.java`
- [X] T025 [P] 创建收银台规则作用范围 Mapper `payflow-cashier-server/src/main/java/com/payflow/cashier/mapper/RiskRuleMerchantScopeMapper.java`
- [X] T026 [P] 创建收银台风控命中记录 Mapper `payflow-cashier-server/src/main/java/com/payflow/cashier/mapper/RiskHitRecordMapper.java`
- [X] T027 创建支付风控上下文 DTO `payflow-cashier-server/src/main/java/com/payflow/cashier/dto/PaymentRiskContext.java`
- [X] T028 修改风控规则服务接口支持按商户查询适用规则 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/RiskRuleService.java`
- [X] T029 实现按商户加载启用规则并按优先级排序 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/RiskRuleServiceImpl.java`
- [X] T030 创建风控命中记录写入服务 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/RiskHitRecordService.java`
- [X] T031 更新收银台配置刷新订阅以刷新风控规则缓存 `payflow-cashier-server/src/main/java/com/payflow/cashier/redis/CashierConfigRefreshSubscriber.java`

**Checkpoint**: 共享数据结构、Mapper、Service 边界和收银台规则加载基础就绪。

---

## Phase 3: User Story 1 - 管理员配置全局或定向风控 (Priority: P1) MVP

**Goal**: 管理员能查看所有规则，创建、编辑、启停平台规则，并配置全商户或指定商户作用范围。

**Independent Test**: 管理员创建一条全商户规则和一条仅作用于商户 A/B 的规则，列表可筛选，作用范围保存正确。

### Tests for User Story 1

- [ ] T032 [P] [US1] 编写管理员平台规则服务单元测试 `payflow-admin-server/src/test/java/com/payflow/admin/service/RiskRuleAdminServiceTest.java`
- [ ] T033 [P] [US1] 编写管理员风控 API 集成测试 `payflow-admin-server/src/test/java/com/payflow/admin/controller/AdminRiskControllerTest.java`

### Implementation for User Story 1

- [X] T034 [US1] 在管理端风控服务中实现管理员规则分页筛选 `payflow-admin-server/src/main/java/com/payflow/admin/service/impl/RiskRuleAdminServiceImpl.java`
- [X] T035 [US1] 在管理端风控服务中实现平台规则创建和字段校验 `payflow-admin-server/src/main/java/com/payflow/admin/service/impl/RiskRuleAdminServiceImpl.java`
- [X] T036 [US1] 在管理端风控服务中实现平台规则编辑、启停和空范围校验 `payflow-admin-server/src/main/java/com/payflow/admin/service/impl/RiskRuleAdminServiceImpl.java`
- [X] T037 [US1] 在管理端风控服务中实现平台定向商户范围替换 `payflow-admin-server/src/main/java/com/payflow/admin/service/impl/RiskRuleAdminServiceImpl.java`
- [X] T038 [US1] 接入风控规则变更审计和收银台配置刷新发布 `payflow-admin-server/src/main/java/com/payflow/admin/service/impl/RiskRuleAdminServiceImpl.java`
- [X] T039 [US1] 重构管理员风控 Controller 使用服务层并新增创建/筛选/状态/范围接口 `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminRiskController.java`
- [X] T040 [P] [US1] 扩展风控 API 调用方法 `payflow-admin-client/src/api/admin.ts`
- [X] T041 [P] [US1] 扩展风控列表筛选和作用范围类型 `payflow-admin-client/src/types/index.ts`
- [ ] T042 [US1] 改造管理员风控页面支持平台规则新增、编辑、启停、筛选和范围选择 `payflow-admin-client/src/pages/admin/risk.vue`
- [ ] T043 [US1] 更新管理员风控接口文档映射 `docs/CONTRACT_MATRIX.md`

**Checkpoint**: User Story 1 可独立运行和测试，作为 MVP 验收范围。

---

## Phase 4: User Story 2 - 商户配置仅对自己生效的风控 (Priority: P1)

**Goal**: 商户用户只能查看、创建、编辑、启停自己的自建规则，不能影响其他商户或平台规则。

**Independent Test**: 商户 A 创建自建规则后，商户 B 无法查看或修改；商户 A 传入其他 merchantId 时被拒绝或忽略。

### Tests for User Story 2

- [ ] T044 [P] [US2] 编写商户风控权限边界单元测试 `payflow-admin-server/src/test/java/com/payflow/admin/service/MerchantRiskRuleServiceTest.java`
- [ ] T045 [P] [US2] 编写商户风控 API 越权集成测试 `payflow-admin-server/src/test/java/com/payflow/admin/controller/MerchantRiskControllerTest.java`

### Implementation for User Story 2

- [ ] T046 [P] [US2] 创建商户风控规则保存 DTO `payflow-admin-server/src/main/java/com/payflow/admin/dto/MerchantRiskRuleUpsertRequest.java`
- [ ] T047 [P] [US2] 创建商户风控服务接口 `payflow-admin-server/src/main/java/com/payflow/admin/service/MerchantRiskRuleService.java`
- [ ] T048 [US2] 实现商户风控服务并从认证上下文派生 merchantId `payflow-admin-server/src/main/java/com/payflow/admin/service/impl/MerchantRiskRuleServiceImpl.java`
- [ ] T049 [US2] 实现商户自建规则查询、创建、编辑、启停权限校验 `payflow-admin-server/src/main/java/com/payflow/admin/service/impl/MerchantRiskRuleServiceImpl.java`
- [ ] T050 [US2] 创建商户风控 Controller `payflow-admin-server/src/main/java/com/payflow/admin/controller/MerchantRiskController.java`
- [ ] T051 [US2] 为商户规则变更接入审计记录和配置刷新发布 `payflow-admin-server/src/main/java/com/payflow/admin/service/impl/MerchantRiskRuleServiceImpl.java`
- [X] T052 [P] [US2] 扩展商户风控 API 调用方法 `payflow-admin-client/src/api/admin.ts`
- [ ] T053 [US2] 在风控页面按当前角色隐藏平台范围配置并固定商户自建语义 `payflow-admin-client/src/pages/admin/risk.vue`

**Checkpoint**: User Story 2 可独立验证商户自助配置和水平越权拒绝。

---

## Phase 5: User Story 3 - 支付请求按适用范围实时评估 (Priority: P1)

**Goal**: 支付请求只评估当前商户适用的启用规则，命中后返回安全的风控拒绝提示并记录命中上下文。

**Independent Test**: 准备平台全局、平台定向、商户自建三类规则，分别用不同商户发起支付请求，验证仅适用规则触发。

### Tests for User Story 3

- [ ] T054 [P] [US3] 编写适用规则加载单元测试 `payflow-cashier-server/src/test/java/com/payflow/cashier/service/RiskRuleServiceTest.java`
- [ ] T055 [P] [US3] 编写支付风控拦截单元测试 `payflow-cashier-server/src/test/java/com/payflow/cashier/service/RiskCheckServiceTest.java`
- [ ] T056 [P] [US3] 编写订单创建风控集成测试 `payflow-cashier-server/src/test/java/com/payflow/cashier/service/OrderRiskIntegrationTest.java`

### Implementation for User Story 3

- [ ] T057 [US3] 修改订单创建前风控调用构建支付风控上下文 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/OrderServiceImpl.java`
- [ ] T058 [US3] 修改风控校验接口接收支付风控上下文 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/RiskCheckService.java`
- [ ] T059 [US3] 实现按当前商户规则集合评估、优先级裁决和安全错误提示 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/RiskCheckServiceImpl.java`
- [ ] T060 [US3] 实现风控命中记录脱敏摘要和写入逻辑 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/RiskHitRecordServiceImpl.java`
- [ ] T061 [US3] 更新自定义规则评估上下文支持 merchantId 和 amountFen `payflow-cashier-server/src/main/java/com/payflow/cashier/risk/RiskQlEvaluator.java`
- [ ] T062 [US3] 移除风控阈值 BigDecimal 元兼容路径并统一 Long 分比较 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/RiskCheckServiceImpl.java`
- [ ] T063 [US3] 更新支付风控错误码和统一异常响应映射 `payflow-cashier-server/src/main/java/com/payflow/cashier/exception/GlobalExceptionHandler.java`

**Checkpoint**: User Story 3 可独立验证支付请求实时拦截、适用范围和命中记录写入。

---

## Phase 6: User Story 4 - 审计和排查风控命中 (Priority: P2)

**Goal**: 管理员可查看全部命中和审计记录，商户只能查看自己相关命中记录。

**Independent Test**: 多个商户触发命中后，管理员可筛选全部记录；商户 B 无法看到商户 A 的记录。

### Tests for User Story 4

- [ ] T064 [P] [US4] 编写风控命中查询服务测试 `payflow-admin-server/src/test/java/com/payflow/admin/service/RiskHitRecordQueryServiceTest.java`
- [ ] T065 [P] [US4] 编写风控审计查询 Controller 测试 `payflow-admin-server/src/test/java/com/payflow/admin/controller/RiskAuditQueryControllerTest.java`

### Implementation for User Story 4

- [X] T066 [US4] 实现管理员全部风控命中记录分页筛选 `payflow-admin-server/src/main/java/com/payflow/admin/service/impl/RiskHitRecordQueryServiceImpl.java`
- [X] T067 [US4] 实现商户风控命中记录按认证商户过滤 `payflow-admin-server/src/main/java/com/payflow/admin/service/impl/RiskHitRecordQueryServiceImpl.java`
- [X] T068 [US4] 实现风控规则审计记录分页筛选 `payflow-admin-server/src/main/java/com/payflow/admin/service/impl/RiskRuleAuditServiceImpl.java`
- [X] T069 [US4] 在管理员风控 Controller 新增命中记录和审计查询接口 `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminRiskController.java`
- [ ] T070 [US4] 在商户风控 Controller 新增商户命中记录查询接口 `payflow-admin-server/src/main/java/com/payflow/admin/controller/MerchantRiskController.java`
- [X] T071 [P] [US4] 扩展命中记录和审计 API 类型 `payflow-admin-client/src/types/index.ts`
- [X] T072 [P] [US4] 扩展命中记录和审计 API 方法 `payflow-admin-client/src/api/admin.ts`
- [ ] T073 [US4] 在风控页面增加命中记录和审计查询入口 `payflow-admin-client/src/pages/admin/risk.vue`

**Checkpoint**: User Story 4 可独立验证审计追踪和命中查询权限隔离。

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: 完成跨故事验证、文档、数据重装和质量门禁。

- [ ] T074 [P] 更新风控配置契约文档 `specs/007-merchant-risk-config/contracts/api.md`
- [ ] T075 [P] 更新商户级风控验证说明 `specs/007-merchant-risk-config/quickstart.md`
- [ ] T076 更新前后端完整接口矩阵 `docs/CONTRACT_MATRIX.md`
- [ ] T077 运行后端编译并修复发现的问题 `pom.xml`
- [ ] T078 运行后端完整测试并修复失败用例 `pom.xml`
- [ ] T079 运行管理端前端构建并修复类型或构建错误 `payflow-admin-client/package.json`
- [ ] T080 按 quickstart 完成平台全局、平台定向、商户自建、越权、审计五个验收场景 `specs/007-merchant-risk-config/quickstart.md`
- [ ] T081 执行宪法合规自查并记录结果 `specs/007-merchant-risk-config/plan.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 无依赖，可立即开始。
- **Foundational (Phase 2)**: 依赖 Setup 完成，阻断所有用户故事。
- **User Stories (Phase 3+)**: 全部依赖 Foundational 完成。
- **Polish (Phase 7)**: 依赖所有计划交付的用户故事完成。

### User Story Dependencies

- **US1 管理员配置全局或定向风控 (P1)**: Foundational 后可开始，MVP 范围。
- **US2 商户配置仅对自己生效的风控 (P1)**: Foundational 后可开始；依赖共享服务边界，不依赖 US1 UI，但可复用 US1 的 DTO/VO。
- **US3 支付请求按适用范围实时评估 (P1)**: Foundational 后可开始；需要至少有测试数据或 US1/US2 创建的规则用于端到端验证。
- **US4 审计和排查风控命中 (P2)**: 依赖审计与命中写入能力，建议在 US1/US2/US3 完成后实施。

### Within Each User Story

- 测试任务先写，确保能覆盖目标行为。
- DTO/实体/Mapper 优先于 Service。
- Service 优先于 Controller。
- Controller/API 优先于前端页面。
- 当前故事达到独立测试标准后再进入下一优先级故事。

### Module Boundary Order

```text
sql/schema + dto/entity/mapper
    → payflow-admin-server service/controller
    → payflow-cashier-server risk execution
    → payflow-admin-client api/types/page
    → docs/tests/quickstart
```

---

## Parallel Execution Examples

### User Story 1

```bash
Task: "编写管理员平台规则服务单元测试 payflow-admin-server/src/test/java/com/payflow/admin/service/RiskRuleAdminServiceTest.java"
Task: "编写管理员风控 API 集成测试 payflow-admin-server/src/test/java/com/payflow/admin/controller/AdminRiskControllerTest.java"
Task: "扩展风控 API 调用方法 payflow-admin-client/src/api/admin.ts"
Task: "扩展风控列表筛选和作用范围类型 payflow-admin-client/src/types/index.ts"
```

### User Story 2

```bash
Task: "创建商户风控规则保存 DTO payflow-admin-server/src/main/java/com/payflow/admin/dto/MerchantRiskRuleUpsertRequest.java"
Task: "创建商户风控服务接口 payflow-admin-server/src/main/java/com/payflow/admin/service/MerchantRiskRuleService.java"
Task: "扩展商户风控 API 调用方法 payflow-admin-client/src/api/admin.ts"
```

### User Story 3

```bash
Task: "编写适用规则加载单元测试 payflow-cashier-server/src/test/java/com/payflow/cashier/service/RiskRuleServiceTest.java"
Task: "编写支付风控拦截单元测试 payflow-cashier-server/src/test/java/com/payflow/cashier/service/RiskCheckServiceTest.java"
Task: "编写订单创建风控集成测试 payflow-cashier-server/src/test/java/com/payflow/cashier/service/OrderRiskIntegrationTest.java"
```

### User Story 4

```bash
Task: "编写风控命中查询服务测试 payflow-admin-server/src/test/java/com/payflow/admin/service/RiskHitRecordQueryServiceTest.java"
Task: "编写风控审计查询 Controller 测试 payflow-admin-server/src/test/java/com/payflow/admin/controller/RiskAuditQueryControllerTest.java"
Task: "扩展命中记录和审计 API 类型 payflow-admin-client/src/types/index.ts"
Task: "扩展命中记录和审计 API 方法 payflow-admin-client/src/api/admin.ts"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. 完成 Phase 1: Setup。
2. 完成 Phase 2: Foundational。
3. 完成 Phase 3: User Story 1。
4. 停止并验证：管理员可创建平台全商户规则和平台定向规则，列表筛选和范围维护正确。
5. MVP 验收后再继续商户自助和支付执行侧改造。

### Incremental Delivery

1. Setup + Foundational → 数据结构、服务边界和执行侧规则加载就绪。
2. + US1 → 管理员平台规则管理可用。
3. + US2 → 商户自助规则和水平越权防护可用。
4. + US3 → 支付请求实时按适用范围拦截可用。
5. + US4 → 命中记录和审计排查闭环可用。

### Parallel Team Strategy

1. 团队共同完成 Setup + Foundational。
2. Foundational 完成后：
   - 开发者 A: US1 管理员平台规则配置。
   - 开发者 B: US2 商户自助规则隔离。
   - 开发者 C: US3 支付请求风控执行。
   - 开发者 D: US4 命中记录和审计查询。
3. 每个故事独立完成测试后集成。

---

## Validation Summary

- 所有任务均使用 `- [ ] T###` 格式。
- 所有用户故事阶段任务均包含 `[US#]` 标签。
- 所有任务描述均包含明确文件路径。
- 并行任务使用 `[P]` 标记且位于不同文件或无直接依赖。
- 每个用户故事包含独立测试标准和可验收 Checkpoint。
