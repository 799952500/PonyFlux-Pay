# Ops Contract: 可观测性与部署

**Created**: 2026-05-29  
**Feature**: [../spec.md](../spec.md)

## 1) Actuator 端点（三服务对齐）

| 服务 | 端口 | prod 暴露 |
|------|------|-----------|
| admin-server | 3003 | `health`, `info` |
| cashier-server | 3002 | `health`, `info` |
| recon-server | 3004 | `health`, `info`, `metrics`, `prometheus` |

**dev profile** 可额外暴露 `metrics,prometheus`。

## 2) recon 自定义指标（Micrometer）

| 指标名 | 类型 | 标签 |
|--------|------|------|
| `recon.task.duration` | Timer | `task_id`, `status` |
| `recon.task.failures` | Counter | `reason` |
| `recon.diff.count` | Gauge/Counter | `diff_type` |

## 3) 日志规范

| 环境 | `com.payflow` 级别 | MyBatis SQL |
|------|-------------------|-------------|
| dev | DEBUG（可选） | 可开启 |
| prod | INFO | **关闭** stdout |

**脱敏规则**:
- 手机号：`139****1219`
- 密钥/密码：不输出
- 渠道 API 响应 body：ERROR 级最多 200 字符；完整 body 仅 DEBUG

## 4) docker-compose 初始化

**权威入口**: `python scripts/install_demo_db.py`

**compose 要求**:
- MySQL init **不得**引用 `sql/full-reseed-payflow-demo.sql`
- 提供 `.env.example` 列出：`DB_PASSWORD`, `JWT_SECRET`, `INTERNAL_TOKEN`, `MASTER_KEY`

## 5) 健康检查（compose / K8s）

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:3003/actuator/health"]
  interval: 30s
  timeout: 5s
  retries: 3
```

（cashier `3002`，recon `3004` 同理）

## 6) Flyway 一致性

| 库 | 目标版本 |
|----|----------|
| payflow_admin | V11 |
| payflow_cashier | V5 |
| payflow_recon | V1 baseline |

`install_demo_db.py` 的 `flyway_schema_history` 插入记录 MUST 与上表一致。

## 7) CI 质量门禁（Wave 3）

| 门禁 | 初期阈值 |
|------|----------|
| JaCoCo 行覆盖率（聚合） | ≥ 40% |
| Playwright E2E | nightly → 逐步 required |
| 静默跳过测试 | 禁止（须 `@Disabled` 或 Testcontainers） |
