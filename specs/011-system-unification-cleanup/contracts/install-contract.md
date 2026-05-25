# Contract: 一键演示库安装

**Feature**: `011-system-unification-cleanup`

## 入口

```bash
python scripts/install_demo_db.py [--host HOST] [--port PORT] [--user USER] [--password PASS]
```

## 执行顺序（固定）

| 阶段 | 文件 | 说明 |
|------|------|------|
| 1 | `sql/schema/00_create_databases.sql` | 建库 |
| 2 | `sql/schema/payflow_admin.sql` | admin + recon + admin_sys 全量 DDL |
| 3 | `sql/schema/payflow_cashier.sql` | cashier 全量 DDL |
| 4 | `sql/seed/payflow_cashier_seed.sql` | 收银台演示数据 |
| 5 | `sql/seed/payflow_admin_seed.sql` | 后台演示数据 |

**禁止**出现在 `SQL_FILES`：`sql/migrations/*`、`sql/admin/*`、`sql/cashier/*`、`sql/full-reseed-payflow-demo.sql`、各 server `src/main/resources/sql/*.sql`。

## 成功输出

- 每文件一行 `OK  <relative-path>  (N statements)`
- 结尾：`演示库安装完成` + `管理后台登录: admin / admin123`
- 建议校验提示：`SELECT COUNT(*) FROM payflow_cashier.cashier_orders;`

## 幂等行为（FR-010）

| 场景 | 预期 |
|------|------|
| 空库首次安装 | 成功 |
| 已有完整库重复执行 | 成功（覆盖式 DROP/CREATE）或明确提示先 `--drop`（若实现） |
| 部分残留旧表名 | 失败并提示清库：`DROP DATABASE payflow_admin; DROP DATABASE payflow_cashier;` |

## 安装后校验（可选脚本）

`scripts/validate_table_prefixes.py`（待实现）或 SQL 见 [table-prefix-validation.md](./table-prefix-validation.md)。
