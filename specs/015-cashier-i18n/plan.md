# Implementation Plan: 收银台国际化（简体中文 / 繁体中文 / 英文）

**Branch**: `015-cashier-i18n` | **Date**: 2026-06-03 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/015-cashier-i18n/spec.md`

## Summary

在现有 `vue-i18n` 基线上，将收银台国际化扩展为**全栈能力**：

1. **商户下单驱动语言**：`POST /orders` 新增 `language`，持久化至 `cashier_orders.display_language`，收银台/收据只读展示、无切换器。
2. **后台支付方式三语配置**：`admin_payment_methods` 增加 6 个 per-language 列，管理后台表单三语录入且全部必填。
3. **收银台按语言解析支付方式名**：内部接口与 `CashierResponse` 返回已解析的单语言 `name`/`description`。
4. **门户页独立策略**：登录/入驻保留浏览器判定 + `LocaleSwitcher`。
5. **补齐 `zh-TW` 语言包**与 Element Plus 运行时 locale 切换。

技术结论见 [research.md](./research.md)；数据模型见 [data-model.md](./data-model.md)；接口契约见 [contracts/](./contracts/)。

## Technical Context

**Language/Version**: Java 17、TypeScript 5.3、Vue 3.4  
**Primary Dependencies**: Spring Boot 3.2.5、MyBatis-Plus 3.5.7、vue-i18n 11.4、Element Plus 2.6、Pinia 2.1  
**Storage**: MySQL `payflow_admin`（`admin_payment_methods` 扩展）、`payflow_cashier`（`cashier_orders.display_language`）  
**Testing**: Playwright（cashier-client）、手工 API + 后台日志闭环；Java 单元测试按需（`DisplayLocale`、校验器）  
**Target Platform**: Linux JVM 17 + 现代浏览器  
**Project Type**: Maven 多模块 Web（2 后端 + 2 前端）  
**Performance Goals**: 语言切换/首屏渲染 < 1s（SC-001）  
**Constraints**: 不新增支付渠道；不改动支付 SPI；API 保持 `R<T>`  
**Scale/Scope**: 3 语言 × 收银台 ~15 页面/组件 + 1 个 admin 表单 + 4 个后端触点

## Constitution Check

*GATE: Phase 0 研究前 — 已通过；Phase 1 设计后复查如下。*

| # | 宪法原则 | 状态 | 说明 |
|---|----------|------|------|
| 1 | I. 模块边界纪律 | PASS | cashier-server 管订单语言；admin-server 管支付方式多语言与内部接口；各前端只改本 client。可选在 `payflow-common` 增加 `DisplayLocale` 常量供复用。 |
| 2 | II. 支付渠道抽象 | N/A | 不触碰 PayStrategy / Handler。 |
| 3 | III. 数据库分区 | PASS | `admin_*` 列扩展 admin 库；`cashier_orders.display_language` 为 cashier 库必要字段（修正 spec 初稿「不涉及 cashier 库」）。 |
| 4 | IV. API 响应规范 | PASS | 扩展字段，仍 `R` / `Map{code,message,data}`。 |
| 5 | V. 密钥与配置安全 | N/A | 无密钥变更。 |
| 6 | 编码规范 | PASS | 中文 Javadoc/注释；camelCase API / snake_case DB。 |
| 7 | 数据库访问 | PASS | 显式列映射；迁移脚本可回滚；禁止 `${}`。 |
| 8 | 安全编码 | PASS | `language` 白名单；非法值回退；日志不记敏感信息。 |
| 9 | 测试规范 | PASS | quickstart + Playwright 扩展；支付主路径三语回归。 |

**Gate Result**: **ALL PASS**

## Project Structure

### Documentation (this feature)

```text
specs/015-cashier-i18n/
├── plan.md              # 本文件
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1
├── contracts/           # Phase 1
│   ├── create-order.md
│   ├── cashier-info.md
│   ├── admin-payment-method.md
│   └── internal-cashier-payment-methods.md
└── tasks.md             # /speckit-tasks 输出
```

### Source Code (touch map)

| 模块 | 改动概要 |
|------|----------|
| `sql/migrations/` + `sql/schema/` + `sql/seed/` | DDL、历史数据回填、演示种子 |
| `payflow-cashier-server` | `CreateOrderRequest`、`Order`、`CashierResponse`、`DisplayLocale`、`OrderServiceImpl`、`AdminPaymentConfigClient` |
| `payflow-admin-server` | `PaymentMethod` 实体、校验、`PaymentMethodService`、`CashierPaymentConfigServiceImpl`、`InternalCashierPaymentController` |
| `payflow-admin-client` | `payment-methods.vue` 三语表单、`types`、API payload |
| `payflow-cashier-client` | `zh-TW.ts`、`useDisplayLocale`、`LocaleSwitcher`（门户）、订单页去切换器、`CashierInfo.displayLanguage` |
| `docs/CONTRACT_MATRIX.md` | 补充 `language` / `displayLanguage` / 多语言字段 |

**Structure Decision**: 不新增 Maven 模块；复用现有四端与内部 HTTP 集成。

## Implementation Phases

> 详细任务拆解由 `/speckit-tasks` 生成；以下为推荐实施顺序与要点。

### Phase A — 数据库与种子（P0）

1. 新增迁移 `sql/migrations/2026-06-03_cashier_i18n.sql`（见 [data-model.md](./data-model.md)）。
2. 更新 `sql/schema/payflow_admin.sql`、`payflow_cashier.sql`。
3. 回填：`method_name` → `method_name_zh_cn` 等；繁/英暂复制简体供演示，运营后台编辑时补全真实译文。
4. `cashier_orders.display_language DEFAULT 'zh-CN'`。

### Phase B — 后端公共与收银台服务（P0）

1. **`DisplayLocale`**（`payflow-cashier-server` 或 `payflow-common`）  
   - `normalize(String raw) → String`  
   - `SUPPORTED = Set.of("zh-CN","zh-TW","en-US")`
2. **`CreateOrderRequest`** + **`Order`** + **`OrderServiceImpl.createOrder`** 写入 `displayLanguage`。
3. **`CashierResponse`** + **`getCashierInfo`** 返回 `displayLanguage`。
4. **`AdminPaymentConfigClient`**: `fetchPaymentMethods(merchantId, channel, locale)`。
5. **`resolvePaymentMethods`**: 传入订单语言；DTO `name`/`description` 来自 admin 已解析值。
6. 内置 fallback 列表保持 channel 级 i18n 键（admin 不可用时的兜底）。

### Phase C — 管理后台服务（P0）

1. **`PaymentMethod`** 实体 6 字段 + MyBatis 映射。
2. **`PaymentMethodService`**: `validateLocalizedFields` 三语必填；create/update 同步 legacy `method_name`/`description`。
3. **`LocalizedTextResolver`**（或静态方法）：`resolveMethodName(pm, locale)` / `resolveDescription(pm, locale)`。
4. **`CashierPaymentConfigServiceImpl`**: 接受 `locale`，输出解析后的 `methodName`/`description`。
5. **`InternalCashierPaymentController`**: 新增 `@RequestParam locale`。

### Phase D — 管理后台前端（P1）

1. **`payment-methods.vue`** 表单：展示名/描述各 3 个 `el-input`（分组标签：简体中文 / 繁體中文 / English）。
2. **`FormRules`**: 六字段 required + trim。
3. **`types/index.ts`**: 扩展 `PaymentMethod` 接口；提交 payload camelCase。
4. 列表列默认显示 `methodNameZhCn`；详情弹窗展示三语。

### Phase E — 收银台前端（P0）

1. **`src/locales/zh-TW.ts`**: 自 `zh-CN` 翻译为台湾繁体；`locales/index.ts`、`i18n.ts` 注册。
2. **`useDisplayLocale.ts`**:
   - `applyLocale(locale)` → i18n + Element Plus + `document.documentElement.lang`
   - `detectBrowserLocale()` → 门户首次访问
3. **`useCashierCheckout`**: `getCashierInfo` 后 `applyLocale(data.displayLanguage ?? 'zh-CN')`；**不**写 `localStorage`。
4. **门户**（`login`/`register`/`onboarding`）: 挂载 `LocaleSwitcher`，沿用 `LOCALE_STORAGE_KEY`。
5. **订单路由**（`cashier/*`、`receipt`）: 确认无 `LocaleSwitcher`；路由 `beforeEach` 按 meta 区分。
6. **`element-plus-locale.d.ts`**: 声明 `zh-tw` 模块。
7. **`types/CashierInfo`**: `displayLanguage` 可选字段。
8. **`api/cashier.ts`**: 透传 `displayLanguage`。

### Phase F — 文档与联调（P1）

1. 更新 `docs/CONTRACT_MATRIX.md` 相关行。
2. 按 [quickstart.md](./quickstart.md) 手工验收。
3. 扩展 `payflow-cashier-client/e2e/cashier-smoke.spec.ts`（三语 snapshot 或文案断言）。

### Phase G — 可选后续（Out of scope）

- `payflow-sdk-java` 示例增加 `language` 字段。
- Admin 其它资源（渠道名）多语言。

## Complexity Tracking

无宪法豁免项。

## Post-Design Constitution Re-check

设计完成后复查：双库变更均有前缀约束；模块边界清晰；无支付核心侵入；测试路径已在 quickstart 定义。**可进入 `/speckit-tasks`。**
