# Tasks: 商户数据隔离治理

**Input**: Design documents from `/specs/008-merchant-data-isolation/`  
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/, quickstart.md

**Tests**: 本规格明确要求验证多商户隔离、跨商户拒绝、全局配置共享、Playwright E2E 与后台日志闭环，因此包含测试任务。

**Organization**: 任务按用户故事分组，每个故事可独立实施和测试。任务分组采用 PonyFlux-Pay 项目的 Maven 模块边界。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行执行（不同文件，无依赖）
- **[Story]**: 归属的用户故事（US1、US2、US3）
- 任务描述必须包含精确的文件路径

## Phase 1: Setup（共享基础设施）

**Purpose**: 建立任务实施所需的清单、迁移、测试数据和契约基线。

- [X] T001 梳理商户隔离入口清单并记录到 `specs/008-merchant-data-isolation/isolation-inventory.md`
- [X] T002 [P] 对照接口契约更新前后端契约矩阵 `docs/CONTRACT_MATRIX.md`
- [X] T003 [P] 准备多商户验收数据设计并记录到 `specs/008-merchant-data-isolation/acceptance.md`
- [X] T004 创建商户隔离治理增量迁移脚本 `sql/migrations/2026-05-21_merchant_data_isolation_governance.sql`
- [X] T005 更新 Demo 重装数据以包含两个商户管理员、系统管理员和跨商户样本 `sql/seed/payflow_admin_seed.sql`
- [X] T006 更新交易侧 Demo 数据以包含多商户订单、支付、退款和安全审计样本 `sql/seed/payflow_cashier_seed.sql`

---

## Phase 2: Foundational（阻断性前置条件）

**Purpose**: 必须在所有用户故事之前完成的商户授权范围、审计和数据分类基础能力。

**CRITICAL**: 在本阶段完成之前，不得开始任何用户故事工作。

- [X] T007 定义后台用户商户授权上下文 DTO `payflow-admin-server/src/main/java/com/payflow/admin/dto/MerchantScopeDTO.java`
- [X] T008 实现后台授权范围解析服务接口 `payflow-admin-server/src/main/java/com/payflow/admin/service/AdminMerchantScopeService.java`
- [X] T009 实现后台授权范围解析服务 `payflow-admin-server/src/main/java/com/payflow/admin/service/impl/AdminMerchantScopeServiceImpl.java`
- [X] T010 将登录响应补充商户授权范围与平台管理员标识 `payflow-admin-server/src/main/java/com/payflow/admin/service/impl/AdminAuthServiceImpl.java`
- [X] T011 [P] 定义隔离检查项 DTO `payflow-admin-server/src/main/java/com/payflow/admin/dto/DataIsolationCheckDTO.java`
- [X] T012 [P] 定义隔离检查查询 DTO `payflow-admin-server/src/main/java/com/payflow/admin/dto/DataIsolationCheckQueryDTO.java`
- [X] T013 [P] 定义商户级资源引用 DTO `payflow-admin-server/src/main/java/com/payflow/admin/dto/MerchantResourceRefDTO.java`
- [X] T014 创建数据隔离检查项实体 `payflow-admin-server/src/main/java/com/payflow/admin/entity/DataIsolationCheck.java`
- [X] T015 创建数据隔离检查项 Mapper `payflow-admin-server/src/main/java/com/payflow/admin/mapper/DataIsolationCheckMapper.java`
- [X] T016 实现数据隔离检查服务接口 `payflow-admin-server/src/main/java/com/payflow/admin/service/DataIsolationCheckService.java`
- [X] T017 实现数据隔离检查服务 `payflow-admin-server/src/main/java/com/payflow/admin/service/impl/DataIsolationCheckServiceImpl.java`
- [X] T018 扩展后台审计服务以记录商户归属和拒绝访问结果 `payflow-admin-server/src/main/java/com/payflow/admin/service/AuditLogService.java`
- [X] T019 为后台跨商户访问拒绝补充统一错误处理 `payflow-admin-server/src/main/java/com/payflow/admin/exception/GlobalExceptionHandler.java`
- [X] T020 [P] 补充后台授权范围单元测试 `payflow-admin-server/src/test/java/com/payflow/admin/service/AdminMerchantScopeServiceTest.java`
- [X] T021 [P] 补充隔离检查服务单元测试 `payflow-admin-server/src/test/java/com/payflow/admin/service/DataIsolationCheckServiceTest.java`

