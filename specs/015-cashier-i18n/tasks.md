---
description: "收银台国际化（015-cashier-i18n）可执行任务列表"
---

# Tasks: 收银台国际化（简体中文 / 繁体中文 / 英文）

**Input**: `specs/015-cashier-i18n/`（plan.md、spec.md、research.md、data-model.md、contracts/、quickstart.md）  
**Prerequisites**: 已在分支 `015-cashier-i18n`；设计产物齐全  
**Tests**: spec 要求 Playwright E2E 与后台日志闭环 — 仅在 Polish 阶段包含 E2E 任务（非 TDD 先写测试）

**Organization**: 按用户故事分组；Foundational 阻断所有故事；US3 后端需在 US1 支付方式展示联调前完成。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行（不同文件、无未完成依赖）
- **[Story]**: US1–US5 对应 spec.md 用户故事

## Path Conventions

| 模块 | 源码路径 |
|------|----------|
| payflow-cashier-server | `payflow-cashier-server/src/main/java/com/payflow/cashier/` |
| payflow-admin-server | `payflow-admin-server/src/main/java/com/payflow/admin/` |
| payflow-admin-client | `payflow-admin-client/src/` |
| payflow-cashier-client | `payflow-cashier-client/src/` |
| SQL | `sql/migrations/`、`sql/schema/`、`sql/seed/` |

---

## Phase 1: Setup（准备）

**Purpose**: 确认范围与设计输入，无代码变更

- [x] T001 阅读 `specs/015-cashier-i18n/spec.md` 与 `plan.md`，确认五则澄清结论（订单语言无切换器、门户有切换器、三语必填、per-language 列）
- [x] T002 [P] 阅读 `specs/015-cashier-i18n/contracts/` 下 4 份契约与 `data-model.md` 列命名约定

---

## Phase 2: Foundational（阻断性前置）

**Purpose**: 数据库与共享工具 — **本阶段完成前不得开始用户故事**

- [x] T003 编写迁移脚本 `sql/migrations/2026-06-03_cashier_i18n.sql`：`admin_payment_methods` 增加 6 列；`cashier_orders` 增加 `display_language`；回填 `method_name`→`method_name_zh_cn` 等；繁/英列暂复制简体
- [x] T004 [P] 同步全量 Schema：`sql/schema/payflow_admin.sql` 中 `admin_payment_methods` 定义
- [x] T005 [P] 同步全量 Schema：`sql/schema/payflow_cashier.sql` 中 `cashier_orders.display_language`
- [x] T006 [P] 更新演示种子 `sql/seed/payflow_admin_seed.sql`（支付方式三语占位与 `method_name` 同步）
- [x] T007 本地执行迁移或 `python scripts/install_demo_db.py` 验证 DDL 成功
- [x] T008 新增 `payflow-cashier-server/src/main/java/com/payflow/cashier/util/DisplayLocale.java`（`SUPPORTED`、`normalize` 非法回 `zh-CN`）
- [x] T009 [P] 扩展实体 `payflow-admin-server/src/main/java/com/payflow/admin/entity/PaymentMethod.java`（6 个多语言字段 + `@TableField` 映射）
- [x] T010 [P] 扩展实体 `payflow-cashier-server/src/main/java/com/payflow/cashier/entity/Order.java` 增加 `displayLanguage` 映射 `display_language`

**Checkpoint**: 库表与 `DisplayLocale` 就绪

---

## Phase 3: User Story 3 - 后台支付方式多语言配置 (Priority: P1)

**Goal**: 运营在后台为三语录入支付方式展示名与描述；内部接口按 `locale` 返回单语言文案

**Independent Test**: 后台编辑支付方式 — 缺任一语言保存失败；三语保存成功；`GET /api/v1/internal/cashier/payment-methods?locale=zh-TW` 返回繁体 `methodName`

### Implementation for User Story 3

