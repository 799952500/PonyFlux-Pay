# Data Model: 双端统一主题与 UI 体验优化

本特性无后端持久化实体；以下为**前端本地偏好与视觉令牌**的逻辑模型。

## 实体

### Theme（主题）

| 字段 | 类型 | 说明 |
|------|------|------|
| key | `mint` \| `ocean` \| `violet` \| `dark` | 主题标识，映射 `data-theme` |
| personality | `fresh` \| `night` | 性格：清新族 / 暗夜族 |
| label | string | 展示名（如「清新薄荷」） |
| primaryColor | string | 预览色块 |

**关系**: 1 个 Theme 对应 1 组 Design Token 覆盖（`themes.css` 中 `[data-theme='…']`）。

**状态**: 用户通过顶栏或外观页选择 → 写入 `localStorage.adminTheme` → `document.documentElement.dataset.theme`。

---

### TableDensity（表格密度）

| 字段 | 类型 | 说明 |
|------|------|------|
| key | `standard` \| `compact` | 密度标识 |
| label | string | 「标准」/「紧凑」 |
| rowPaddingY | CSS length | 单元格垂直内边距（Token） |
| headerHeight | CSS length | 表头行高（Token） |

**关系**: 与 Theme 正交；映射 `data-table-density`。

**状态**: 默认 `standard` → 用户在外观页修改 → `localStorage.adminTableDensity` → 全站 `.data-table` 应用。

**校验**: 非法存储值回退 `standard`；localStorage 不可用时仅会话内有效。

---

### DesignToken（视觉令牌）

| 分类 | 示例变量 | 说明 |
|------|----------|------|
| 页面 | `--pf-bg-page`, `--pf-card-bg` | 背景层级 |
| 文本 | `--pf-text-primary`, `--pf-amount` | 正文与金额 |
| 表格 | `--pf-table-header`, `--pf-table-hover`, `--pf-table-stripe` | 表头、悬停、斑马纹 |
| 密度 | `--pf-table-cell-py`, `--pf-table-header-py` | 标准/紧凑覆盖 |
| 图表 | ChartThemeColors（TS 对象） | ECharts 配色 |

**规则**: 组件禁止硬编码 `#334155` 等字面量（存量逐步清除）；表格相关仅通过上表变量派生。

---

### TableStyleContract（表格规范）

逻辑约束集合（非数据库表）：

| 规则 ID | 约束 |
|---------|------|
| TSC-01 | 所有管理端列表 `el-table` 必须 `class` 含 `data-table` |
| TSC-02 | 必须 `stripe` 以启用主题化斑马纹 |
| TSC-03 | 表头/行/悬停/分页/空态颜色来自 DesignToken |
| TSC-04 | 清新族斑马纹使用 `--pf-table-stripe`（低对比） |
| TSC-05 | 暗夜斑马纹使用 `--pf-table-stripe-strong`（略强，且满足 AA） |
| TSC-06 | 行内 `el-tag`、按钮不得撑破密度规定的行高 |

---

### UserThemePreference / UserTableDensityPreference

| 存储键 | 值域 | 作用域 |
|--------|------|--------|
| `adminTheme` | Theme.key | 管理端 |
| `adminTableDensity` | TableDensity.key | 管理端 |

收银台（若后续支持）可预留 `cashierThemeMode: fresh \| night`，本期仅 CSS 默认 fresh + 可选 `prefers-color-scheme` 对齐。

## 状态流转

```text
[首次访问]
  → theme=mint, density=standard（默认）
[用户切换主题]
  → 更新 localStorage + data-theme → 全站 CSS 变量重算 → 图表 refresh
[用户切换密度]
  → 更新 localStorage + data-table-density → 表格行高即时更新
[刷新页面]
  → init() 从 localStorage 恢复；失败则默认
[禁用 localStorage]
  → 会话内有效；刷新后回退默认（可一次性提示）
```

## 与规格 FR 映射

| FR | 模型要素 |
|----|----------|
| FR-001~003 | Theme, UserThemePreference |
| FR-011~018 | TableStyleContract, DesignToken, TableDensity |
| FR-015~016 | UserTableDensityPreference |
| FR-010 | DesignToken 驱动全局组件 |