**Checkpoint**: 授权范围、检查项、审计和安全拒绝基础设施就绪，可以开始用户故事。

---

## Phase 3: User Story 1 - 商户管理员只能运维本商户数据 (Priority: P1) MVP

**Goal**: 商户管理员在后台只能查看、维护、统计、导出和批量处理授权商户范围内的数据，无法通过参数或资源标识跨商户访问。

**Independent Test**: 使用商户 A 管理员访问订单、支付、退款、渠道配置、路由、对账、风控、统计和导出入口，确认只返回商户 A；使用商户 B 标识访问详情、编辑、审核、退款或批量操作时被拒绝且不泄露数据。

### Tests for User Story 1

- [X] T022 [P] [US1] 编写后台订单隔离测试 `payflow-admin-server/src/test/java/com/payflow/admin/service/OrderServiceMerchantIsolationTest.java`
- [X] T023 [P] [US1] 编写后台支付与退款隔离测试 `payflow-admin-server/src/test/java/com/payflow/admin/service/PaymentRefundMerchantIsolationTest.java`
- [X] T024 [P] [US1] 编写渠道账号与路由隔离测试 `payflow-admin-server/src/test/java/com/payflow/admin/service/ChannelConfigMerchantIsolationTest.java`
- [X] T025 [P] [US1] 编写对账查询隔离测试 `payflow-admin-server/src/test/java/com/payflow/admin/service/AdminReconMerchantIsolationTest.java`
- [X] T026 [P] [US1] 编写导出与批量操作隔离测试 `payflow-admin-server/src/test/java/com/payflow/admin/service/AdminExportMerchantIsolationTest.java`
- [X] T027 [P] [US1] 编写收银端资源归属回归测试 `payflow-cashier-server/src/test/java/com/payflow/cashier/service/ResourceOwnershipServiceTest.java`

### Implementation for User Story 1

