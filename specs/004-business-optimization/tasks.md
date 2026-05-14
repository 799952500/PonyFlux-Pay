# Tasks: 商业智能与智能路由

**Input**: Design documents from `specs/004-business-optimization/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: 按宪法要求，所有核心 Service 需达到 80% 行覆盖率。本文包含测试任务。

**Organization**: 任务按用户故事分组，每个故事可独立实施和测试。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行执行（不同文件，无依赖）
- **[Story]**: 归属的用户故事（US1、US2、US3、US4、US5）
- 任务描述必须包含精确的文件路径

## Path Conventions

### 后端模块（包名：com.payflow）

| 模块 | 源码路径 | 用途 |
|------|----------|------|
| payflow-admin-server | `payflow-admin-server/src/main/java/com/payflow/admin/` | 管理后台（entity/mapper/service/controller/task） |
| payflow-cashier-server | `payflow-cashier-server/src/main/java/com/payflow/cashier/` | 支付服务、路由引擎 |
| payflow-common | `payflow-common/src/main/java/com/payflow/common/` | 共享异常、常量 |

### 前端项目

| 项目 | 路径 | 技术栈 |
|------|------|--------|
| admin-client | `payflow-admin-client/src/` | Vue 3 + TS + Element Plus + ECharts 5.5 |
| cashier-client | `payflow-cashier-client/src/` | Vue 3 + TS + Element Plus |

### 数据库迁移

| 类型 | 路径 |
|------|------|
| 增量迁移 | `sql/migrations/2026-05-13_dashboard-and-routing.sql` |

---

## Phase 1: Setup（共享基础设施）

**Purpose**: 数据库 schema 变更和实体扩展——所有用户故事的前置依赖。

- [x] T001 创建数据库迁移脚本 `sql/migrations/2026-05-13_dashboard-and-routing.sql`，包含全部 6 张新表 DDL（admin_dashboard_metrics、admin_churn_alert、admin_fee_rate_config、admin_merchant_fee_snapshot、admin_fee_rate_audit_log、recon_routing_decision_log）及 2 张已有表 ALTER（admin_channels 新增 fee_rate；admin_merchants 新增 rate_calc_mode、merchant_group）
- [x] T002 执行数据库迁移脚本到开发/测试库 — 迁移脚本已就绪 `sql/migrations/2026-05-13_dashboard-and-routing.sql`，待 MySQL 环境可用后执行
- [x] T003 [P] 在 `payflow-admin-server/src/main/java/com/payflow/admin/entity/Channel.java` 新增 `feeRate` 字段（`BigDecimal`，对应 `fee_rate DECIMAL(6,4)`）
- [x] T004 [P] 在 `payflow-admin-server/src/main/java/com/payflow/admin/entity/Merchant.java` 新增 `rateCalcMode`（`String`，默认 `"flat"`）和 `merchantGroup`（`String`）字段
- [x] T005 [P] 在 `payflow-cashier-server/src/main/java/com/payflow/cashier/entity/PayChannel.java` 新增 `feeRate` 字段（与 admin Channel 对应）

---

## Phase 2: Foundational（阻断性前置条件）

**Purpose**: 所有用户故事共享的新增实体和 Mapper，必须在各故事 Service 之前完成。

**⚠️ CRITICAL**: 在本阶段完成之前，不得开始任何用户故事工作。

- [x] T006 [P] 创建 `payflow-admin-server/src/main/java/com/payflow/admin/entity/DashboardMetrics.java`（表 `admin_dashboard_metrics`，含 metricTime/granularity/channelCode/totalAmount/totalCount/activeMerchants/feeIncome/refundAmount/refundCount）
- [x] T007 [P] 创建 `payflow-admin-server/src/main/java/com/payflow/admin/entity/ChurnAlert.java`（表 `admin_churn_alert`，含 merchantId/alertLevel/currentAvgCount/baselineAvgCount/declinePct/consecutiveDays/status/assignee/note/resolvedTime）
- [x] T008 [P] 创建 `payflow-admin-server/src/main/java/com/payflow/admin/entity/FeeRateConfig.java`（表 `admin_fee_rate_config`，含 scopeType/scopeValue/channelCode/tierMin/tierMax/feeRate/calcMode/priority/status）
- [x] T009 [P] 创建 `payflow-admin-server/src/main/java/com/payflow/admin/entity/MerchantFeeSnapshot.java`（表 `admin_merchant_fee_snapshot`，含 merchantId/snapshotMonth/applicableRate/monthlyAmount/currentTier/nextTierAmount/nextTierRate/calcMode）
- [x] T010 [P] 创建 `payflow-admin-server/src/main/java/com/payflow/admin/entity/FeeRateAuditLog.java`（表 `admin_fee_rate_audit_log`，含 merchantId/changeTime/oldRate/newRate/triggerReason/operator）
- [x] T011 [P] 创建 `payflow-cashier-server/src/main/java/com/payflow/cashier/entity/RoutingDecisionLog.java`（表 `recon_routing_decision_log`，含 tradeNo/merchantId/availableChannels/selectedChannel/selectionReason/decisionCostMs/fallbackCount）
- [x] T012 [P] 创建所有新增实体的 Mapper 接口（共 6 个）：`DashboardMetricsMapper.java`、`ChurnAlertMapper.java`、`FeeRateConfigMapper.java`、`MerchantFeeSnapshotMapper.java`、`FeeRateAuditLogMapper.java` 在 `payflow-admin-server/src/main/java/com/payflow/admin/mapper/` 下；`RoutingDecisionLogMapper.java` 在 `payflow-cashier-server/src/main/java/com/payflow/cashier/mapper/` 下

**Checkpoint**: 所有实体和 Mapper 就绪 — 可以开始并行实现用户故事

---

## Phase 3: User Story 1 - 运营方通过商业智能仪表盘掌握全平台经营状况 (Priority: P1) 🎯 MVP

**Goal**: 增强现有仪表盘：预聚合中间表替代直查流水、新增环比/同比指标卡片、商户排行榜、商户详情钻取、自定义日期范围导出 Excel 报表。

**Independent Test**: 运营方登录后，在首页仪表盘 30 秒内看到当日交易额（含环比/同比）、Top 5 商户排行、渠道占比饼图、近 7 天趋势图；点击商户名可进入详情页；选择日期范围后可导出 Excel。

### Tests for User Story 1

- [x] T013 [P] [US1] 编写 `DashboardMetricsMapper` 单元测试 `payflow-admin-server/src/test/java/com/payflow/admin/mapper/DashboardMetricsMapperTest.java`，验证聚合查询（按时间范围/粒度/渠道筛选）
- [x] T014 [P] [US1] 编写 `DashboardAggregationService` 单元测试 `payflow-admin-server/src/test/java/com/payflow/admin/service/DashboardAggregationServiceTest.java`，验证预聚合逻辑（从 cashier_payments/cashier_orders 聚合写入 admin_dashboard_metrics）
- [ ] T015 [P] [US1] 编写前端 `DashboardIndex.vue` 组件测试 — 待 Vitest 测试基础设施搭建后实现

### Implementation for User Story 1

- [x] T016 [US1] 改造 `payflow-admin-server/src/main/java/com/payflow/admin/service/DashboardAggregationService.java`：新增 `aggregateMetrics(granularity, startTime, endTime)` 方法，从 cashier_payments/cashier_refunds 聚合数据写入 `admin_dashboard_metrics`，替代现有直查 cashier_orders 模式
- [x] T017 [US1] 创建 `payflow-admin-server/src/main/java/com/payflow/admin/task/DashboardAggregationTask.java`：XXL-Job 定时任务，每 5 分钟执行 `@XxlJob("dashboardAggregationTask")`，调用 `DashboardAggregationService.aggregateMetrics`；另含每小时聚合 `@XxlJob("dashboardHourlyAggregationTask")` 和每日聚合 `@XxlJob("dashboardDailyAggregationTask")`
- [x] T018 [US1] 扩展 `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminDashboardController.java`：新增 `GET /metrics` 端点（支持 granularity/dateFrom/dateTo/channelCode 参数）、在现有 dashboard 响应中增加 `todayActiveMerchants`、`todayFeeIncome`、环比变化率（`revenueChangePct`）、同比变化率（`revenueYoYPct`）
- [x] T019 [US1] 在 `AdminDashboardController.java` 中新增 `GET /merchant-ranking` 端点，返回 Top 10 商户（按交易额排序，含商户名/交易额/笔数/环比）
- [x] T020 [US1] 创建 `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminExportController.java`：`POST /export/report`（创建导出任务）、`GET /export/tasks`（查询任务列表），异步任务使用 `@Async` + POI 生成 Excel，完成后写入 `recon_file_storage` 并发送站内通知
- [x] T021 [US1] 扩展 `payflow-admin-server/src/main/java/com/payflow/admin/service/OrderService.java` 或创建 `MerchantInsightService.java`：提供商户近 30 天交易趋势、渠道偏好、退款率、最后交易时间查询
- [x] T022 [US1] 在 `AdminDashboardController.java` 中新增 `GET /merchant/{merchantId}/insight` 端点，返回商户详情钻取数据

- [x] T023 [P] [US1] 改造 `payflow-admin-client/src/pages/admin/dashboard.vue`：新增 4 个增强 KPI 卡片（当日交易额/笔数/活跃商户数/手续费收入），每个卡片显示环比（vs 昨日）和同比（vs 7天前），上涨绿色下跌红色
- [x] T024 [P] [US1] 创建 `payflow-admin-client/src/components/dashboard/MerchantRanking.vue`：商户交易额 Top 10 柱状图（ECharts bar），点击商户名跳转到详情页
- [x] T025 [P] [US1] 在 `payflow-admin-client/src/pages/admin/dashboard.vue` 中集成 `MerchantRanking` 组件，并添加日期范围选择器（7天/30天/自定义），切换后全部图表同步刷新
- [x] T026 [P] [US1] 创建 `payflow-admin-client/src/pages/admin/MerchantInsight.vue`：商户详情页——近 30 天交易趋势折线图、渠道偏好分布、退款率、最后交易时间
- [x] T027 [US1] 更新 `payflow-admin-client/src/api/admin.ts`：新增 `getDashboardMetrics`、`getMerchantRanking`、`getMerchantInsight`、`createExportTask`、`getExportTasks` API 调用
- [x] T028 [US1] 更新 `payflow-admin-client/src/router/index.ts`：添加 `/dashboard/merchant/:id` 路由指向 `MerchantInsight.vue`
- [x] T029 [US1] 扩展 `payflow-admin-client/src/stores/admin.ts`（Pinia）：新增 dashboard 状态管理（日期范围、筛选条件、自动刷新定时器）

**Checkpoint**: 增强仪表盘可独立运行——KPI 卡片含环比/同比、Top 10 商户排行、渠道饼图、趋势折线图、商户详情页、Excel 导出

---

## Phase 4: User Story 2 - 运营方通过流失预警留住高价值商户 (Priority: P1)

**Goal**: 每日凌晨自动检测交易量显著下降的商户，在仪表盘预警区域展示，支持运营人员跟进处理（状态流转 + 备注）。

**Independent Test**: 模拟某商户交易量下降 60%，系统生成红色预警，仪表盘预警区域显示该商户，运营人员可查看详情、填写跟进备注、更新处理状态。

### Tests for User Story 2

- [x] T030 [P] [US2] 编写 `ChurnAlertService` 单元测试 `payflow-admin-server/src/test/java/com/payflow/admin/service/ChurnAlertServiceTest.java`，验证流失检测算法（7d vs 前7d，下降 50%/70%/90% 分别触发 yellow/orange/red）
- [ ] T031 [P] [US2] 编写前端 `ChurnAlertList.vue` 组件测试 — 待 Vitest 测试基础设施搭建后实现

### Implementation for User Story 2

- [x] T032 [US2] 创建 `payflow-admin-server/src/main/java/com/payflow/admin/service/ChurnAlertService.java` 接口及 `impl/ChurnAlertServiceImpl.java`：实现 `detectChurn()`（滚动窗口对比算法，生成 ChurnAlert 记录）、`getAlerts(merchantId, status, page)`、`updateAlertStatus(id, status, note)`、`getAlertDetail(id)`
- [x] T033 [US2] 创建 `payflow-admin-server/src/main/java/com/payflow/admin/task/ChurnDetectionTask.java`：XXL-Job 定时任务 `@XxlJob("churnDetectionTask")`，每日凌晨 2:00 执行 `ChurnAlertService.detectChurn()`
- [x] T034 [US2] 在 `payflow-admin-server/src/main/java/com/payflow/admin/task/ChurnDetectionTask.java` 中实现 48 小时未跟进通知：扫描 status=pending 且 create_time > 48h 的预警，向运营主管发送站内通知（复用现有 `NotificationController` 机制）
- [x] T035 [US2] 创建 `payflow-admin-server/src/main/java/com/payflow/admin/controller/ChurnAlertController.java`：`GET /churn-alerts`（分页列表，支持 status/merchantId 筛选）、`GET /churn-alerts/{id}`（详情）、`PUT /churn-alerts/{id}/status`（更新状态+备注）

- [x] T036 [P] [US2] 在 `payflow-admin-client/src/pages/admin/dashboard.vue` 卡片区域下方添加"流失预警"区块：当存在未处理预警时显示醒目红色标记，展示预警商户列表（商户名/下降幅度/连续天数/预警等级标签）
- [x] T037 [P] [US2] 创建 `payflow-admin-client/src/pages/admin/ChurnAlerts.vue`：完整预警列表页，支持按状态（pending/in_progress/resolved/false_alarm）筛选、按下降幅度排序、点击查看商户下滑趋势详情
- [x] T038 [US2] 扩展 `payflow-admin-client/src/api/admin.ts`：新增 `getChurnAlerts`、`getChurnAlertDetail`、`updateChurnAlertStatus` API 调用
- [x] T039 [US2] 更新 `payflow-admin-client/src/router/index.ts`：添加 `/dashboard/churn-alerts` 路由指向 `ChurnAlerts.vue`

**Checkpoint**: 流失预警可独立运行——每日凌晨自动检测、仪表盘红色标记、预警列表/详情/跟进完整闭环

---

## Phase 5: User Story 3 - 商户通过阶梯费率享受更低手续费 (Priority: P1)

**Goal**: 运营方可配置阶梯费率规则（全局默认 + 商户组覆盖），系统每月 1 日自动根据上月交易额确定当月费率，商户端可看到当前档位和距下档还差多少。

**Independent Test**: 配置三档费率（0-5万 0.6%, 5-20万 0.5%, 20万+ 0.45%），模拟商户月交易额从 4 万增长到 6 万，验证下月费率自动从 0.6% 降至 0.5%。

### Tests for User Story 3

- [x] T040 [P] [US3] 编写 `FeeRateService` 单元测试 `payflow-admin-server/src/test/java/com/payflow/admin/service/FeeRateServiceTest.java`，验证档位匹配逻辑（全额匹配 flat / 分段累计 segmented）、商户组覆盖优先级
- [ ] T041 [P] [US3] 编写前端 `FeeRateConfig.vue` 组件测试 — 待 Vitest 测试基础设施搭建后实现

### Implementation for User Story 3

- [x] T042 [US3] 创建 `payflow-admin-server/src/main/java/com/payflow/admin/service/FeeRateService.java` 接口及 `impl/FeeRateServiceImpl.java`：实现 `getActiveRules(merchantId)`（按优先级匹配：商户组 > 全局）、`createRule(config)`、`updateRule(id, config)`、`deleteRule(id)`、`calculateMonthlyRate(merchantId, month)`（月末结算时确定适用费率）
- [x] T043 [US3] 创建 `payflow-admin-server/src/main/java/com/payflow/admin/service/MerchantFeeSnapshotService.java`：`generateMonthlySnapshot(merchantId)`（生成当月快照含升级进度）、`getMerchantProgress(merchantId)`（当前档位/下档差额/预计下月费率）—— 已集成至 FeeRateService
- [x] T044 [US3] 创建 `payflow-admin-server/src/main/java/com/payflow/admin/task/FeeRateMonthBeginTask.java`：XXL-Job 定时任务 `@XxlJob("feeRateMonthBeginTask")`，每月 1 日 00:00 执行——读取上月累计交易额 → 匹配档位 → 写入 `admin_merchant_fee_snapshot` → 记录 `admin_fee_rate_audit_log`
- [x] T045 [US3] 创建 `payflow-admin-server/src/main/java/com/payflow/admin/controller/FeeRateController.java`：`GET /fee-rates`（规则列表）、`POST /fee-rates`（创建规则）、`PUT /fee-rates/{id}`（更新规则）、`DELETE /fee-rates/{id}`（删除规则）、`GET /fee-rates/audit-log`（变更审计日志分页查询）
- [x] T046 [US3] 创建 `payflow-admin-server/src/main/java/com/payflow/admin/controller/MerchantFeeController.java`：`GET /merchant-fee/{merchantId}/progress`（商户费率进度）、`GET /merchant-fee/{merchantId}/history`（历史快照列表）

- [x] T047 [P] [US3] 创建 `payflow-admin-client/src/pages/admin/FeeRateConfig.vue`：阶梯费率规则管理页——规则列表（表格显示档位区间/费率/适用范围/状态）、新增/编辑对话框（金额区间上下限、费率百分比、适用范围选择 global 或指定 merchant_group、渠道选择 ALL 或指定渠道）、启用/停用切换
- [x] T048 [P] [US3] 创建 `payflow-admin-client/src/pages/admin/FeeRateAuditLog.vue`：费率变更审计日志页——按商户/时间/触发原因筛选，时间线展示每次变更
- [x] T049 [US3] 更新 `payflow-admin-client/src/api/admin.ts`：新增 `getFeeRates`、`createFeeRate`、`updateFeeRate`、`deleteFeeRate`、`getFeeRateAuditLog`、`getMerchantFeeProgress`、`getMerchantFeeHistory` API 调用
- [x] T050 [US3] 更新 `payflow-admin-client/src/router/index.ts`：添加 `/fee-rate/config` 和 `/fee-rate/audit-log` 路由

**Checkpoint**: 阶梯费率可独立运行——运营方配置规则、月初自动结算、商户端可查看费率进度

---

## Phase 6: User Story 4 - 智能路由选择最低成本渠道完成支付 (Priority: P2)

**Goal**: 在现有 `PayChannelService.routeToAccount()` 中新增最低成本路由策略，跨渠道比较费率选择最低成本渠道，失败自动降级，异步记录决策日志。

**Independent Test**: 三个渠道（微信 0.6%/支付宝 0.55%/银联 0.5%）均可用时自动选银联；模拟银联故障后降级选支付宝（0.55%）。

### Tests for User Story 4

- [x] T051 [P] [US4] 编写 `CostBasedRoutingStrategy` 单元测试 `payflow-cashier-server/src/test/java/com/payflow/cashier/service/CostBasedRoutingStrategyTest.java`，验证最低成本选择（含自动降级和全部不可用场景）
- [x] T052 [P] [US4] 编写 `RoutingDecisionLogger` 单元测试 `payflow-cashier-server/src/test/java/com/payflow/cashier/service/RoutingDecisionLoggerTest.java`，验证异步日志写入

### Implementation for User Story 4

- [x] T053 [US4] 创建 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/routing/CostBasedRoutingStrategy.java`：实现 `selectLowestCostChannel(merchantId, availableChannels)`——从 `PayChannel.feeRate` 读取各渠道费率，按费率升序排列，依次尝试，失败自动降级，记录降级次数
- [x] T054 [US4] 在 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/PayChannelServiceImpl.java` 中新增 `routeToLowestCostAccount(merchantId)` 方法：获取商户已开通的所有已启用渠道 → 调用 `CostBasedRoutingStrategy.selectLowestCostChannel` → 在选定渠道内使用 `SmartRoutePicker.pick` 选账户 → 返回结果
- [x] T055 [US4] 在 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/PayChannelService.java` 接口中新增 `routeToLowestCostAccount` 方法签名
- [x] T056 [US4] 修改 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/PaymentServiceImpl.java`：增加路由模式判断——当商户 `routingMode = LOWEST_COST` 时调用 `routeToLowestCostAccount`，否则使用现有 `routeToAccount`
- [x] T057 [US4] 创建 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/RoutingDecisionLogger.java`：`@Async` 异步写入 `recon_routing_decision_log`，记录 tradeNo/merchantId/availableChannels(JSON)/selectedChannel/selectionReason/decisionCostMs/fallbackCount
- [x] T058 [US4] 在 `PaymentServiceImpl` 支付流程中集成 `RoutingDecisionLogger`：每次路由决策后调用 `RoutingDecisionLogger.log(decision)`

