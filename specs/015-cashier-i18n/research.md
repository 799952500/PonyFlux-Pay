# Research: 收银台国际化（015-cashier-i18n）

**Date**: 2026-06-03

## R1 — 订单展示语言如何传递到收银台

**Decision**: 在 `cashier_orders` 新增 `display_language` 列（`VARCHAR(16)`），下单时写入；`GET /cashier/{orderId}` 的 `CashierResponse` 返回 `displayLanguage` 字段。

**Rationale**:
- 规范要求语言由下单接口决定，付款人不可切换；必须持久化到订单，避免仅靠 URL 参数被篡改。
- `attach` 透传字段语义不匹配，且商户可随意填写。
- 与现有 `Order` 实体 / `OrderServiceImpl.createOrder` 流程一致，改动面可控。

**Alternatives considered**:
- **URL query `?lang=`**：易被修改，与「仅商户决定语言」冲突 → 拒绝。
- **仅存 Redis**：订单缓存 TTL 30 分钟，过期后收银台无法恢复语言 → 拒绝。

---

## R2 — 支付方式多语言存储与解析

**Decision**: 在 `admin_payment_methods` 增加 6 列 per-language 字段；保留原 `method_name` / `description` 列用于迁移期兼容（写入时同步 `method_name_zh_cn` → `method_name`）。收银台内部接口按 `locale` 查询参数解析展示字段。

**Rationale**:
- 澄清会话明确选择 per-language 列，非 JSON、非翻译表。
- `CashierPaymentConfigServiceImpl` 当前直接 `pm.getMethodName()`，改为 `LocalizedTextResolver` 按 locale 取列即可。
- 三语必填在 `PaymentMethodService.create/update` 统一校验。

**Alternatives considered**:
- **JSON `name_i18n` 列**：澄清已否决。
- **独立 `admin_i18n_texts` 表**：规范禁止 → 拒绝。

**列命名约定**:
| 逻辑字段 | 列名 |
|----------|------|
| 展示名-简体 | `method_name_zh_cn` |
| 展示名-繁体 | `method_name_zh_tw` |
| 展示名-英文 | `method_name_en` |
| 描述-简体 | `description_zh_cn` |
| 描述-繁体 | `description_zh_tw` |
| 描述-英文 | `description_en` |

---

## R3 — 受支持语言标识与白名单

**Decision**: 固定枚举 `zh-CN` | `zh-TW` | `en-US`；在 `payflow-cashier-server` 提供 `DisplayLocale` 工具类（normalize + isSupported），下单与内部调用统一使用。

**Rationale**:
- 与现有 `payflow-cashier-client` i18n locale 键一致。
- 非法值回退 `zh-CN`，不抛业务异常（符合 FR-003）。

**Alternatives considered**:
- **BCP47 短码 `zh`/`en`**：与 vue-i18n 及 Element Plus locale 键不一致 → 拒绝。

---

## R4 — 收银台前端语言应用策略

**Decision**: 新增 `useDisplayLocale` composable，封装：设置 `vue-i18n` locale、动态切换 Element Plus locale、`document.documentElement.lang`、路由 title。订单相关路由在 `getCashierInfo` 成功后调用 `applyLocale(response.displayLanguage)`，**不读取** `localStorage`。门户路由（login/register/onboarding）保留 `LocaleSwitcher` + `localStorage`。

**Rationale**:
- 现有 `main.ts` 仅在启动时设置 Element Plus 语言，无法满足订单页按 API 切换。
- `PaymentMethodList` 已直接渲染后端 `methodName`，静态 UI 走 i18n 即可分工清晰。

**Alternatives considered**:
- **订单页也保留切换器**：澄清已否决。

---

## R5 — Element Plus 繁体中文

**Decision**: 引入 `element-plus/dist/locale/zh-tw.mjs`（或 `zh-tw` 等价路径）作为 `zh-TW` 的组件库语言包；若构建路径不可用则回退 `zh-cn` 并记录技术债（实现阶段验证）。

**Rationale**: Element Plus 官方提供繁体 locale，与 `en`、`zh-cn` 模式一致。

---

## R6 — 数据迁移与种子数据

**Decision**: 新增迁移脚本 `sql/migrations/2026-06-03_cashier_i18n.sql`：
1. `ALTER admin_payment_methods` 增加 6 列；
2. `UPDATE` 将现有 `method_name` → `method_name_zh_cn`，`description` → `description_zh_cn`，英文/繁体暂复制简体（运营后续在后台补全繁体/英文，**保存时仍须三语必填**——迁移后首次编辑需运营补录）；
3. `ALTER cashier_orders` 增加 `display_language DEFAULT 'zh-CN'`；
4. 同步更新 `sql/schema/payflow_admin.sql`、`payflow_cashier.sql` 与 `sql/seed` 演示数据。

**Rationale**: 避免升级后内部接口返回空名称；历史订单默认 `zh-CN`。

**Note**: 种子/迁移复制简体到三语仅为**可读占位**；生产环境运营须在后台表单补全真实繁体/英文后方可再次保存（符合三语必填策略）。

---

## R7 — 规范与实现的差异修正

**Decision**: 实现计划采纳 `cashier_orders.display_language`（`cashier_` 前缀），修正 spec 宪法表中「不涉及 cashier 库结构变更」的表述——在 `plan.md` Constitution Check 中记录为**必要扩展**，非违规。

**Rationale**: 无订单级持久化则无法满足「刷新后语言一致」（SC-004）。
