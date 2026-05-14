# Research: 商业智能与智能路由 — 技术方案研究

**Date**: 2026-05-13
**Purpose**: 解决 Technical Context 中的关键技术决策点。

---

## 1. 仪表盘数据聚合策略

**Decision**: 定时 XXL-Job 任务预聚合到中间表，前端直读中间表。

**Rationale**: 交易流水表（`cashier_payments`）数据量大，实时聚合会导致仪表盘加载慢。采用 T+0 增量聚合（每 5 分钟），写入 `admin_dashboard_metrics` 中间表，前端 API 直接查询聚合结果，响应时间 < 100ms。

**Alternatives considered**:
- 实时查询流水表 + Redis 缓存 → Redis 故障时查询直接打崩 MySQL，风险高
- OLAP 引擎（ClickHouse/Doris）→ 引入新组件运维成本高，当前数据量不需要

---

## 2. 流失预警检测算法

**Decision**: 滚动窗口对比法 — 最近 7 天日均 vs 前 7 天日均。

**Rationale**: 简单、可解释、计算量小。每日凌晨 2:00 执行一次，扫描所有活跃商户（近 30 天有交易），计算两个 7 天窗口的日均交易笔数/金额，下降超过阈值则生成预警。

**Alternatives considered**:
- 线性回归趋势预测 → 过度设计，小商户波动大误报多
- 环比下降（仅对比昨日）→ 过于敏感，周末/节假日误报

**Implementation details**:
```
预警等级 = 
  下降 50%-70% → 黄色预警
  下降 70%-90% → 橙色预警  
  下降 > 90%   → 红色预警
```

---

## 3. 智能路由 "最低成本模式" 实现方案

**Decision**: 在现有 `PayChannelService.routeToAccount()` 中新增路由策略枚举 `LOWEST_COST`，复用现有渠道可用性检测逻辑。

**Rationale**: 当前路由已支持"按权重"模式，新增"最低成本"只需扩展策略而不重写路由框架。渠道费率从 `admin_channels` 表新增 `fee_rate` 字段读取。路由决策日志异步写入（`@Async`），不阻塞支付主流程。

**Alternatives considered**:
- 独立路由服务（微服务化）→ 当前单体架构，引入网络开销，过度设计
- Redis Sorted Set 按费率排序 → 增加 Redis 依赖，费率变更时需同步

---

## 4. 前端仪表盘技术选型

**Decision**: 复用 admin-client 现有技术栈 — Vue 3 + Element Plus + ECharts 5.5。

**Rationale**: admin-client 已集成 ECharts 5.5（见 CLAUDE.md），无需引入新图表库。Element Plus 提供日期选择器、表格、卡片等组件。Pinia 管理仪表盘状态（日期范围、筛选条件）。

**Alternatives considered**:
- AntV/G2 → 功能更强但需引入新依赖，与现有 ECharts 重复
- Grafana 嵌入 → 需要额外部署，数据需要走 Grafana 数据源

---

## 5. 报表导出方案

**Decision**: 小数据同步导出（< 10000 行），大数据异步导出。

**Rationale**: Excel 导出使用 Apache POI（项目已有的依赖）。同步导出：直接查询 → 生成 Excel → 返回文件流。异步导出：XXL-Job 任务生成文件 → 存储到 `recon_file_storage` → 站内通知下载链接。

**Alternatives considered**:
- EasyExcel → 性能更好但需引入新依赖
- CSV 格式 → 简单但中文乱码问题多，Excel 对运营人员更友好

---

## 6. 数据聚合表设计原则

**Decision**: 三级粒度 — 5 分钟快照（当天）、小时汇总（近 7 天）、日汇总（历史）。

**Rationale**: 仪表盘当天数据需要 5 分钟刷新（运营实时监控），近 7 天趋势图用小时粒度即可，历史数据按天存储节省空间。定期清理：5 分钟快照保留 1 天，小时汇总保留 30 天，日汇总永久保留。

**Table**: `admin_dashboard_metrics`
- `metric_time` DATETIME
- `granularity` ENUM('5min','hour','day')
- `total_amount` BIGINT (分)
- `total_count` INT
- `active_merchants` INT
- `fee_income` BIGINT (分)
- `channel_code` VARCHAR (渠道维度，ALL=汇总)