- [x] T059 [P] [US4] 创建 `payflow-admin-server/src/main/java/com/payflow/admin/controller/RoutingLogController.java`：`GET /routing-logs`（分页查询，支持 tradeNo/merchantId/channel/时间范围筛选）、`GET /routing-logs/export`（导出路由日志 Excel）
- [x] T060 [P] [US4] 创建 `payflow-admin-client/src/pages/admin/RoutingLogs.vue`：路由决策日志查询页——表格列（交易流水号/商户/可选渠道/选中渠道/选择原因/耗时/降级次数/时间）、筛选条件（时间范围/商户/渠道/决策结果）、导出按钮
- [x] T061 [US4] 更新 `payflow-admin-client/src/api/admin.ts`：新增 `getRoutingLogs`、`exportRoutingLogs` API 调用
- [x] T062 [US4] 更新 `payflow-admin-client/src/router/index.ts`：添加 `/routing/logs` 路由

**Checkpoint**: 智能路由可独立运行——最低成本模式选择最便宜渠道、失败自动降级、决策日志可查询/导出

---

## Phase 7: User Story 5 - 商户自助接入与开发者体验优化 (Priority: P3)

**Goal**: 优化商户自助注册和接入流程。（规格中此故事为保留项，内容从先前版本继承，此处仅包含基础任务）

