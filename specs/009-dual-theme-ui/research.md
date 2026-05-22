# Research: 双端统一主题与 UI 体验优化

## Decision: 延续现有 CSS 变量 + `data-theme` 属性驱动，不引入新 UI 框架

**Rationale**: 管理端已具备 `themes.css`、`theme.ts`（mint/ocean/violet/dark）、`dark-theme-overrides.css` 与全局 `.data-table` 规则；收银台已有品牌色变量。在现有 Token 体系上扩展表格密度与斑马纹令牌，改动面最小且与 Element Plus 兼容。

**Alternatives considered**:
- 引入 Tailwind 暗色 class 策略替代 `data-theme`：需全站重写，风险高。
- 每页 Scoped 表格样式：无法保证 FR-011 全站一致，维护成本高。

## Decision: 表格密度由独立 Pinia Store + `data-table-density` 文档属性控制

**Rationale**: 主题与密度是正交维度（4 主题 × 2 密度）。密度不应写入 `data-theme`，否则切换主题时需同步改写密度。在 `<html>` 或 `body` 上增加 `data-table-density="standard|compact"`，由 `useTableDensityStore` 管理，与 `useThemeStore` 并列。

**Alternatives considered**:
- 仅用 `el-table` 的 `size="small"`：各页不一致，且无法表达「紧凑」行高令牌。
- 后端用户配置表：规格明确本地偏好、无 API。

## Decision: 斑马纹强度按主题性格分档，通过 Token 而非页面开关

**Rationale**: 澄清结论为清新轻斑马纹、暗夜略强斑马纹，且深色不得吞没内容。在 `themes.css` 为清新预设与 `dark` 分别定义 `--pf-table-stripe` / `--pf-table-stripe-strong`，`.data-table.el-table--striped` 统一引用；暗夜悬停层叠加时正文对比度由 `--pf-table-hover` 与 FR-018 走查约束。

**Alternatives considered**:
- 用户可选是否斑马纹：增加选项噪音，与「按主题」澄清冲突。
- 关闭 stripe 属性：失去读行辅助，宽表可读性下降。

## Decision: 外观偏好入口新建「外观与显示」页面，顶栏保留主题快捷切换

**Rationale**: 现有 `settings.vue` 为后端 `system_config` 业务配置，不宜混入用户 UI 偏好。规格要求设置中与主题并列的表格密度配置。采用独立路由 `preferences.vue`（外观与显示），顶栏 Brush 下拉保留快速换肤，设置页提供主题 + 密度完整说明与预览。

**Alternatives considered**:
- 仅顶栏下拉：无法满足「设置里可配置密度」的验收场景。
- 写入 `settings.vue`：与系统配置语义混淆。

## Decision: 管理端表格统一依赖 `.data-table` + 全局 CSS，页面层仅补结构类名

**Rationale**: 约 25+ 页面已使用 `class="data-table"`；`style.css` 已集中表头、斑马纹、hover、fixed 列规则。本特性重点是补全未挂 `data-table` 的表格、统一 `stripe`、清除硬编码色（如 `text-blue-600`），并扩展 compact 令牌。

**Alternatives considered**:
- 封装 `PfDataTable` 组件：长期更优但本期范围过大；可在 plan Phase 2 任务中作为可选后续。
- 依赖 `patch-admin-tables.mjs` 一次性脚本：可作为 CI/开发辅助，不能替代运行时 Token。

## Decision: 图表主题从「仅 dark 分支」扩展为按 `data-theme` 全预设适配

**Rationale**: `chartTheme.ts` 当前仅区分 dark/非 dark，mint/ocean/violet 共用一套浅色图表色，与「清新性格一致」有差距。应按主题 key 返回柔和/张力两套图表 Token，并在 dashboard 切换主题时 `setOption` 刷新。

**Alternatives considered**:
- 图表保持现状：仪表盘与主题切换体验割裂，违反 US2/US4。

## Decision: 收银台采用与管理端同源的 Token 子集（不要求四主题切换 UI）

**Rationale**: 规格允许收银台不暴露多预设，但清新/暗夜性格须同源。收银台默认清新；若系统 `prefers-color-scheme: dark` 或后续增加开关，使用与 admin `dark` 同源的 CSS 变量文件（可抽 `packages` 或复制精简 Token 到 `cashier-client/src/styles/brand-tokens.css`）。

**Alternatives considered**:
- 收银台完整四主题：规格未要求，增加测试矩阵。
- 收银台不改：违反 FR-004/SC-003。

## Decision: 验证以 Playwright 视觉冒烟 + 表格路由清单走查为主

**Rationale**: 宪法要求 UI 变更按需 E2E。本特性无 API 契约变更，验收重点是主题/密度切换、表格一致性、暗夜对比度抽检（可用 axe 或手工清单）。

**Alternatives considered**:
- 纯截图对比 Percy：未在仓库配置，本期用 Playwright + 人工走查清单。
