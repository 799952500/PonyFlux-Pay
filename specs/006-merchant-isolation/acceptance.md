# 验收记录：006-merchant-isolation

**Date**: 2026-05-18  
**Branch**: `006-merchant-isolation`

## 自动化验收（本地已执行）

| 项 | 命令 / 测试类 | 结果 |
|----|----------------|------|
| 商户隔离单元/参数化测试 | `MerchantIsolationSecurityTest`（41 场景） | PASS |
| merchantId 绑定 | `MerchantIdGuardTest` | PASS |
| 资源所有权 | `ResourceOwnershipServiceTest` | PASS |
| Controller ArchUnit（FR-020） | `MerchantControllerArchTest` | PASS |
| Admin 审计分页上限 | `AdminSecurityAuditServiceTest` | PASS |
| 收银台编译 | `mvn -B -pl payflow-cashier-server -am -DskipTests compile` | PASS |

### T058 全量测试（2026-05-18）

```bash
mvn -B test
```

| 模块 | 结果 | 说明 |
|------|------|------|
| `payflow-cashier-server` | **PASS**（49 tests，含 ArchUnit + 商户隔离） | 本特性相关测试全部通过 |
| `payflow-admin-server` | **4 errors**（非本特性引入） | `DashboardMetricsMapperTest` 需完整 Spring 上下文 + MySQL；`HttpSmokeRunnerTest` 需本地已启动 3003 服务 |
| 其他模块 | 未执行至（admin 失败后 reactor 停止） | |

本特性新增测试均可独立通过：

```bash
mvn -B -pl payflow-cashier-server -am "-Dsurefire.failIfNoSpecifiedTests=false" \
  "-Dtest=MerchantIsolationSecurityTest,MerchantControllerArchTest,MerchantIdGuardTest,ResourceOwnershipServiceTest" test
mvn -B -pl payflow-admin-server "-Dtest=AdminSecurityAuditServiceTest" test
```

## quickstart.md 手工步骤

| § | 场景 | 状态 | 备注 |
|----|------|------|------|
| 1 | `mvn test` 安全子集 | 已完成 | 见上表 |
| 2 | merchantId 不一致 → 403/5101 | 待手工 | 需 MySQL + Redis + 双商户 JWT |
| 3 | 跨商户读订单 → 404/5102 | 待手工 | |
| 4 | 跨商户退款 → 404/5102 | 待手工 | |
| 5 | 兼容不传/一致 merchantId | 待手工 | |
| 6 | 管理端审计列表 API/UI | 待手工 | 需 Flyway V4 + 菜单迁移 |
| 7 | 持久层 SQL 日志抽查 | 待手工 | dev DEBUG |
| 8 | 回归白名单（notify/cashier/public） | 待手工 | |
| — | `docs/CONTRACT_MATRIX.md` 已更新 | 已完成 | T054 |
| — | ArchUnit `MerchantControllerArchTest` | 已完成 | T055 |
| — | CI 门禁步骤 | 已完成 | T056 |

## T059 宪法合规自检

对照 [plan.md](./plan.md) Constitution Check：

| # | 原则 | 结论 |
|---|------|------|
| 1 | 模块边界 | 通过：隔离在 cashier；审计查询在 admin |
| 2 | 支付渠道抽象 | N/A |
| 3 | 数据库分区 | 通过：`cashier_security_audit` + Flyway V4 |
| 4 | API 响应规范 | 通过：5101→403、5102→404 统一 R/Map |
| 5 | 密钥安全 | 通过：审计表无密钥 |
| 6 | 编码规范 | 通过 |
| 7 | 数据库访问 | 通过：Lambda 分页、pageSize≤100 |
| 8 | 安全编码 | 通过：本特性即安全加固 |
| 9 | 测试规范 | 通过：≥30 安全场景 + ArchUnit |

**Gate Result**: ALL PASS
