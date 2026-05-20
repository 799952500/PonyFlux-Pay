# PonyFlux Pay — SQL 脚本说明

演示环境与本地开发请使用 **`schema/` + `seed/`** 两套脚本，结构清晰、可重复执行。

## 目录结构

```
sql/
├── README.md                 # 本说明
├── install_demo.sql          # 安装指引（供 DataGrip 手动执行时参考顺序）
├── schema/                   # 第一部分：建库 + 建表（DDL）
│   ├── 00_create_databases.sql
│   ├── payflow_admin.sql     # 运营库：admin_* / sys_* / recon_* + 兼容视图
│   └── payflow_cashier.sql   # 收银台库：cashier_* + 安全审计表
├── seed/                     # 第二部分：演示数据（DML）
│   ├── payflow_cashier_seed.sql
│   └── payflow_admin_seed.sql
└── migrations/               # Flyway 增量迁移（生产/升级用，非演示首选）
```

## 一键安装（推荐）

```bash
python scripts/install_demo_db.py
```

等价于按顺序执行：

1. `schema/00_create_databases.sql`
2. `schema/payflow_admin.sql`
3. `schema/payflow_cashier.sql`
4. `seed/payflow_cashier_seed.sql`
5. `seed/payflow_admin_seed.sql`

## 演示账号

| 用途 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 管理后台 JWT 登录 | `admin` | `admin123` | SUPER_ADMIN，全菜单 |
| 财务演示 | `finance_demo` | `admin123` | 仅商户 M100001 数据 |
| 风控演示 | `risk_demo` | `admin123` | 含安全审计菜单 |

## 演示数据覆盖的页面

| 模块 | 数据要点 |
|------|----------|
| 数据概览 / 仪表盘 | 近 7 日 `admin_dashboard_metrics`、多渠道小时指标 |
| 通知中心 | 2 笔 `REFUNDING` 待办退款 |
| 订单 / 退款 | 10 笔订单、多状态；5 笔退款 |
| 对账 | 成功/失败任务、差异、账单明细 |
| 渠道 / 路由 / 账号 | 微信/支付宝/银联完整配置 |
| 阶梯费率 / 费率审计 | 5 档规则 + 变更记录 |
| 路由决策日志 | 5 条智能路由记录 |
| 商户流失预警 | 黄/橙/红 三级预警 |
| 安全审计 | 5 条越权拒绝记录 |
| 商户进件 | 待审/通过/驳回 各一例 |
| RBAC | 全量菜单 + 角色授权 |

商户号与订单号在 **admin / cashier 两库对齐**（M100001–M100003）。

## DataGrip 手动执行

在编辑器中**按文件顺序**依次 Run 上述 5 个 SQL 文件（每个文件整文件执行，勿只选中单行）。

## 与 Flyway 的关系

- **新环境演示**：用 `schema/` + `seed/`，不要混跑旧版 `full-reseed` 里的 `CREATE TABLE IF NOT EXISTS` 片段。
- **已上线增量升级**：继续用各服务 `src/main/resources/db/migration/` 下的 Flyway 脚本；`sql/migrations/` 为历史归档参考。

## 旧脚本

| 文件 | 状态 |
|------|------|
| `full-reseed-payflow-demo.sql` | 已废弃，请改用 `install_demo_db.py` |
| `admin/schema.sql`、`cashier/schema.sql` | 已由 `schema/payflow_*.sql` 替代 |
| `admin/data.sql` 等 | 已合并进 `seed/` |
