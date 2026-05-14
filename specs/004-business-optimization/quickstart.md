# Quick Start: 商业智能与智能路由 — 开发环境

**Date**: 2026-05-13
**Audience**: PonyFlux-Pay 项目开发者

---

## 1. 功能概述

本功能为 PonyFlux-Pay 增加两大核心能力：

- **商业智能仪表盘**：运营方可视化查看实时交易数据、商户排行、渠道分布、自动流失预警
- **阶梯费率 + 智能路由**：商户交易量越大费率越低，系统自动选择最低成本渠道

## 2. 前置依赖

- Java 17 + Maven 3.8+
- Node.js 18+ (前端)
- MySQL 8.0 (payflow_admin + payflow_cashier 双库)
- Redis 7.0+ (缓存)
- XXL-Job 2.4+ (定时任务调度)

## 3. 数据库迁移

```bash
# 执行新增表的 DDL 迁移
python scripts/run_mysql_sql.py sql/migrations/2026-05-13_dashboard-and-routing.sql
```

## 4. 模块变更概览

| 模块 | 变更内容 | 启动命令 |
|------|---------|----------|
| `payflow-admin-server` | 仪表盘 API、流失预警、费率配置、数据导出 | `mvn -B -pl payflow-admin-server spring-boot:run` |
| `payflow-cashier-server` | 智能路由 `LOWEST_COST` 策略、路由决策日志 | `mvn -B -pl payflow-cashier-server spring-boot:run` |
| `payflow-admin-client` | 仪表盘页面、预警列表、费率配置页面、路由日志查询 | `cd payflow-admin-client && npm ci && npm run dev` |

## 5. 前端开发

```bash
cd payflow-admin-client
npm ci
npm run dev     # 启动在 :3001
```

新增页面路由：
- `/dashboard` — BI 仪表盘首页
- `/dashboard/merchant/:id` — 商户交易详情
- `/dashboard/churn-alerts` — 流失预警列表
- `/fee-rate/config` — 阶梯费率配置
- `/routing/logs` — 路由决策日志查询

## 6. 后端关键 API

### 仪表盘

| 接口 | 说明 |
|------|------|
| `GET /api/v1/admin/dashboard/metrics` | 获取仪表盘核心指标（支持日期范围/粒度参数） |
| `GET /api/v1/admin/dashboard/trend` | 交易趋势图数据 |
| `GET /api/v1/admin/dashboard/channel-distribution` | 渠道占比饼图数据 |
| `GET /api/v1/admin/dashboard/merchant-ranking` | 商户交易额排行榜 |

### 流失预警

| 接口 | 说明 |
|------|------|
| `GET /api/v1/admin/churn-alerts` | 预警列表（分页/筛选） |
| `GET /api/v1/admin/churn-alerts/{id}` | 预警详情 |
| `PUT /api/v1/admin/churn-alerts/{id}/status` | 更新预警状态 |

### 阶梯费率

| 接口 | 说明 |
|------|------|
| `GET /api/v1/admin/fee-rates` | 费率规则列表 |
| `POST /api/v1/admin/fee-rates` | 创建费率规则 |
| `PUT /api/v1/admin/fee-rates/{id}` | 更新费率规则 |
| `DELETE /api/v1/admin/fee-rates/{id}` | 删除费率规则 |

### 路由日志

| 接口 | 说明 |
|------|------|
| `GET /api/v1/admin/routing-logs` | 路由决策日志（分页/筛选） |
| `GET /api/v1/admin/routing-logs/export` | 导出路由日志 |

### 数据导出

| 接口 | 说明 |
|------|------|
| `POST /api/v1/admin/export/report` | 创建导出任务（异步） |
| `GET /api/v1/admin/export/tasks` | 查询导出任务列表 |

## 7. 定时任务配置

在 XXL-Job 管理后台注册以下任务：

| 任务名称 | Cron | 说明 |
|---------|------|------|
| `dashboardAggregationTask` | `*/5 * * * *` | 5 分钟粒度仪表盘数据聚合 |
| `dashboardHourlyAggregationTask` | `10 * * * *` | 每小时聚合 |
| `dashboardDailyAggregationTask` | `5 0 * * *` | 每日汇总 |
| `churnDetectionTask` | `0 2 * * *` | 每日凌晨 2 点流失预警检测 |
| `feeRateMonthBeginTask` | `0 0 1 * *` | 每月 1 日 0 点费率档位结算 |

## 8. 路由模式切换

商户侧路由策略通过 `admin_merchants` 表配置：

```sql
-- 切换某商户到最低成本模式
UPDATE admin_merchants SET routing_mode = 'LOWEST_COST' WHERE id = {merchant_id};

-- 恢复默认权重模式
UPDATE admin_merchants SET routing_mode = 'WEIGHTED' WHERE id = {merchant_id};
```

## 9. 测试验证

```bash
# 后端测试
mvn -B -pl payflow-admin-server test -Dtest=DashboardServiceTest
mvn -B -pl payflow-admin-server test -Dtest=ChurnAlertServiceTest
mvn -B -pl payflow-admin-server test -Dtest=FeeRateServiceTest
mvn -B -pl payflow-cashier-server test -Dtest=CostBasedRoutingStrategyTest

# 前端测试
cd payflow-admin-client && npm run test
```