- [X] T028 [US1] 将商户授权范围应用到后台订单查询与详情 `payflow-admin-server/src/main/java/com/payflow/admin/service/OrderService.java`
- [X] T029 [US1] 将商户授权范围应用到后台订单接口 `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminOrderController.java`
- [X] T030 [US1] 将商户授权范围应用到后台支付查询 `payflow-admin-server/src/main/java/com/payflow/admin/service/PaymentService.java`
- [X] T031 [US1] 将商户授权范围应用到后台退款查询与审核 `payflow-admin-server/src/main/java/com/payflow/admin/service/AdminRefundService.java`
- [X] T032 [US1] 将商户授权范围应用到后台退款接口 `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminRefundController.java`
- [X] T033 [US1] 将商户授权范围应用到渠道账号查询与维护 `payflow-admin-server/src/main/java/com/payflow/admin/service/PaymentAccountService.java`
- [X] T034 [US1] 将商户授权范围应用到渠道账号接口 `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminChannelAccountController.java`
- [X] T035 [US1] 将商户授权范围应用到商户支付方式配置 `payflow-admin-server/src/main/java/com/payflow/admin/service/MerchantPaymentMethodService.java`
- [X] T036 [US1] 将商户授权范围应用到商户支付路由配置 `payflow-admin-server/src/main/java/com/payflow/admin/service/MerchantPaymentRouteService.java`
- [X] T037 [US1] 将商户授权范围应用到渠道路由同步服务 `payflow-admin-server/src/main/java/com/payflow/admin/service/MerchantCashierRouteSyncService.java`
- [X] T038 [US1] 将商户授权范围应用到对账任务和差异查询 `payflow-admin-server/src/main/java/com/payflow/admin/service/AdminReconQueryService.java`
- [X] T039 [US1] 将商户授权范围应用到安全审计查询 `payflow-admin-server/src/main/java/com/payflow/admin/service/AdminSecurityAuditService.java`
- [X] T040 [US1] 将商户授权范围应用到风控规则管理 `payflow-admin-server/src/main/java/com/payflow/admin/service/RiskRuleAdminService.java`
- [X] T041 [US1] 将商户授权范围应用到风控命中记录查询 `payflow-admin-server/src/main/java/com/payflow/admin/service/RiskHitRecordQueryService.java`
- [X] T042 [US1] 将商户授权范围应用到仪表盘聚合指标 `payflow-admin-server/src/main/java/com/payflow/admin/service/DashboardAggregationService.java`
- [X] T043 [US1] 将商户授权范围应用到流失预警查询 `payflow-admin-server/src/main/java/com/payflow/admin/service/ChurnAlertService.java`
- [X] T044 [US1] 将商户授权范围应用到计费与费率查询 `payflow-admin-server/src/main/java/com/payflow/admin/service/FeeRateService.java`
- [X] T045 [US1] 将商户授权范围应用到智能路由日志查询 `payflow-admin-server/src/main/java/com/payflow/admin/controller/RoutingLogController.java`
- [X] T046 [US1] 将导出任务限定到授权商户范围 `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminExportController.java`
- [X] T047 [US1] 确保收银端订单查询继续使用资源归属校验 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/ResourceOwnershipService.java`
- [X] T048 [US1] 确保收银端退款创建和查询使用当前商户上下文 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/RefundServiceImpl.java`
- [X] T049 [US1] 确保收银端支付链路创建时绑定认证商户 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/PaymentServiceImpl.java`
- [X] T050 [US1] 确保支付回调、补单和 Webhook 投递保持商户归属一致 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/PayNotifyService.java`
- [X] T051 [US1] 在后台前端 Store 保存授权商户范围 `payflow-admin-client/src/stores/admin.ts`
- [X] T052 [US1] 调整后台 API 请求类型以携带可选商户范围筛选 `payflow-admin-client/src/types/index.ts`
- [X] T053 [US1] 在订单页面自动限制商户管理员可见范围 `payflow-admin-client/src/pages/admin/orders/index.vue`
- [X] T054 [US1] 在订单详情页面隐藏或拒绝授权外资源展示 `payflow-admin-client/src/pages/admin/orders/detail.vue`
- [X] T055 [US1] 在退款页面自动限制商户管理员可见范围 `payflow-admin-client/src/pages/admin/refunds.vue`
- [X] T056 [US1] 在支付账号页面自动限制商户管理员可见范围 `payflow-admin-client/src/pages/admin/payment-accounts.vue`
- [X] T057 [US1] 在渠道路由页面自动限制商户管理员可见范围 `payflow-admin-client/src/pages/admin/channel-routes.vue`
- [X] T058 [US1] 在对账页面自动限制商户管理员可见范围 `payflow-admin-client/src/pages/admin/reconcile/results.vue`
- [X] T059 [US1] 在风控页面自动限制商户管理员可见范围 `payflow-admin-client/src/pages/admin/risk.vue`
- [X] T060 [US1] 在仪表盘页面自动限制商户管理员统计范围 `payflow-admin-client/src/pages/admin/dashboard.vue`
- [X] T061 [US1] 更新前后端合同矩阵中的商户管理员数据范围说明 `docs/CONTRACT_MATRIX.md`

**Checkpoint**: User Story 1 可独立运行；商户管理员无法跨商户读取、修改、统计、导出或批量处理数据。

---

## Phase 4: User Story 2 - 系统管理员可进行全局治理与跨商户审计 (Priority: P1)

**Goal**: 系统管理员保留跨商户查看、筛选、审计、排障和全局配置治理能力，并能明确区分商户级数据与全局配置。

**Independent Test**: 使用系统管理员查看商户级数据列表、全局配置、跨商户统计和审计入口，确认可按商户筛选并看到数据归属；维护全局配置时不强制绑定单一商户。

### Tests for User Story 2

- [X] T062 [P] [US2] 编写系统管理员跨商户筛选测试 `payflow-admin-server/src/test/java/com/payflow/admin/service/SystemAdminMerchantScopeTest.java`
- [X] T063 [P] [US2] 编写全局配置访问与脱敏测试 `payflow-admin-server/src/test/java/com/payflow/admin/service/GlobalConfigAccessTest.java`
- [X] T064 [P] [US2] 编写审计定位能力测试 `payflow-admin-server/src/test/java/com/payflow/admin/service/AdminAuditMerchantTraceTest.java`

