# Docker 数据库初始化

`docker-compose.yml` 中的 MySQL **不再**挂载已删除的 `sql/full-reseed-payflow-demo.sql`。

首次启动后，在宿主机执行：

```bash
python scripts/install_demo_db.py
```

将按 `sql/schema/` + `sql/seed/` 与 Flyway 历史（至 V11）写入演示数据。