- [x] T011 [P] [US3] 新增 `payflow-admin-server/src/main/java/com/payflow/admin/kit/LocalizedTextResolver.java`（`resolveMethodName` / `resolveDescription` 按 locale 选列）
- [x] T012 [US3] 在 `payflow-admin-server/src/main/java/com/payflow/admin/service/impl/PaymentMethodServiceImpl.java` 增加 `validateLocalizedFields`（六字段非空）；create/update 同步 `methodName`/`description`  legacy 列
- [x] T013 [US3] 扩展 `payflow-admin-server/src/main/java/com/payflow/admin/service/CashierPaymentConfigService.java` 接口：`listPaymentMethodsForCashier(merchantId, orderChannel, locale)`
- [x] T014 [US3] 更新 `payflow-admin-server/src/main/java/com/payflow/admin/service/impl/CashierPaymentConfigServiceImpl.java` 使用 `LocalizedTextResolver` 输出解析后的 `methodName`/`description`
- [x] T015 [US3] 更新 `payflow-admin-server/src/main/java/com/payflow/admin/controller/InternalCashierPaymentController.java` 增加 `@RequestParam(required = false) String locale` 并传入 Service
- [x] T016 [P] [US3] 确认 `payflow-admin-server/src/main/java/com/payflow/admin/controller/PaymentMethodController.java` 创建/更新走校验（必要时在 Service 层统一拦截）
- [x] T017 [P] [US3] 扩展 `payflow-admin-client/src/types/index.ts` 中 `PaymentMethod` 接口（`methodNameZhCn` 等 6 字段）
- [x] T018 [US3] 改造 `payflow-admin-client/src/pages/admin/payment-methods.vue`：展示名/描述各 3 个输入框；`FormRules` 六字段 required；列表默认显示简体名称；详情展示三语
- [x] T019 [US3] 调整 `payflow-admin-client/src/pages/admin/payment-methods.vue` 提交 payload（create/update）映射 camelCase 多语言字段

**Checkpoint**: 后台三语 CRUD + 内部接口 locale 可用

---

## Phase 4: User Story 1 - 收银台按下单语言展示 (Priority: P1) 🎯 MVP

**Goal**: 商户下单传 `language`，收银台/收据按订单语言展示，**无**用户可见语言切换器

**Independent Test**: `language=zh-TW` 下单 → 打开 `payUrl` → 界面繁体、无切换器；刷新仍为繁体；支付方式名为后台繁体配置

### Implementation for User Story 1

- [x] T020 [P] [US1] 扩展 `payflow-cashier-server/src/main/java/com/payflow/cashier/dto/CreateOrderRequest.java` 增加 `language` 字段
- [x] T021 [US1] 更新 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/OrderServiceImpl.java` 的 `createOrder`：`DisplayLocale.normalize` 后写入 `Order.displayLanguage`
- [x] T022 [P] [US1] 扩展 `payflow-cashier-server/src/main/java/com/payflow/cashier/dto/CashierResponse.java` 增加 `displayLanguage`
- [x] T023 [US1] 更新 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/OrderServiceImpl.java` 的 `getCashierInfo` 返回 `displayLanguage`（含已支付分支）
- [x] T024 [US1] 扩展 `payflow-cashier-server/src/main/java/com/payflow/cashier/client/AdminPaymentConfigClient.java`：`fetchPaymentMethods(merchantId, orderChannel, locale)` 追加 query `locale`
- [x] T025 [US1] 更新 `OrderServiceImpl.resolvePaymentMethods` 传入 `order.getDisplayLanguage()` 调用内部接口
- [x] T026 [P] [US1] 扩展 `payflow-cashier-client/src/types/index.ts` 中 `CashierInfo.displayLanguage`
- [x] T027 [P] [US1] 更新 `payflow-cashier-client/src/api/cashier.ts` 保留 `displayLanguage` 字段透传
- [x] T028 [US1] 新增 `payflow-cashier-client/src/composables/useDisplayLocale.ts`（`applyLocale`：vue-i18n + Element Plus + `document.documentElement.lang`；订单页不写 `localStorage`）
- [x] T029 [US1] 更新 `payflow-cashier-client/src/composables/useCashierCheckout.ts`：`getCashierInfo` 成功后 `applyLocale(data.displayLanguage ?? 'zh-CN')`
- [x] T030 [US1] 确认收银台订单路由（`payflow-cashier-client/src/pages/cashier/`、`receipt/index.vue`）**不**挂载语言切换器；必要时在 `payflow-cashier-client/src/router/index.ts` 增加 `meta.portal` 区分