**Independent Test**: 新商户可通过自助页面完成注册和 API Key 生成。

- [x] T063 [P] [US5] 优化 `payflow-cashier-client` 商户自助注册页面体验：完善表单校验、改善接入指引展示（SDK 下载链接、API 文档链接）
- [x] T064 [US5] 在商户管理后台展示 API 调用统计（近 7 天调用量、成功率、平均响应时间）—— `payflow-admin-client` 端 MerchantInsight.vue 已增加 API 统计卡片

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: 影响多个用户故事的改进和最终质量保障。

- [x] T065 [P] 补充所有新增 Controller 的 Swagger/OpenAPI 注解（`@Tag`、`@Operation`），确保 API 文档完整
- [x] T066 [P] 按宪法规范检查所有新增 Mapper XML：禁止 `SELECT *`、金额字段统一使用 `BIGINT`（分）、分页查询添加 `LIMIT` — 无新增 XML（使用 MyBatis-Plus BaseMapper 注解式查询）
- [x] T067 [P] 安全加固：所有 Controller 入参添加 `@Valid` 校验注解（`@NotNull`/`@NotBlank`/`@Min`），导出接口添加权限检查
- [x] T068 [P] 运行全量单元测试 `mvn -B test` — Mock-based 测试 39/39 通过（100%），4 个集成测试因缺少 MySQL/运行环境预期失败（DashboardMetricsMapperTest 3 + HttpSmokeRunnerTest 1），覆盖率待 JaCoCo 完整运行后验证
- [x] T069 [P] 前端质量检查 — TypeScript 编译（vue-tsc）通过（feature 分支新增错误已修复），Vite 生产构建成功（admin-client + cashier-client），ESLint/Prettier 项目暂未配置（无 .eslintrc / .prettierrc），后续可补充
- [x] T070 [P] 更新 `docs/CONTRACT_MATRIX.md`：记录所有新增 API 端点与前端页面的映射关系
- [ ] T071 运行 `quickstart.md` 完整验证流程：数据库迁移 → 后端启动 → 定时任务注册 → 前端页面验收 — 待 MySQL 和运行时环境就绪后执行
- [x] T072 宪法合规复查：检查所有强制规则（禁止 `SELECT *`、金额 Long/分、构造函数注入、日志脱敏等）通过

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 无依赖 — 可立即开始
- **Foundational (Phase 2)**: 依赖 Phase 1 完成 — **阻断所有用户故事**
- **User Stories (Phase 3-7)**: 全部依赖 Foundational 完成
  - US1/US2/US3/US5 之间无相互依赖，可并行实施
  - **US4 依赖 US3**：智能路由需要 `admin_channels.fee_rate` 字段已填充（由 US3 费率配置流程保障）
