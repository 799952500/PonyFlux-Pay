# Quickstart: 双端统一主题与 UI 体验优化

## 目标

验证管理端主题/表格密度一致、表格为重中之重、收银台品牌同源，以及暗夜表格无「吞字」。

## 实施备注（表格扫页）

管理端含 `el-table` 的页面均已使用 `class="data-table"` + `stripe`（2026-05-22 扫页）；仪表盘内嵌表已补 `stripe`。

## 前置条件

1. 本地可启动管理端与收银台：

```bash
mvn -B -pl payflow-admin-server spring-boot:run
mvn -B -pl payflow-cashier-server spring-boot:run
```

2. 前端开发服务：

```bash
cd payflow-admin-client && npm run dev
cd payflow-cashier-client && npm run dev
```

3. 使用演示账号登录管理端（密码见 `scripts/verify_admin_password.py` 输出）。

## 验证路径

### 1. 表格全站一致性（P1）

1. 主题设为 **清新薄荷**，密度 **标准**。
2. 依次打开：`/admin/orders`、`/admin/merchants`、`/admin/reconcile/tasks`、`/admin/routing-logs`、`/admin/audit-logs`、`/admin/users`、`/admin/refunds`、`/admin/dashboard`。
3. 确认：表头背景、行高、斑马纹深浅、悬停色、分页样式一致；无灰白默认表头。
4. 切换到 **紧凑**（外观与显示页），重复步骤 2，确认仅行高变密、风格语言不变。

### 2. 主题性格与斑马纹（P1）

1. 在 `mint` → `ocean` → `violet` 间切换，表格斑马纹始终「轻」，整体清新。
2. 切换到 **暗夜**，斑马纹略强、对比更高，金额列与状态标签仍清晰。
3. 在暗夜下 hover 多行，确认无文字与背景融为一体的单元格。

### 3. 外观偏好持久化（P1）

1. 打开 **外观与显示**（`/admin/preferences`）。
2. 设置主题 `violet`、密度 `compact`，刷新浏览器。
3. 确认恢复 violet + compact，顶栏主题指示一致。

### 4. 主题切换性能（P3）

1. 在订单列表页快速切换 4 主题各 3 次。
2. 确认 1 秒内表格配色更新，无整页白屏；翻页后新行样式正确。

### 5. 收银台品牌对齐（P1）

1. 管理端 `mint` 下打开收银台支付演示页。
2. 对比主色、圆角、按钮气质与管理端一致。
3. （若已实现）收银台暗夜或 `prefers-color-scheme: dark` 下检查金额区可读性。

### 6. 图表与次要组件（P2）

1. 打开仪表盘，切换 `mint` 与 `dark`。
2. 确认折线/柱状/饼图轴线、Tooltip、图例随主题更新，无残留浅色轴。

### 7. Playwright 与日志

```bash
cd payflow-admin-client && npx playwright test
cd payflow-cashier-client && npx playwright test
```

关注 admin-server 控制台无 ERROR（本特性无后端变更，仅确认无回归）。

## 表格路由抽检清单（至少 10 项）

| # | 路由 | 备注 |
|---|------|------|
| 1 | `/admin/orders` | 宽表、金额列 |
| 2 | `/admin/merchants` | 操作列 |
| 3 | `/admin/refunds` | 状态标签 |
| 4 | `/admin/reconcile/tasks` | 双表 |
| 5 | `/admin/reconcile/results` | |
| 6 | `/admin/routing-logs` | |
| 7 | `/admin/audit-logs` | |
| 8 | `/admin/users` | |
| 9 | `/admin/dashboard` | 嵌套表 |
| 10 | `/admin/data-isolation` | |
| 11 | `/admin/settings` | 配置表 |
| 12 | `/admin/search` | 搜索结果表 |

## 通过标准

- SC-007~010 对应项全部满足。
- 表格走查清单 100% 符合 [ui-theme-contract.md](./contracts/ui-theme-contract.md) §3。
- 参评者主观：各页表格「一致、美观」≥ 90%（可用内部走查表代替）。
- Playwright 冒烟通过；主题/密度相关阻塞缺陷为 0。