**Checkpoint**: 下单语言驱动收银台展示（依赖 US3 内部接口）

---

## Phase 5: User Story 2 - 三语全量静态文案覆盖 (Priority: P1)

**Goal**: `zh-CN` / `zh-TW` / `en-US` 前端语言包键 100% 对齐，无键名泄漏

**Independent Test**: 三种语言遍历收银台/门户页面，无 `cashier.xxx` 键名显示；繁体用词正确（如「支付寶」）

### Implementation for User Story 2

- [x] T031 [P] [US2] 新增 `payflow-cashier-client/src/locales/zh-TW.ts`（与 `zh-CN.ts` 键对齐，台湾繁体译文）
- [x] T032 [US2] 更新 `payflow-cashier-client/src/locales/index.ts` 与 `payflow-cashier-client/src/i18n.ts` 注册 `zh-TW`
- [x] T033 [P] [US2] 更新 `payflow-cashier-client/src/element-plus-locale.d.ts` 声明 `element-plus/dist/locale/zh-tw`（或项目实际路径）
- [x] T034 [US2] 在 `payflow-cashier-client/src/composables/useDisplayLocale.ts` 为 `zh-TW` 加载 Element Plus 繁体 locale（不可用时记录回退 `zh-cn`）
- [x] T035 [US2] 对照 `payflow-cashier-client/src/locales/zh-CN.ts` 与 `en-US.ts`、`zh-TW.ts` 校验键集合一致（脚本或手工 diff）
- [x] T036 [P] [US2] 更新 `payflow-cashier-client/src/router/index.ts` 路由 `title` 使用 `t()` 且随 `applyLocale` 刷新

**Checkpoint**: 三语静态 UI 完整

---

## Phase 6: User Story 4 - 门户页面语言自适应与切换 (Priority: P2)

**Goal**: 登录/入驻/入驻结果页：浏览器自动判定 + 手动切换器 + `localStorage` 持久化

**Independent Test**: 英文浏览器首次打开 `/login` 为英文；切换繁体刷新保持；订单页不受影响

### Implementation for User Story 4

- [x] T037 [P] [US4] 新增 `payflow-cashier-client/src/components/LocaleSwitcher.vue`（三语选项，写入 `payflow-cashier-locale`）
- [x] T038 [US4] 在 `payflow-cashier-client/src/composables/useDisplayLocale.ts` 实现 `detectBrowserLocale()`（`zh`→简体/繁体启发式，`en`→`en-US`）
- [x] T039 [P] [US4] 挂载 `LocaleSwitcher` 至 `payflow-cashier-client/src/pages/login/index.vue`
- [x] T040 [P] [US4] 挂载 `LocaleSwitcher` 至 `payflow-cashier-client/src/pages/register/index.vue` 与 `payflow-cashier-client/src/pages/onboarding/result.vue`
- [x] T041 [US4] 更新 `payflow-cashier-client/src/main.ts`：门户路由首次加载时 `detectBrowserLocale` + `localStorage`；订单路由不覆盖已设 locale
- [x] T042 [US4] 门户页切换时调用 `applyLocale` 并持久化 `LOCALE_STORAGE_KEY`（`payflow-cashier-client/src/i18n.ts`）

**Checkpoint**: 门户与订单语言策略分离

---

## Phase 7: User Story 5 - 语言与页面元数据同步 (Priority: P2)

**Goal**: `<html lang>` 与标签页标题与当前语言一致

**Independent Test**: 英文订单页 `document.documentElement.lang === 'en'`；繁体为 `zh-TW`

### Implementation for User Story 5

- [x] T043 [US5] 在 `payflow-cashier-client/src/composables/useDisplayLocale.ts` 完善 `htmlLangFor(locale)` 映射（`en-US`→`en`）
- [x] T044 [US5] 在 `payflow-cashier-client/src/router/index.ts` 的 `afterEach` 根据当前 locale 设置 `document.title`（复用 `router.*` i18n 键）

**Checkpoint**: 元数据与语言一致

---