### Implementation for User Story 2

- [X] T065 [US2] 实现数据隔离检查结果查询接口 `payflow-admin-server/src/main/java/com/payflow/admin/controller/DataIsolationCheckController.java`
- [X] T066 [US2] 为商户管理接口补充系统管理员商户筛选和归属展示 `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminMerchantController.java`
- [X] T067 [US2] 为后台搜索接口补充商户归属和系统管理员范围筛选 `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminSearchController.java`
- [X] T068 [US2] 为操作日志接口补充商户归属筛选 `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminAuditLogController.java`
- [X] T069 [US2] 区分系统配置中的全局项与商户级敏感项 `payflow-admin-server/src/main/java/com/payflow/admin/service/SystemConfigService.java`
- [X] T070 [US2] 区分支付方式公共定义与商户支付方式配置 `payflow-admin-server/src/main/java/com/payflow/admin/service/PaymentMethodService.java`
- [X] T071 [US2] 区分基础渠道公共定义与商户渠道账号配置 `payflow-admin-server/src/main/java/com/payflow/admin/service/ChannelService.java`
- [X] T072 [US2] 为后台审计日志写入商户归属、资源类别和拒绝原因 `payflow-admin-server/src/main/java/com/payflow/admin/service/AuditLogService.java`
- [X] T073 [US2] 在后台 API 模块增加隔离检查结果接口 `payflow-admin-client/src/api/admin.ts`
- [X] T074 [US2] 在后台类型定义中增加隔离检查项和授权范围类型 `payflow-admin-client/src/types/index.ts`
- [X] T075 [US2] 新增数据隔离治理页面 `payflow-admin-client/src/pages/admin/data-isolation.vue`
- [X] T076 [US2] 在后台路由注册数据隔离治理页面 `payflow-admin-client/src/router/index.ts`
- [X] T077 [US2] 在侧边栏或菜单数据中加入数据隔离治理入口 `sql/seed/payflow_admin_seed.sql`
- [X] T078 [US2] 在系统配置页面展示全局配置标识和脱敏摘要 `payflow-admin-client/src/pages/admin/settings.vue`
- [X] T079 [US2] 在审计日志页面增加商户归属筛选和展示 `payflow-admin-client/src/pages/admin/audit-logs.vue`
- [X] T080 [US2] 更新合同矩阵中的系统管理员治理和全局配置契约 `docs/CONTRACT_MATRIX.md`

**Checkpoint**: User Story 2 可独立运行；系统管理员可跨商户治理，全局配置不会被误隔离。

---

## Phase 5: User Story 3 - 发现并治理历史数据隔离缺口 (Priority: P2)

**Goal**: 检查现有数据和功能入口，识别缺少商户归属或未按商户限制的入口，并将无法自动判断的数据限制访问或纳入人工确认。

**Independent Test**: 执行隔离检查后，所有数据类别被标记为商户级、全局级、系统审计或待人工确认；应商户隔离但缺少归属的数据对普通商户管理员不可见；整改后跨商户访问测试通过。

### Tests for User Story 3

- [X] T081 [P] [US3] 编写数据分类扫描服务测试 `payflow-admin-server/src/test/java/com/payflow/admin/service/DataIsolationInventoryScanServiceTest.java`
- [X] T082 [P] [US3] 编写缺失归属限制访问测试 `payflow-admin-server/src/test/java/com/payflow/admin/service/UnownedDataRestrictionTest.java`
- [X] T083 [P] [US3] 编写对账任务商户归属一致性测试 `payflow-recon-server/src/test/java/com/payflow/recon/service/ReconMerchantIsolationTest.java`

### Implementation for User Story 3