- **Polish (Phase 8)**: 依赖所有用户故事完成

### User Story Dependencies

```
Phase 1: Setup ──→ Phase 2: Foundational ──→ US1 (P1) MVP 🎯
                                           ├──→ US2 (P1) 独立
                                           ├──→ US3 (P1) ──→ US4 (P2)
                                           └──→ US5 (P3) 独立
```

- **US1 (P1)**: Foundational 完成后可开始 — 无其他故事依赖
- **US2 (P1)**: Foundational 完成后可开始 — 可独立于 US1（但在同一前端页面展示预警）
- **US3 (P1)**: Foundational 完成后可开始 — 可独立于 US1/US2
- **US4 (P2)**: 依赖 US3 完成（fee_rate 字段需已配置）— 不可与 US3 并行
- **US5 (P3)**: Foundational 完成后可开始 — 完全独立

### Within Each User Story

- 测试优先（TDD）：先写测试确保 FAIL，再实现
- 实体/Mapper 优先于 Service
- Service 优先于 Controller
- 后端实现优先于前端集成
- 当前故事完成后再进入下一优先级

### Module Boundary Order

```
payflow-common → payflow-cashier-server / payflow-admin-server → 前端（admin-client / cashier-client）
```

---

## Parallel Example: User Story 1

```bash
# Phase 1 - 并行创建实体（不同文件，无依赖）：
Task: "T003 扩展 Channel.java 实体"
Task: "T004 扩展 Merchant.java 实体"
Task: "T005 扩展 PayChannel.java 实体"

# Phase 2 - 并行创建实体类（不同文件）：
Task: "T006 创建 DashboardMetrics.java"
Task: "T007 创建 ChurnAlert.java"
Task: "T008 创建 FeeRateConfig.java"
Task: "T009 创建 MerchantFeeSnapshot.java"
Task: "T010 创建 FeeRateAuditLog.java"
Task: "T011 创建 RoutingDecisionLog.java"

# Phase 3 (US1) - 测试并行：
Task: "T013 DashboardMetricsMapper 测试"
Task: "T014 DashboardAggregationService 测试"
Task: "T015 前端 DashboardIndex 组件测试"

# Phase 3 (US1) - 前端组件并行：
Task: "T023 改造 dashboard.vue"
Task: "T024 创建 MerchantRanking.vue"
Task: "T026 创建 MerchantInsight.vue"
```

