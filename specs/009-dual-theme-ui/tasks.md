# Tasks: 双端统一主题与 UI 体验优化

**Input**: Design documents from `/specs/009-dual-theme-ui/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ui-theme-contract.md, quickstart.md

**Tests**: 规格与宪法要求 UI 变更按需 Playwright 验证；包含 E2E 与 quickstart 走查任务（非 TDD 单元测试）。

**Organization**: 任务按用户故事分组；实现仅限 `payflow-admin-client` 与 `payflow-cashier-client`，无后端变更。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行执行（不同文件，无未完成依赖）
- **[Story]**: US1–US5 对应 spec.md 用户故事

---

## Phase 1: Setup（共享准备）

**Purpose**: 确认分支、契约与扫页基线，无阻断再进入 Foundational。

- [x] T001 确认当前分支为 `009-dual-theme-ui` 且 `specs/009-dual-theme-ui/plan.md` 与 `spec.md` 已对齐
- [x] T002 [P] 通读 UI 契约并摘录表格标记要求到实施备注 `specs/009-dual-theme-ui/contracts/ui-theme-contract.md`
- [x] T003 [P] 运行 `rg "<el-table" payflow-admin-client/src/pages/admin --glob "*.vue" -l` 生成待改页面清单（附于 PR 描述或 `specs/009-dual-theme-ui/quickstart.md` 注释）

---

## Phase 2: Foundational（阻断性前置 — 路径 A）

**Purpose**: 设计令牌、`tableDensity` Store 与启动初始化；**所有用户故事依赖本阶段**。

**CRITICAL**: 完成前不得开始 US1–US5。

- [x] T004 在 `payflow-admin-client/src/styles/themes.css` 为 `mint`/`ocean`/`violet`/`dark` 增加 `--pf-table-stripe`、`--pf-table-stripe-strong`、`--pf-table-cell-py`、`--pf-table-cell-py-compact`、`--pf-table-header-py`、`--pf-table-header-py-compact`
- [x] T005 在 `payflow-admin-client/src/style.css` 扩展 `.data-table`：`[data-table-density='compact']` 密度规则、斑马纹引用新 Token、保持 fixed 列与 hover 不透明逻辑
- [x] T006 [P] 审计并修正 `payflow-admin-client/src/styles/dark-theme-overrides.css` 中与表格 Token 冲突的硬编码（表头/行/输入框）
- [x] T007 新增 `payflow-admin-client/src/stores/tableDensity.ts`（`standard`|`compact`、`localStorage` 键 `adminTableDensity`、`apply()` 设置 `data-table-density`）
- [x] T008 在 `payflow-admin-client/src/App.vue` 启动时调用 `useThemeStore().init()` 与 `useTableDensityStore().init()`
- [x] T009 [P] 在 `payflow-admin-client/src/main.ts` 确认已引入 `themes.css`、`style.css`、`dark-theme-overrides.css` 顺序正确

**Checkpoint**: 切换 `data-theme` / `data-table-density` 后 CSS 变量与文档根属性生效。

---

## Phase 3: User Story 1 - 管理端表格全站风格统一（Priority: P1）🎯 MVP

**Goal**: 全站 `.data-table` 视觉一致；清新轻斑马纹、暗夜略强斑马纹；标准/紧凑密度可即时生效；暗夜无吞字。

**Independent Test**: 按 `specs/009-dual-theme-ui/quickstart.md` §1–§2：8+ 表格路由在 4 主题 × 2 密度下表头/行/hover/分页一致；暗夜金额列与状态标签清晰可辨。

### Implementation for User Story 1

- [x] T010 [P] [US1] 为 `payflow-admin-client/src/pages/admin/orders/index.vue` 的 `el-table` 补齐 `class="data-table"`、`stripe`、`table-layout="auto"`
- [x] T011 [P] [US1] 为 `payflow-admin-client/src/pages/admin/merchants.vue` 全部 `el-table` 补齐 `data-table` + `stripe`
- [x] T012 [P] [US1] 为 `payflow-admin-client/src/pages/admin/refunds.vue` 补齐 `data-table` + `stripe`
- [x] T013 [P] [US1] 为 `payflow-admin-client/src/pages/admin/reconcile/tasks.vue` 与 `reconcile/results.vue` 补齐 `data-table` + `stripe`
- [x] T014 [P] [US1] 为 `payflow-admin-client/src/pages/admin/reconcile/summary.vue` 补齐 `data-table` + `stripe`
- [x] T015 [P] [US1] 为 `payflow-admin-client/src/pages/admin/dashboard.vue` 内嵌表格补齐 `data-table` + `stripe`
- [x] T016 [P] [US1] 为 `payflow-admin-client/src/pages/admin/users.vue`、`roles.vue`、`menus.vue` 补齐 `data-table` + `stripe`
- [x] T017 [P] [US1] 为 `payflow-admin-client/src/pages/admin/audit-logs.vue`、`security-audit.vue`、`RoutingLogs.vue` 补齐 `data-table` + `stripe`
- [x] T018 [P] [US1] 为 `payflow-admin-client/src/pages/admin/risk.vue`、`ChurnAlerts.vue`、`channel-routes.vue` 补齐 `data-table` + `stripe`
- [x] T019 [P] [US1] 为 `payflow-admin-client/src/pages/admin/payment-accounts.vue`、`payment-methods.vue`、`channels.vue` 补齐 `data-table` + `stripe`
- [x] T020 [P] [US1] 为 `payflow-admin-client/src/pages/admin/FeeRateConfig.vue`、`FeeRateAuditLog.vue`、`onboarding.vue` 补齐 `data-table` + `stripe`
- [x] T021 [P] [US1] 为 `payflow-admin-client/src/pages/admin/data-isolation.vue`、`search.vue`、`dicts.vue`、`settings.vue` 补齐 `data-table` + `stripe`
- [x] T022 [US1] 清除上述页面表格区域内 Tailwind 硬编码色（如 `text-blue-600`），改用语义类或 `var(--pf-*)`（优先订单/对账/审计页）
- [x] T023 [US1] 在 `payflow-admin-client/src/style.css` 统一 `.page-table-shell` 下分页、空态、`el-table__empty-text` 的主题色
- [x] T024 [US1] 约束行内 `el-tag`/`el-button` 在紧凑密度下不撑破行高（`style.css` 或 `.data-table` 子选择器）
- [ ] T025 [US1] 执行暗夜对比度走查并调优 Token（订单/对账/审计页），满足 FR-018 / SC-010，记录于 PR
- [ ] T026 [US1] 按 `quickstart.md` 表格 12 路由清单完成人工抽检并勾选契约 §7

**Checkpoint**: US1 可独立演示；表格为重中之重验收通过。

---

## Phase 4: User Story 2 - 管理端主题切换后全局风格一致（Priority: P1）

**Goal**: 四主题全局一致；外观页可配置主题与表格密度；顶栏与设置页状态同源。

**Independent Test**: 切换 mint/ocean/violet/dark 浏览登录、仪表盘、订单、设置；`preferences` 修改密度全站即时生效；刷新后偏好恢复。

### Implementation for User Story 2

- [x] T027 [US2] 新增 `payflow-admin-client/src/pages/admin/preferences.vue`（主题四选一 + 表格密度二选一 + 说明文案）
- [x] T028 [US2] 在 `payflow-admin-client/src/router/index.ts` 注册路由 `/admin/preferences`（名称：外观与显示）
- [x] T029 [US2] 在 `payflow-admin-client/src/pages/admin/layout.vue` 增加「外观与显示」入口（侧栏或用户区），与顶栏 Brush 共用 `useThemeStore`/`useTableDensityStore`
- [ ] T030 [P] [US2] 扫描并修正 `payflow-admin-client/src/pages/login/index.vue` 硬编码色，绑定 `data-theme` Token
- [ ] T031 [P] [US2] 扫描 `payflow-admin-client/src/pages/admin/layout.vue` 侧栏/顶栏漏色（非 dark 预设的 slate 硬编码）
- [ ] T032 [P] [US2] 扫描弹窗/抽屉全局样式 `payflow-admin-client/src/styles/dark-theme-overrides.css` 与 `light-theme-soft.css` 一致性
- [x] T033 [US2] 本地存储不可用时在 `preferences.vue` 或 store 内增加一次性提示（不阻断操作）

**Checkpoint**: US2 与 US1 叠加后主题+密度+表格协同无误。

---

## Phase 5: User Story 3 - 收银台与后台设计语言对齐（Priority: P1）

**Goal**: 收银台清新气质与管理端 mint 同源；暗夜（若启用）与管理端 dark 性格一致。

**Independent Test**: 并排打开管理端 mint 与收银台支付页；对比主色、圆角、按钮；窄屏 390px 支付主路径可走通。

### Implementation for User Story 3

- [x] T034 [P] [US3] 新增或扩展 `payflow-cashier-client/src/styles/brand-tokens.css`（从 admin `themes.css` 抽取 mint/dark 核心变量）
- [x] T035 [US3] 在 `payflow-cashier-client/src/styles/main.css` 引入 `brand-tokens.css` 并绑定收银台主色/金额色
- [ ] T036 [P] [US3] 对齐 `payflow-cashier-client/src/pages/cashier/index.vue`（或主入口）头部与金额区配色
- [ ] T037 [P] [US3] 对齐 `payflow-cashier-client/src/pages/cashier/components/OrderCard.vue` 与 `CashierNav.vue` 品牌色
- [ ] T038 [US3] 对齐支付结果/失败态组件配色与圆角（`payflow-cashier-client/src/pages/cashier/**`）
- [ ] T039 [US3] 若收银台含表格/列表，应用与管理端一致的轻/强斑马纹规则（`payflow-cashier-client/src/styles/main.css`）
- [ ] T040 [US3] 移动端 390px 走查支付主路径无遮挡（SC-005）

**Checkpoint**: US3 可独立于 US4 验收品牌一致性。

---

## Phase 6: User Story 4 - 跨页面与跨组件的视觉协调（Priority: P2）

**Goal**: 图表、空态、加载态随主题更新；mint/ocean/violet 图表不再共用一套浅色。

**Independent Test**: 仪表盘切换四主题，图表轴线/Tooltip 同步变化；空列表页无默认灰白块。

### Implementation for User Story 4

- [x] T041 [US4] 扩展 `payflow-admin-client/src/utils/chartTheme.ts` 按 `mint|ocean|violet|dark` 返回 `ChartThemeColors`
- [x] T042 [US4] 在 `payflow-admin-client/src/pages/admin/dashboard.vue` 监听 `themeStore.themeKey` 并刷新 ECharts `setOption`
- [ ] T043 [P] [US4] 检查其他使用 ECharts 的页面并接入 `getChartTheme()`（如有 `MerchantRanking.vue` 等）
- [ ] T044 [US4] 统一仪表盘/列表空态与 `el-skeleton` 遮罩色为 Token（`style.css` 或组件层）

**Checkpoint**: US4 完成；次要组件无主题漏网。

---

## Phase 7: User Story 5 - 主题切换过程体验流畅（Priority: P3）

**Goal**: 主题/密度切换 1s 内生效；无 FOUC；翻页后表格样式正确。

**Independent Test**: 订单列表页快速切换 4 主题各 3 次；切换后立刻翻页/筛选。

### Implementation for User Story 5

- [x] T045 [US5] 在 `payflow-admin-client/src/stores/theme.ts` 的 `apply()` 确保同步设置 `color-scheme`（dark 主题）
- [ ] T046 [US5] 避免主题切换时整页 remount；确认 `layout.vue` 中 `router-view` 过渡不导致表格长时间无样式
- [ ] T047 [US5] 验证 `tableDensityStore.setDensity` 后已挂载 `el-table` 行高即时更新（必要时 `key` 或 CSS 变量刷新）

**Checkpoint**: US5 体验项满足 SC-002/008/009 主观与计时标准。

---

## Phase 8: Polish & Cross-Cutting（路径 F）

**Purpose**: E2E、文档、合规章节验收。

- [ ] T048 [P] 新增 Playwright 场景 `payflow-admin-client/tests/theme-preferences.spec.ts`（主题切换 + 外观页密度持久化）
- [ ] T049 [P] 扩展或新增 `payflow-admin-client/tests/` 订单列表表格可见性断言（暗夜主题）
- [ ] T050 运行 `cd payflow-admin-client && npx playwright test` 并修复失败用例
- [ ] T051 运行 `cd payflow-cashier-client && npx playwright test` 回归支付主路径
- [ ] T052 执行 `specs/009-dual-theme-ui/quickstart.md` 全路径并记录 SC-001~010 证据
- [ ] T053 [P] 若需文档化新路由，更新 `docs/CONTRACT_MATRIX.md` 中 `/admin/preferences` 说明
- [ ] T054 合规章节自检：确认无后端/API/数据库变更，宪法 1/6/9 相关项通过

---

## Dependencies & Execution Order

### Phase Dependencies

```text
Setup (Phase 1)
  → Foundational (Phase 2) 阻断一切
    → US1 表格 (Phase 3) MVP
      → US2 主题+外观页 (Phase 4) 依赖 T007–T009
    → US3 收银台 (Phase 5) 可与 US4 并行，建议在 US1 后
    → US4 图表 (Phase 6) 建议在 US2 后
    → US5 体验 (Phase 7) 依赖 US1–US2
  → Polish (Phase 8) 依赖 US1–US5
```

### User Story Dependencies

| 故事 | 依赖 | 可与谁并行 |
|------|------|------------|
| US1 | Phase 2 | —（优先完成） |
| US2 | Phase 2；与 US1 表格样式叠加测试 | US3（后期） |
| US3 | Phase 2；建议 US1 核心 Token 稳定后 | US4 |
| US4 | Phase 2；建议 US2 主题切换可用后 | US3 |
| US5 | US1 + US2 | — |

### Within Each User Story

- Foundational Token/Store 先于页面扫改
- 先全局 CSS 契约，再逐页 `el-table` 标记
- 收银台在 admin Token 定稿后对齐

---

## Parallel Example: User Story 1

```bash
# 多开发者同时扫不同页面（T010–T021 标 [P]）：
# 开发者 A: orders + merchants + refunds
# 开发者 B: reconcile/* + dashboard
# 开发者 C: users/roles/audit + payment-* + settings
```

## Parallel Example: Foundational + US2

```bash
# T006 dark-theme-overrides 与 T007 tableDensity.ts 可并行
# T030 login 与 T031 layout 扫色可并行（US2）
```

---

## Implementation Strategy

### MVP First（仅 User Story 1）

1. Phase 1 Setup  
2. Phase 2 Foundational（**必须**）  
3. Phase 3 US1（表格全站 + 暗夜对比度）  
4. **STOP**：按 `quickstart.md` §1–§2 验收 → 再进入 US2  

### Incremental Delivery

1. Foundational → US1（MVP 可演示表格统一）  
2. + US2（主题 + 外观偏好页）  
3. + US3（收银台品牌）  
4. + US4（图表/空态）  
5. + US5（切换性能）  
6. Polish（Playwright + quickstart 全量）

### Suggested MVP Scope

- **最小可交付**: Phase 2 + Phase 3（US1）  
- **产品可发布**: Phase 2–4（US1 + US2）  
- **完整特性**: Phase 2–8（含 US3–US5 与 Playwright）

---

## Task Summary

| Phase | 任务数 | 故事 |
|-------|--------|------|
| Setup | 3 | — |
| Foundational | 6 | — |
| US1 表格 | 17 | US1 |
| US2 主题 | 7 | US2 |
| US3 收银台 | 7 | US3 |
| US4 组件 | 4 | US4 |
| US5 体验 | 3 | US5 |
| Polish | 7 | — |
| **合计** | **54** | |

**并行机会**: US1 页面扫改（T010–T021）、Foundational（T006+T007）、US2 扫色（T030–T032）、Polish 测试（T048–T049）

**独立测试标准**: 见各 Phase「Independent Test」；总验收见 `quickstart.md` 通过标准。
