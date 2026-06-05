# SQL 初始化权威入口

## 推荐方式（开发 / CI / 客户交付）

```bash
# 项目根目录
.\setup.ps1          # Windows
./setup.sh           # Linux / macOS
python scripts/setup.py --db-only   # 仅数据库
```

`scripts/setup.py` 默认会 **DROP 并重建** `payflow_admin` / `payflow_cashier`，再依次执行：

1. `sql/schema/00_create_databases.sql`
2. `sql/schema/payflow_admin.sql`、`sql/schema/payflow_cashier.sql`
3. `sql/seed/payflow_cashier_seed.sql`、`payflow_cashier_merchant_notify_demo.sql`、`payflow_admin_seed.sql`
4. 同步 Flyway 历史至 V11（admin / cashier 各模块）
5. 校验表前缀与 `admin/admin123` 账号

## 目录说明

| 路径 | 用途 |
|------|------|
| `sql/schema/` | 全量 DDL（权威结构） |
| `sql/seed/` | 演示数据 |
| `sql/migrations/` | 历史增量脚本（参考；新环境勿单独手工执行） |
| `payflow-*-server/src/main/resources/db/migration/` | 各服务 Flyway 迁移 |

## Docker Compose

`docker compose up -d mysql redis` 启动中间件后，在宿主机执行 `.\setup.ps1` 或 `python scripts/setup.py`。

## 禁止

- 勿引用已删除的 `full-reseed-payflow-demo.sql` 等死链脚本
- 勿单独跑 seed 而不跑 schema（会导致「半套库」接口 500）
- 生产环境勿直接使用 seed 中的演示密钥