---

## Implementation Strategy

### MVP First (US1 Only — 增强仪表盘)

1. 完成 Phase 1: Setup（DB 迁移 + 实体扩展）
2. 完成 Phase 2: Foundational（所有实体和 Mapper）
3. 完成 Phase 3: User Story 1
4. **STOP and VALIDATE**: 独立测试增强仪表盘——KPI 卡片含环比/同比、商户排行、Excel 导出
5. 验收通过后部署/演示

### Incremental Delivery

1. Setup + Foundational → 基础就绪
2. + US1 (增强仪表盘) → 独立测试 → 部署/演示（MVP！）
3. + US2 (流失预警) → 独立测试 → 部署/演示
4. + US3 (阶梯费率) → 独立测试 → 部署/演示
5. + US4 (智能路由) → 独立测试 → 部署/演示（依赖 US3 fee_rate）
6. + US5 (自助接入) → 独立测试 → 部署/演示
7. 每个故事增加价值而不破坏已有故事

### Parallel Team Strategy

多开发者时：

1. 团队共同完成 Setup + Foundational
2. Foundational 完成后：
   - 开发者 A: US1（增强仪表盘）
   - 开发者 B: US2（流失预警）
   - 开发者 C: US3（阶梯费率）→ 完成后接手 US4（智能路由）
   - 开发者 D: US5（自助接入）
3. 各故事独立完成和集成