## Phase 8: Polish & Cross-Cutting（收尾）

**Purpose**: 文档、编译、E2E、宪法合规

- [x] T045 [P] 更新 `docs/CONTRACT_MATRIX.md`：`POST /orders` 的 `language`、`GET /cashier/{id}` 的 `displayLanguage`、支付方式多语言字段、内部接口 `locale`
- [x] T046 运行 `mvn -B -pl payflow-cashier-server,payflow-admin-server -DskipTests compile` 确认编译通过
- [x] T047 按 `specs/015-cashier-i18n/quickstart.md` 执行手工验收（三语下单 + 后台保存拦截）
- [x] T048 扩展 `payflow-cashier-client/e2e/cashier-smoke.spec.ts`：覆盖 `displayLanguage` 或门户 `LocaleSwitcher` 至少一条断言
- [x] T049 运行 `cd payflow-cashier-client && npx playwright test`，监控 `payflow-cashier-server` 日志至支付主路径无阻断 ERROR
- [x] T050 宪法合规自检：模块边界、API `R` 格式、`language` 白名单、admin/cashier 表前缀（对照 `specs/015-cashier-i18n/plan.md` Constitution Check）

---

## Dependencies & Execution Order

### Phase Dependencies

```text
Setup (1) → Foundational (2) → US3 (3) → US1 (4) ─┬→ US2 (5) 可并行
                                                    ├→ US4 (6) 依赖 US2 之 useDisplayLocale
                                                    └→ US5 (7) 依赖 US1/US4 之 applyLocale
Polish (8) → 全部故事完成后
```

### User Story Dependencies

| 故事 | 依赖 | 说明 |
|------|------|------|
| US3 | Foundational | 实体列 + 迁移必须先有 |
| US1 | US3 + Foundational | 支付方式名依赖内部接口 locale |
| US2 | Foundational | 可与 US1 前端并行（T031–T036 与 T028–T030） |
| US4 | US2 之 `useDisplayLocale` | 复用 composable |
| US5 | US1/US4 | 元数据随 `applyLocale` |

### Within-Story Order

- 后端：DTO/实体 → Service → Controller/Client
- 前端：types → composable → 页面集成
- admin-client US3 可与 cashier-server US1（T020–T025）并行，但联调需 T014–T015 完成

---

## Parallel Example

### Foundational 并行

```bash
# 同时进行（不同 SQL/Java 文件）：
T004 sql/schema/payflow_admin.sql
T005 sql/schema/payflow_cashier.sql
T009 PaymentMethod.java
T010 Order.java
```

### US1 + US2 前端并行（US3 后端已就绪）

```bash
T031 locales/zh-TW.ts
T028 composables/useDisplayLocale.ts
T026 types/index.ts
```

### US4 门户并行

```bash
T039 login/index.vue
T040 register/index.vue + onboarding/result.vue
```

---

## Implementation Strategy

### MVP First（最小可演示）

1. Phase 1–2：迁移 + `DisplayLocale` + 实体  
2. Phase 3（US3）：后台三语 + 内部接口  
3. Phase 4（US1）+ Phase 5（US2）之 T031–T032：下单语言 + 繁体包  
4. **STOP**：按 quickstart 验证 `language=zh-TW` 收银台  
5. 再完成 US4、US5、Polish  

### Incremental Delivery

| 增量 | 交付价值 |
|------|----------|
| Foundational + US3 | 后台可配三语支付方式 |
| + US1 + US2 | 商户控制收银台语言（MVP） |
| + US4 | 门户自助切换 |
| + US5 + Polish | 可访问性 + 文档 + E2E |

---

## Task Summary

| 指标 | 数量 |
|------|------|
| **总任务数** | 50 |
| Setup | 2 |
| Foundational | 8 |
| US3 | 9 |
| US1 | 11 |
| US2 | 6 |
| US4 | 6 |
| US5 | 2 |
| Polish | 6 |

**Suggested MVP scope**: T001–T032 + T020–T030（Foundational + US3 + US1 + zh-TW 注册）

**Format validation**: 全部 50 项均符合 `- [x] Txxx [P?] [USn?] 描述含路径` 格式
