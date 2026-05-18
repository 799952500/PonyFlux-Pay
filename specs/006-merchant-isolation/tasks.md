# Tasks: 商户数据隔离与水平越权急修

**Input**: Design documents from `/specs/006-merchant-isolation/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: spec 明确要求（SC-001、SC-005、FR-019）：新增 `MerchantIsolationSecurityTest` ≥30 用例 + admin 审计 API 测试 + 全量回归。

**Organization**: 按用户故事（US1–US5）分组；模块边界遵循宪法原则 I。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行（不同文件、无未完成依赖）
- **[Story]**: US1–US5（仅用户故事阶段任务带标签）
- 路径使用仓库实际包名 `com.payflow.*`

## Path Conventions

| 模块 | 源码路径 |
|------|----------|
| payflow-cashier-server | `payflow-cashier-server/src/main/java/com/payflow/cashier/` |
| payflow-admin-server | `payflow-admin-server/src/main/java/com/payflow/admin/` |
| payflow-admin-client | `payflow-admin-client/src/` |
| Flyway（cashier） | `payflow-cashier-server/src/main/resources/db/migration/cashier/` |

---

## Phase 1: Setup（共享准备）

**Purpose**: 错误码常量、异步配置、特性开关占位

- [X] T001 在 `payflow-cashier-server/src/main/java/com/payflow/cashier/constant/MerchantSecurityErrorCodes.java` 定义 `5101`/`5102`/`5103` 及默认 message 常量
- [X] T002 [P] 在 `payflow-cashier-server/src/main/java/com/payflow/cashier/config/SecurityAuditAsyncConfig.java` 新增 `securityAuditExecutor` 线程池 Bean
- [X] T003 [P] 在 `payflow-cashier-server/src/main/resources/application.yml` 增加 `payflow.security.audit.alert-threshold`（默认 20）与 `alert-window-minutes`（默认 5）

---

## Phase 2: Foundational（阻断性前置 — 必须先完成）

**Purpose**: 商户上下文、拦截器链、全局异常映射；所有用户故事依赖本阶段

**⚠️ CRITICAL**: 本阶段完成前不得开始 US1–US5。

- [X] T004 新增 `payflow-cashier-server/src/main/java/com/payflow/cashier/context/MerchantContext.java`（ThreadLocal：`merchantId`、`authMode`、`requestPath`、`clientIp`、`get`/`set`/`clear`）
- [X] T005 新增 `payflow-cashier-server/src/main/java/com/payflow/cashier/context/MerchantScopeHolder.java`（`runInSystemMode`、`runWithMerchant`、`isSystemMode`、`clear` + try-finally 约定文档）
- [X] T006 新增 `payflow-cashier-server/src/main/java/com/payflow/cashier/context/AuthMode.java` 枚举（JWT、HMAC、INTERNAL）
- [X] T007 新增 `payflow-cashier-server/src/main/java/com/payflow/cashier/middleware/MerchantContextInterceptor.java`（认证后写入 Context；`afterCompletion` 调用 `MerchantContext.clear()`）
- [X] T008 改造 `payflow-cashier-server/src/main/java/com/payflow/cashier/middleware/JwtAuthInterceptor.java`——认证成功后设置 `authMode=JWT`（仍写 `ATTR_MERCHANT_ID` 供 Context 拦截器读取）
- [X] T009 改造 `payflow-cashier-server/src/main/java/com/payflow/cashier/middleware/MerchantSignatureInterceptor.java`——验签成功后设置 `authMode=HMAC`
- [X] T010 改造 `payflow-cashier-server/src/main/java/com/payflow/cashier/config/WebMvcConfig.java`——注册 `MerchantContextInterceptor`（顺序：认证拦截器 → Context → 后续绑定/所有权拦截器）；维护白名单路径列表
- [X] T011 改造 `payflow-cashier-server/src/main/java/com/payflow/cashier/exception/GlobalExceptionHandler.java`——`BizException(5101)` → HTTP 403；`5102`/`5103` 对外统一 → HTTP 404 + message「请求的资源不存在」
- [X] T012 [P] 新增 `payflow-cashier-server/src/main/java/com/payflow/cashier/config/MerchantSecurityProperties.java`（`@ConfigurationProperties(prefix="payflow.security")`）绑定告警阈值

**Checkpoint**: 上下文可注入、错误码可抛出并正确映射 HTTP 状态

---

## Phase 3: User Story 1 — 商户无法跨越身份创建订单 (Priority: P1) 🎯 MVP

**Goal**: 禁止请求体 `merchantId` 覆盖 JWT/HMAC 上下文；修复 `OrderController.createOrder` 越权创建

**Independent Test**: 商户 A 的 Token + 请求体 `merchantId=商户B` → HTTP 403、`code=5101`；不传或传一致值 → 订单落在商户 A 名下

### Tests for User Story 1

- [ ] T013 [P] [US1] 新增 `payflow-cashier-server/src/test/java/com/payflow/cashier/middleware/MerchantIdBindingInterceptorTest.java`（不一致 403、一致放行、无字段从 Context 取值）
- [ ] T014 [P] [US1] 在 `payflow-cashier-server/src/test/java/com/payflow/cashier/controller/OrderControllerSecurityTest.java` 覆盖 POST `/api/v1/orders` 三种 merchantId 场景

### Implementation for User Story 1

- [X] T015 [US1] 新增 `payflow-cashier-server/src/main/java/com/payflow/cashier/middleware/MerchantIdBindingInterceptor.java`（解析 query/JSON 中 `merchantId`；不等 → `BizException(5101)` + 调用审计占位接口）
- [X] T016 [US1] 在 `WebMvcConfig.java` 注册 `MerchantIdBindingInterceptor`（作用于 `/api/v1/orders/**`、`/api/v1/payments/**`、`/api/v1/refunds/**`、`/api/v1/merchant/**`、`/api/v1/payment-links/**`，排除白名单）
- [X] T017 [US1] 修复 `payflow-cashier-server/src/main/java/com/payflow/cashier/controller/OrderController.java`——删除「以传入 merchantId 为准」逻辑；`createOrder` 使用 `MerchantContext.getMerchantId()` 写入 `CreateOrderRequest`
- [X] T018 [US1] 改造 `payflow-cashier-server/src/main/java/com/payflow/cashier/dto/CreateOrderRequest.java`——`merchantId` 字段添加 `@Schema(deprecated = true)` 与说明注释

**Checkpoint**: US1 可独立验收；不得再在商户 B 名下创建订单

---

## Phase 4: User Story 2 — 商户无法跨越身份查询/操作资源 (Priority: P1)

**Goal**: 按资源 ID 校验所有权；跨商户读/写统一 404 + 5102

**Independent Test**: 商户 A 查询商户 B 的 `orderId` → 404 + 5102，响应体无订单字段；对本商户资源正常 200

### Tests for User Story 2

- [ ] T019 [P] [US2] 新增 `payflow-cashier-server/src/test/java/com/payflow/cashier/service/ResourceOwnershipServiceTest.java`（存在且匹配、存在但不匹配、不存在均返回统一语义）
- [ ] T020 [P] [US2] 新增 `payflow-cashier-server/src/test/java/com/payflow/cashier/middleware/MerchantResourceOwnershipInterceptorTest.java`（路径变量 orderId/paymentId/refundId 场景）

### Implementation for User Story 2

- [X] T021 [US2] 新增 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/ResourceOwnershipService.java`（按 `orderId`/`paymentId`/`refundId`/`linkId` 查 `merchant_id`；不匹配或 null → `BizException(5102)`，审计 reason 5103）
- [X] T022 [US2] 新增 `payflow-cashier-server/src/main/java/com/payflow/cashier/middleware/MerchantResourceOwnershipInterceptor.java`（URI 模式 → 资源类型；调用 `ResourceOwnershipService`）
- [X] T023 [US2] 在 `WebMvcConfig.java` 注册 `MerchantResourceOwnershipInterceptor` 并配置路径模式（`orders/{orderId}`、`refunds/{refundId}`、`payments/status/{paymentId}`、`merchant/orders/{orderId}` 等）
- [X] T024 [US2] 改造 `payflow-cashier-server/src/main/java/com/payflow/cashier/controller/OrderController.java`——`getOrderDetail` 依赖拦截器或显式 `assertOwn(orderId)`
- [X] T025 [US2] 改造 `payflow-cashier-server/src/main/java/com/payflow/cashier/controller/RefundController.java`——`getRefund` 与 `create` 的 `paymentId` 级联所有权校验
- [ ] T026 [US2] 改造 `payflow-cashier-server/src/main/java/com/payflow/cashier/controller/PaymentController.java`——`getPaymentStatus` 所有权校验（若在本 Phase API 范围内）
- [ ] T027 [US2] 改造 `payflow-cashier-server/src/main/java/com/payflow/cashier/controller/MerchantQueryController.java`——`orders/{orderId}`、`payments/{paymentId}/status` 校验
- [ ] T028 [US2] 改造 `payflow-cashier-server/src/main/java/com/payflow/cashier/controller/PaymentLinkController.java`——列表/单条查询限定本商户（拦截器 + `merchant_id` 条件）

**Checkpoint**: 跨商户资源访问 100% 返回统一 404，无字段泄漏

---

## Phase 5: User Story 3 — 持久层纵深防御 (Priority: P1)

**Goal**: MyBatis 自动追加/校验 `merchant_id`；INSERT 覆盖；系统模式豁免 ≤10 处

**Independent Test**: 故意无 WHERE 的 `selectList` 在商户 A 上下文仅返回 A 的数据；回调路径 `runInSystemMode` 不注入错误条件

### Tests for User Story 3

- [ ] T029 [P] [US3] 新增 `payflow-cashier-server/src/test/java/com/payflow/cashier/mybatis/MerchantScopeInnerInterceptorTest.java`（SELECT 追加 WHERE、INSERT 补全/校验、系统模式跳过）
- [ ] T030 [P] [US3] 新增 `payflow-cashier-server/src/test/java/com/payflow/cashier/context/MerchantScopeHolderTest.java`（try-finally 清理、线程池无泄漏）

### Implementation for User Story 3

- [X] T031 [US3] 新增 `payflow-cashier-server/src/main/java/com/payflow/cashier/config/MybatisPlusConfig.java` 并注册 `MybatisPlusInterceptor`（TenantLine 实现 merchant_id 行级过滤）
- [ ] T032 [US3] 新增 `payflow-cashier-server/src/main/java/com/payflow/cashier/mybatis/MerchantScopeInnerInterceptor.java`（受保护表白名单；SELECT/UPDATE/DELETE 追加条件；INSERT 校验/补全；DEBUG 日志）
- [ ] T033 [US3] 对照 `payflow-cashier-server/src/main/resources/db/migration/cashier/V1__baseline.sql` 确认受保护表名（含 webhook 表若存在）并写入拦截器常量
- [X] T034 [US3] 在 `payflow-cashier-server/src/main/java/com/payflow/cashier/controller/PayNotifyController.java`（及 notify 入口）使用 `MerchantScopeHolder.runInSystemMode` 包裹渠道查单逻辑
- [X] T035 [US3] 在 `payflow-cashier-server/src/main/java/com/payflow/cashier/task/OrderTimeoutTask.java` 查单路径使用 `runInSystemMode`（若尚未覆盖）
- [ ] T036 [US3] 梳理并在代码注释中标注全部系统模式豁免点（目标 ≤10 处），输出清单到 PR 描述（可附 `specs/006-merchant-isolation/research.md` 链接）

**Checkpoint**: SQL 日志可见 `merchant_id` 注入；回调与定时任务不因拦截器失败

---

## Phase 6: User Story 4 — 安全审计全栈 (Priority: P2)

**Goal**: 越权拒绝异步落库；admin 分页 API；admin-client 列表页

**Independent Test**: 触发 5101 后 `cashier_security_audit` 有记录；RISK 角色可访问列表；普通 ADMIN 返回 403

### Tests for User Story 4

- [ ] T037 [P] [US4] 新增 `payflow-cashier-server/src/test/java/com/payflow/cashier/service/SecurityAuditServiceTest.java`（异步写入、失败不抛到调用方）
- [ ] T038 [P] [US4] 新增 `payflow-admin-server/src/test/java/com/payflow/admin/controller/AdminSecurityAuditControllerTest.java`（RISK 可查询、无权限 403、分页参数校验）

### Implementation for User Story 4

- [X] T039 [US4] 新增 Flyway `payflow-cashier-server/src/main/resources/db/migration/cashier/V4__cashier_security_audit.sql`（见 data-model.md）
- [X] T040 [P] [US4] 新增 `payflow-cashier-server/src/main/java/com/payflow/cashier/entity/SecurityAuditEntity.java` 与 `mapper/SecurityAuditMapper.java`
- [X] T041 [US4] 新增 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/SecurityAuditService.java`（`recordDenied` 异步；字段映射；超阈值 WARN 日志）
- [X] T042 [US4] 在 `MerchantIdBindingInterceptor.java` 与 `MerchantResourceOwnershipInterceptor.java`（及 `ResourceOwnershipService`）拒绝分支调用 `SecurityAuditService.recordDenied`
- [X] T043 [P] [US4] 新增 `payflow-admin-server/src/main/java/com/payflow/admin/entity/cashier/SecurityAuditEntity.java` 与 `mapper/cashier/SecurityAuditMapper.java`（cashier 数据源）
- [X] T044 [US4] 新增 `payflow-admin-server/src/main/java/com/payflow/admin/service/AdminSecurityAuditService.java`（分页查询；`pageSize` 上限 100；Lambda 条件，禁止 `${}`）
- [X] T045 [US4] 新增 `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminSecurityAuditController.java`——`GET /api/v1/admin/security/audit` + `@RequireRole({"RISK","SUPER_ADMIN"})`
- [X] T046 [P] [US4] 新增 `payflow-admin-client/src/api/securityAudit.ts`（列表查询 API 封装）
- [X] T047 [US4] 新增 `payflow-admin-client/src/pages/admin/security-audit.vue`（筛选：merchantId、时间、outcome、reasonCode、requestPath；分页表格）
- [X] T048 [US4] 在 `payflow-admin-client/src/router/index.ts` 注册路由 `/security-audit`
- [X] T049 [US4] 在 `sql/migrations/2026-05-18_security-audit-menu.sql` 增加「安全审计」菜单项（父级：系统管理）

**Checkpoint**: 运营可在管理后台查看 DENIED 审计记录

---

## Phase 7: User Story 5 — 现有商户集成不被破坏 (Priority: P1)

**Goal**: 兼容不传 merchantId 与传一致值；回归与文档同步

**Independent Test**: quickstart.md 第 5 节两组请求均 `code=0`；`mvn -B test` 全绿

### Tests for User Story 5

- [X] T050 [US5] 新增 `payflow-cashier-server/src/test/java/com/payflow/cashier/security/MerchantIsolationSecurityTest.java`——聚合 ≥30 场景（US1–US3 跨商户读/写/merchantId 不一致/白名单路径/兼容路径）
- [ ] T051 [P] [US5] 在 `MerchantIsolationSecurityTest.java` 增加 HMAC 路径用例（`/api/v1/merchant/orders/{id}`、`/api/v1/refunds`）

### Implementation for User Story 5

- [ ] T052 [US5] 审查并更新所有 Phase 0 范围内 DTO 的 `merchantId` Swagger `@Schema(deprecated=true)`（`PaymentLinkCreateRequest` 等）
- [ ] T053 [US5] 确认 `WebMvcConfig` 白名单未误伤 `/api/v1/payments/status/**`、`/api/v1/cashier/**`、`/api/v1/public/**`、`/notify/**`——补充集成测试断言

**Checkpoint**: 存量集成路径通过；公开端点行为不变

---

## Phase 8: Polish & Cross-Cutting（收尾）

**Purpose**: 文档、CI 门禁、全量回归

- [X] T054 [P] 更新 `docs/CONTRACT_MATRIX.md`——5101/5102/5103、admin `GET /api/v1/admin/security/audit`、受影响收银台端点行为说明
- [X] T055 新增 `payflow-cashier-server/src/test/java/com/payflow/cashier/arch/MerchantControllerArchTest.java`（ArchUnit：含 `@PathVariable *Id` 的 Controller 方法须在所有权拦截路径表或调用 `ResourceOwnershipService`）——对应 FR-020
- [X] T056 [P] 在 `.github/workflows/ci.yml` 确保 `payflow-cashier-server` 测试阶段包含 `MerchantIsolationSecurityTest` 与 `MerchantControllerArchTest`
- [X] T057 执行 `specs/006-merchant-isolation/quickstart.md` 全部步骤并记录结果（见 `acceptance.md`；手工 curl 步骤待环境）
- [X] T058 运行 `mvn -B test` 全量回归（cashier 全绿；admin 4 个既有集成测试需 MySQL/运行中服务，见 `acceptance.md`）
- [X] T059 宪法合规自检——对照 `plan.md` Constitution Check 九项逐条确认（见 `acceptance.md`）

---

## Dependencies & Execution Order

### Phase Dependencies

```text
Phase 1 (Setup)
    ↓
Phase 2 (Foundational) ← 阻断所有用户故事
    ↓
┌── US1 (merchantId 绑定) ──┐
│         ↓                  │
│    US2 (资源所有权) ← 依赖 US1 的 Context/错误码，可与 US3 部分并行
│         ↓                  │
│    US3 (MyBatis) ← 可与 US2 并行（不同包）
│         ↓                  │
│    US4 (审计) ← 依赖 US1/US2 拒绝分支挂钩
│         ↓                  │
│    US5 (回归) ← 依赖 US1–US4
└────────────────────────────┘
    ↓
Phase 8 (Polish)
```

### User Story Dependencies

| 故事 | 依赖 | 可与谁并行 |
|------|------|------------|
| US1 | Phase 2 | — |
| US2 | Phase 2、US1（审计挂钩前仅需 T004–T011） | US3（T031 起） |
| US3 | Phase 2 | US2（T021 起） |
| US4 | US1/US2 拒绝路径（T042） | T039–T041 可与 US2 尾段并行 |
| US5 | US1–US4 | — |

### Within Each User Story

1. 先写/更新测试（若本故事含测试任务）→ 确认 FAIL
2. 实现 Service / 拦截器
3. 改造 Controller
4. 注册 `WebMvcConfig`
5. 跑本故事 Independent Test

---

## Parallel Example: Foundational 完成后

```bash
# 开发者 A — US1 + US2 拦截器
T015 MerchantIdBindingInterceptor
T021 ResourceOwnershipService
T022 MerchantResourceOwnershipInterceptor

# 开发者 B — US3 持久层（无文件冲突）
T031 MybatisPlusConfig
T032 MerchantScopeInnerInterceptor
T034 PayNotifyController 系统模式

# 开发者 C — US4 数据层（待 T039 迁移后）
T039 V4__cashier_security_audit.sql
T040 SecurityAuditEntity
T043 admin SecurityAuditMapper
```

---

## Parallel Example: User Story 4 前端与后端

```bash
# 后端完成后并行：
T045 AdminSecurityAuditController
T046 securityAudit.ts
T047 security-audit.vue
```

---

## Implementation Strategy

### MVP First（仅 User Story 1）

1. Phase 1 + Phase 2
2. Phase 3（US1）+ T013–T014 测试
3. **停止并验证**：quickstart §2（403 + 5101）
4. 可先发 hotfix 封住最严重创建订单越权

### 推荐完整交付顺序

1. Setup + Foundational
2. US1 → US2 → US3（US2/US3 可双人并行）
3. US4（审计全栈）
4. US5（≥30 安全测试 + 兼容）
5. Polish（CONTRACT_MATRIX、ArchUnit、全量 test）

### 工期对齐（plan.md）

| Wave | 任务 ID 范围 | 日历 |
|------|----------------|------|
| Wave A | T004–T028, T013–T020 | 第 1–3 天 |
| Wave B | T029–T036 | 第 4–5 天 |
| Wave C | T037–T049 | 第 6–8 天 |
| Wave D | T050–T059 | 第 9–10 天 |

---

## Task Summary

| 阶段 | 任务数 | 说明 |
|------|--------|------|
| Phase 1 Setup | 3 | T001–T003 |
| Phase 2 Foundational | 9 | T004–T012 |
| US1 | 6 | T013–T018 |
| US2 | 10 | T019–T028 |
| US3 | 8 | T029–T036 |
| US4 | 13 | T037–T049 |
| US5 | 4 | T050–T053 |
| Polish | 6 | T054–T059 |
| **合计** | **59** | |

**并行机会**: Foundational 完成后 US2 与 US3 可并行；US4 的 admin 前后端可并行（T045–T047）；多个 `[P]` 测试任务可并行编写。

**建议 MVP 范围**: Phase 1 + Phase 2 + Phase 3（US1）+ T013–T014 + quickstart §2。

**独立验收标准**: 各 Phase「Checkpoint」与 spec.md 中 US1–US5 的 Independent Test 一一对应。