- [X] T084 [US3] 定义数据分类扫描服务接口 `payflow-admin-server/src/main/java/com/payflow/admin/service/DataIsolationInventoryScanService.java`
- [X] T085 [US3] 实现数据分类扫描服务 `payflow-admin-server/src/main/java/com/payflow/admin/service/impl/DataIsolationInventoryScanServiceImpl.java`
- [X] T086 [US3] 为订单和支付表生成隔离检查项 `payflow-admin-server/src/main/java/com/payflow/admin/service/impl/DataIsolationInventoryScanServiceImpl.java`
- [X] T087 [US3] 为退款表生成隔离检查项 `payflow-admin-server/src/main/java/com/payflow/admin/service/impl/DataIsolationInventoryScanServiceImpl.java`
- [X] T088 [US3] 为渠道账号和商户路由生成隔离检查项 `sql/migrations/2026-05-21_merchant_data_isolation_governance.sql`
- [X] T089 [US3] 为对账任务、账单记录和差异生成隔离检查项 `payflow-admin-server/src/main/java/com/payflow/admin/service/impl/DataIsolationInventoryScanServiceImpl.java`
- [X] T090 [US3] 为后台用户、角色、菜单和系统配置生成全局级或系统审计分类 `payflow-admin-server/src/main/java/com/payflow/admin/service/impl/DataIsolationInventoryScanServiceImpl.java`
- [X] T091 [US3] 为风控规则和作用商户范围生成隔离检查项 `sql/migrations/2026-05-21_merchant_data_isolation_governance.sql`
- [X] T092 [US3] 为计费、费率、路由日志和流失预警生成隔离检查项 `sql/migrations/2026-05-21_merchant_data_isolation_governance.sql`
- [X] T093 [US3] 在隔离检查接口中提供触发扫描能力 `payflow-admin-server/src/main/java/com/payflow/admin/controller/DataIsolationCheckController.java`
- [X] T094 [US3] 在对账任务生成时确保商户归属写入任务 `payflow-recon-server/src/main/java/com/payflow/recon/service/ReconTaskSeedService.java`
- [X] T095 [US3] 在对账比对时保持账单记录、支付记录和差异的商户归属一致 `payflow-recon-server/src/main/java/com/payflow/recon/service/ReconCompareService.java`
- [X] T096 [US3] 在对账差异处理审计中记录商户归属 `payflow-recon-server/src/main/java/com/payflow/recon/entity/ReconDiff.java`
- [X] T097 [US3] 在隔离治理页面增加扫描、风险等级和整改状态操作 `payflow-admin-client/src/pages/admin/data-isolation.vue`
- [X] T098 [US3] 更新 Demo 全量安装脚本以包含隔离检查项种子数据 `sql/migrations/2026-05-21_merchant_data_isolation_governance.sql`
- [X] T099 [US3] 更新数据清单文档中的分类和豁免结果 `specs/008-merchant-data-isolation/isolation-inventory.md`
- [X] T100 [US3] 更新验收文档中的存量缺口处置结果 `specs/008-merchant-data-isolation/acceptance.md`

**Checkpoint**: User Story 3 可独立运行；存量数据分类完成，缺失归属数据默认限制访问或进入人工确认。

---

## Final Phase: Polish & Cross-Cutting Concerns

**Purpose**: 影响多个用户故事的验证、文档、性能、安全和交付闭环。

