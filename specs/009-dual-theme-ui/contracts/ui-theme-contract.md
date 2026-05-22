# UI Theme Contract: 管理端与收银台

本契约描述**前端展示层**行为，非 REST API。供实现与 Playwright 验收对齐。

## 1. 文档根属性（Document Root Attributes）

| 属性 | 值域 | 默认值 | 作用 |
|------|------|--------|------|
| `data-theme` | `mint` \| `ocean` \| `violet` \| `dark` | `mint` | 主题性格与主色 Token |
| `data-table-density` | `standard` \| `compact` | `standard` | 表格行高/内边距（仅管理端） |

**要求**:
- 切换主题后 1s 内 `data-theme` 生效，无整页刷新。
- 切换密度后已挂载表格行高立即更新。

## 2. 主题性格契约

### 2.1 清新族（`mint` | `ocean` | `violet`）

- 页面背景、卡片、侧栏：柔和浅色层次。
- 表格：轻斑马纹（`--pf-table-stripe`），悬停为浅主色透明层。
- 图表：网格线/轴线低对比，Tooltip 浅色底。

### 2.2 暗夜族（`dark`）

- 页面与卡片：深色底，强调色更高饱和（「狂放」）。
- 表格：略强斑马纹（`--pf-table-stripe-strong`），悬停层可与斑马纹叠加，**单元格正文/金额/标签/按钮相对直接背景 ≥ WCAG AA**。
- 图表：高对比轴线，Tooltip 深色底，可选适度 glow。

## 3. 表格规范契约（管理端）

### 3.1 标记要求

```html
<el-table
  class="data-table"
  stripe
  table-layout="auto"
  :size="tableDensity === 'compact' ? 'small' : 'default'"
/>
```

- `class` 必须包含 `data-table`。
- 必须 `stripe`（斑马纹由全局 CSS + Token 控制，非 Element 默认灰条）。
- 禁止页面内联 `style` 覆盖行背景/表头色。

### 3.2 密度契约

| 密度 | 行垂直内边距 | 表头 | 字号 |
|------|--------------|------|------|
| standard | `--pf-table-cell-py`（约 12–14px 等效） | `--pf-table-header-py` | 14px 正文 |
| compact | `--pf-table-cell-py-compact`（约 8–10px） | 降低表头高度 | **不降低**正文字号 |

### 3.3 交互态

| 状态 | 行为 |
|------|------|
| hover | 不透明 `--pf-table-hover`，fixed 列同步 |
| striped | 偶数行 `--pf-table-stripe` 或 dark 下 `--pf-table-stripe-strong` |
| empty | 空文案色 `--pf-text-secondary`，背景 `--pf-card-bg` |
| loading | 骨架/遮罩使用主题色，非默认灰 |

### 3.4 行内组件

- `el-tag`、`el-button`（link/text）、图标：使用 `size="small"` 或表格行内规范类，不得使行高超过当前密度上限。

## 4. 外观偏好 UI 契约

**路由**: `/admin/preferences`（名称：外观与显示）

| 控件 | 类型 | 存储键 | 说明 |
|------|------|--------|------|
| 主题 | 单选卡片或色块列表 | `adminTheme` | 与顶栏四预设一致 |
| 表格密度 | 单选 `standard` / `compact` | `adminTableDensity` | 修改后全站即时生效 |

**顶栏**: 保留 Brush 下拉快速切换主题（不必移除）。

## 5. 收银台契约（子集）

| 区域 | 清新 | 暗夜（若启用） |
|------|------|----------------|
| 顶栏/品牌区 | 与管理端 mint 主色同源 | 高对比，logo 用 dark 变体 |
| 金额区 | 突出 `--pf-amount` 语义 | 金色/高亮，不降低对比 |
| 主按钮 | 品牌渐变/主色 | 更强对比边框或 glow |
| 表格（若有） | 轻斑马纹 + 标准密度视觉 | 略强斑马纹 + AA |

不要求收银台暴露 `ocean`/`violet` 切换。

## 6. 错误与降级

| 场景 | 行为 |
|------|------|
| localStorage 不可用 | 会话内保持选择；刷新恢复默认；可选 ElMessage 提示一次 |
| 非法 theme/density 值 | 忽略并回退 `mint` / `standard` |
| `prefers-reduced-motion` | 主题切换无动画，即时应用 |

## 7. 验收检查清单（契约级）

- [ ] 12+ 管理路由表格符合 §3.1
- [ ] 4 主题 × 2 密度组合下表头/斑马纹/hover 一致
- [ ] 暗夜 30+ 单元格对比度 AA（SC-010）
- [ ] 外观页修改密度后订单列表行高变化且无需刷新
- [ ] 收银台与管理端并排品牌一致（SC-003）
