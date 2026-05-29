# SQL 初始化权威入口

## 推荐方式（开发 / CI / Docker）

```bash
python scripts/install_demo_db.py
```

该脚本会依次执行：

1. `sql/schema/payflow_admin.sql`、`sql/schema/payflow_cashier.sql`
2. `sql/seed/payflow_admin_seed.sql`、`sql/seed/payflow_cashier_seed.sql`
3. 同步 Flyway 历史至 V11（admin / cashier 各模块）

## 目录说明

| 路径 | 用途 |
|------|------|
| `sql/schema/` | 全量 DDL（权威结构） |
| `sql/seed/` | 演示数据 |
| `sql/migrations/` | 历史增量脚本（参考，新变更优先 Flyway） |
| `payflow-*-server/src/main/resources/db/migration/` | 各服务 Flyway 迁移 |

## Docker Compose

`docker compose up` 启动 MySQL/Redis 与三 Java 服务后，在宿主机执行 `python scripts/install_demo_db.py`，详见 `scripts/docker-init/README.md`。

## 禁止

- 勿再引用已删除的 `full-reseed-payflow-demo.sql` 等死链脚本
- 生产环境勿直接使用 seed 中的演示密钥
