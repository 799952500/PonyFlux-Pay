# Implementation Plan: 项目业务优化 — 商业智能与智能路由

**Branch**: `004-business-optimization` | **Date**: 2026-05-13 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/004-business-optimization/spec.md`

## Summary

为 PonyFlux-Pay 支付网关增加两大核心能力：(1) 商业智能分析仪表盘——为运营方提供实时交易概览、商户排行、渠道分布和自动流失预警；(2) 阶梯费率与智能路由——商户享受"量越大费率越低"的激励，系统自动选择最低成本渠道完成支付。交付物涵盖管理后台前端（admin-client 新增仪表盘页面）、后端聚合计算服务、支付路由模块扩展、以及相关的数据库迁移。

## Technical Context

**Language/Version**: Java 17 / Spring Boot 3.2.5 / Vue 3.4 + TypeScript
**Primary Dependencies**: MyBatis-Plus 3.5.7, Element Plus 2.5+, ECharts 5.5, Axios 1.6
**Storage**: MySQL（`payflow_admin` + `payflow_cashier` 双库）、Redis
**Testing**: JUnit 5 + Mockito / Testcontainers / Vitest + Vue Test Utils
**Target Platform**: Linux Server (JVM 17) + Web Browser（admin-client）
**Project Type**: web-service + web-app（后端服务 + 管理后台前端）
**Performance Goals**: 仪表盘数据 5 分钟内刷新；路由决策 < 50ms；报表导出 10 秒内（<10000 行）
**Constraints**: 聚合计算不直查流水表；路由决策不增加支付耗时 > 50ms；仪表盘前端首屏 < 2 秒
**Scale/Scope**: 每日万级交易流水聚合；百级商户流失预警扫描；千级路由决策/天

## Constitution Check

*GATE: 必须在 Phase 0 研究前通过。Phase 1 设计完成后复查。*

| # | 宪法原则 | 状态 | 说明 |
|---|----------|------|------|
| 1 | I. 模块边界 | ✅ PASS | 仪表盘在 admin-server；路由在 cashier-server；聚合任务可复用 recon-server |
| 2 | II. 支付渠道抽象 | ✅ PASS | 智能路由扩展 PayChannelService，通过 Locator 解析渠道 |
| 3 | III. 数据库分区 | ✅ PASS | 聚合表/费率表 admin_ 前缀；路由日志 recon_ 前缀 |
| 4 | IV. API 响应规范 | ✅ PASS | 所有新增 API 使用统一 { code, message, data } 格式 |
| 5 | V. 密钥与配置安全 | ✅ PASS | 无新增敏感字段 |
| 6 | 编码规范 | ✅ PASS | 遵循命名、格式、注释规范 |
| 7 | 数据库访问规范 | ✅ PASS | 禁止 SELECT *，分页设限，实体注解完整 |
| 8 | 安全编码规范 | ✅ PASS | JWT 拦截器保护，参数校验 |
| 9 | 测试规范 | ✅ PASS | 80% 覆盖率 + DoD 五条件 |

**Gate Result**: ALL PASS

## Project Structure

### Documentation (this feature)

```text
specs/004-business-optimization/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── tasks.md
```

### Source Code (repository root)

```text
# 后端 — payflow-admin-server（仪表盘 + 费率）
payflow-admin-server/src/main/java/com/ponyflux/payflow/admin/
├── controller/DashboardController.java
├── service/DashboardService.java, ChurnAlertService.java, FeeRateService.java
├── mapper/dashboard/DashboardMetricsMapper.java, ChurnAlertMapper.java, FeeRateConfigMapper.java
├── entity/DashboardMetrics.java, ChurnAlert.java, FeeRateConfig.java, MerchantFeeSnapshot.java, FeeRateAuditLog.java
├── task/DashboardAggregationTask.java, ChurnDetectionTask.java

# 后端 — payflow-cashier-server（智能路由）
payflow-cashier-server/src/main/java/com/ponyflux/payflow/cashier/
├── service/routing/CostBasedRoutingStrategy.java, RoutingDecisionLogger.java
└── entity/RoutingDecisionLog.java

# 前端 — payflow-admin-client
payflow-admin-client/src/
├── views/dashboard/DashboardIndex.vue, MerchantDetail.vue, ChurnAlertList.vue
├── components/dashboard/MetricCard.vue, TrendChart.vue, ChannelPieChart.vue, MerchantRanking.vue
├── api/dashboard.ts
└── stores/dashboard.ts

# 数据库
sql/migrations/2026-05-13_dashboard-and-routing.sql
```

**Structure Decision**: 仪表盘归属 admin-server，路由归属 cashier-server，前端仅涉及 admin-client。不创建新 Maven 模块。

## Complexity Tracking

> 无宪法违规，此节留空。