- [X] T101 [P] 更新商户隔离验收说明 `specs/008-merchant-data-isolation/quickstart.md`
- [ ] T102 [P] 更新对账文档中的商户归属说明 `docs/reconciliation.md`
- [ ] T103 [P] 更新退款状态机文档中的商户隔离说明 `docs/REFUND_STATE_MACHINE.md`
- [ ] T104 运行 Demo 数据库重置并验证多商户账号 `scripts/install_demo_db.py`
- [X] T105 运行后台模块编译和测试 `payflow-admin-server/pom.xml`（隔离相关 66 项通过；HttpSmokeRunnerTest 需服务在线）
- [ ] T106 运行收银模块编译和测试 `payflow-cashier-server/pom.xml`
- [ ] T107 运行对账模块编译和测试 `payflow-recon-server/pom.xml`
- [ ] T108 运行全量 Maven 编译 `pom.xml`
- [ ] T109 运行后台前端测试和类型检查 `payflow-admin-client/package.json`
- [ ] T110 按需运行后台 Playwright E2E 并覆盖多商户隔离路径 `payflow-admin-client/playwright.config.ts`
- [ ] T111 按需运行收银台 Playwright E2E 并覆盖支付链路回归 `payflow-cashier-client/playwright.config.ts`
- [ ] T112 监控 admin-server、cashier-server、recon-server 日志并修复阻断错误 `specs/008-merchant-data-isolation/acceptance.md`
- [ ] T113 执行安全复核以确认跨商户拒绝不泄露目标数据 `specs/008-merchant-data-isolation/acceptance.md`
- [ ] T114 执行宪法合规检查并记录结果 `specs/008-merchant-data-isolation/acceptance.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 无依赖，可立即开始。
- **Foundational (Phase 2)**: 依赖 Setup 完成，阻断所有用户故事。
- **User Stories (Phase 3+)**: 全部依赖 Foundational 完成。
- **Polish (Final Phase)**: 依赖所有目标用户故事完成。

### User Story Dependencies

- **User Story 1 (P1)**: Foundational 完成后可开始，是建议 MVP。
- **User Story 2 (P1)**: Foundational 完成后可开始，可与 US1 并行，但共享审计和授权基础。
- **User Story 3 (P2)**: Foundational 完成后可开始；建议在 US1 关键入口隔离完成后验证存量缺口更准确。

### Within Each User Story

- 测试任务先写并确认可暴露当前缺口，再完成实现任务。
- 后端服务范围限制优先于前端展示限制。
- 查询、详情、统计、导出、批量、异步入口必须分别验证。
- 当前故事完成后必须独立验证，不依赖其他故事才算完成。

### Module Boundary Order

涉及多个 Maven 模块时，按依赖顺序实施：

```text
payflow-common（仅确需共享能力时）
  → payflow-admin-server / payflow-cashier-server / payflow-recon-server
  → payflow-admin-client / payflow-cashier-client
  → docs / specs 验收记录
```

---

## Parallel Execution Examples

### User Story 1

```bash
# 可并行先写不同服务的隔离测试
Task: "T022 后台订单隔离测试"
Task: "T023 支付与退款隔离测试"
Task: "T024 渠道账号与路由隔离测试"
Task: "T025 对账查询隔离测试"
Task: "T026 导出与批量操作隔离测试"
Task: "T027 收银端资源归属回归测试"

# 后端不同业务域范围限制可由不同执行者并行修改
Task: "T028-T032 订单、支付、退款范围限制"
Task: "T033-T037 渠道账号、支付方式、路由范围限制"
Task: "T038-T046 对账、审计、风控、统计、导出范围限制"
```

### User Story 2

```bash
# 系统管理员治理测试可并行
Task: "T062 系统管理员跨商户筛选测试"
Task: "T063 全局配置访问与脱敏测试"
Task: "T064 审计定位能力测试"

# 前后端可在接口契约稳定后并行
Task: "T065-T072 后台治理接口与服务"
Task: "T073-T079 前端治理页面与展示"
```

### User Story 3

```bash
# 存量治理测试可并行
Task: "T081 数据分类扫描服务测试"
Task: "T082 缺失归属限制访问测试"
Task: "T083 对账任务商户归属一致性测试"

# 不同数据域扫描规则可并行
Task: "T086-T088 交易与渠道数据扫描"
Task: "T089-T096 对账数据扫描与归属一致性"
Task: "T090-T092 后台配置、风控、费率数据扫描"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. 完成 Phase 1: Setup。
2. 完成 Phase 2: Foundational。
3. 完成 Phase 3: User Story 1。
4. 停止并验证：使用两个商户样本执行列表、详情、统计、导出、批量和异步入口隔离测试。
5. 涉及后台页面和跨服务流程时运行 Playwright/Playwright CLI，并监控后台日志至无阻断错误。

### Incremental Delivery

1. Setup + Foundational → 授权范围、隔离检查和审计基础就绪。
2. + US1 → 商户管理员不可跨商户运维数据，可作为 MVP 演示。
3. + US2 → 系统管理员具备跨商户治理和全局配置维护能力。
4. + US3 → 存量数据缺口可被扫描、限制访问、人工确认或整改。
5. Final Phase → 文档、测试、E2E、日志闭环和宪法合规完成。

### Validation Requirements

- 每个任务必须遵守 Maven 模块边界和统一响应格式。
- 所有商户级查询必须使用服务端授权范围，不能信任前端传入的 `merchantId`。
- 全局配置必须明确白名单或判定理由；含商户专属敏感信息时转为商户级数据。
- 跨商户访问拒绝不得泄露目标数据是否存在。
- Playwright 验证期间必须同步检查后台服务日志。
