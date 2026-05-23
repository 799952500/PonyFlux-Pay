# Implementation Plan: 商户回调记录查询

**Branch**: `010-merchant-callback-query` | **Date**: 2026-05-22 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/010-merchant-callback-query/spec.md`

**Note**: 本计划由 `/speckit-plan` 命令填充。执行工作流见 `.specify/templates/plan-template.md`。

## Summary

为「平台 → 商户」异步通知建立可查询的持久化记录：汇总表（按订单 + 回调类型）+ 明细表（每次 HTTP 尝试），在 `MerchantNotifyWorker` 投递时写入；管理后台新增「商户回调记录」菜单与 API，支持列表筛选、详情出入参展示及商户数据隔离。与既有 `cashier_callback_logs`（渠道入站）严格区分。首版不含手动重发、历史补录、CSV 导出。

## Technical Context

**Language/Version**: Java 17 / TypeScript  
**Primary Dependencies**: Spring Boot 3.2.5 / MyBatis-Plus 3.5.7 / RocketMQ / Vue 3.4 / Element Plus / Axios  
**Storage**: MySQL `payflow_cashier`（新表 `cashier_merchant_notify`、`cashier_merchant_notify_attempt`）；菜单/权限 `payflow_admin`  
**Testing**: JUnit 5 + Mockito（Worker 写库、Service 隔离）；admin API 集成测试；按需 Playwright E2E  
**Target Platform**: Linux Server (JVM 17) / Web Browser  
**Project Type**: web-service + web-app  
**Performance Goals**: 列表查询 p95 &lt; 500ms（20 条分页，有索引）；单次回调写库额外开销 &lt; 50ms  
**Constraints**: 不改变商户回调成功判定规则；不改变统一 API 响应结构；跨商户拒绝不泄露存在性；展示层脱敏  
**Scale/Scope**: cashier-server 写路径 1 处 Worker；admin-server 3 个 GET API；admin-client 1 列表页 + 1 详情抽屉；DDL/seed/菜单

## Constitution Check

*GATE: 必须在 Phase 0 研究前通过。Phase 1 设计完成后复查。*

| # | 宪法原则 | 状态 | 说明 |
|---|----------|------|------|
| 1 | I. 模块边界纪律 | PASS | 写入 cashier-server；查询 admin-server + admin-client；不新增顶层模块。 |
| 2 | II. 支付渠道抽象 | N/A | 不修改 PayStrategy/渠道回调。 |
| 3 | III. 数据库分区 | PASS | 新表 `cashier_*` 落 payflow_cashier；菜单 `sys_menus` 落 payflow_admin。 |
| 4 | IV. API 响应规范 | PASS | 三个 GET 接口返回 `{ code, message, data }`。 |
| 5 | V. 密钥与配置安全 | PASS | 库内存完整参数；API/UI 对 sign/secret 脱敏。 |
| 6 | 编码规范 | PASS | 后续按项目 Java/TS 规范实现。 |
| 7 | 数据库访问 | PASS | 明确字段查询、分页上限 100、禁止 `${}`。 |
| 8 | 安全编码 | PASS | `merchantScope` 强制过滤；越权安全拒绝。 |
| 9 | 测试规范 | PASS | quickstart 定义单测/集成/E2E/日志闭环。 |

**Gate Result**: ALL PASS

## Project Structure

### Documentation (this feature)

```text
specs/010-merchant-callback-query/
├── plan.md              # 本文件
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── quickstart.md        # Phase 1
├── contracts/
│   └── api-contracts.md
└── tasks.md             # Phase 2（/speckit-tasks）
```

### Source Code (repository root)

```text
payflow-cashier-server/
├── entity/MerchantNotify.java, MerchantNotifyAttempt.java
├── mapper/MerchantNotifyMapper.java, MerchantNotifyAttemptMapper.java
├── service/MerchantNotifyRecordService.java
└── consumer/MerchantNotifyWorker.java          # 注入写库

payflow-admin-server/
├── entity/cashier/MerchantNotify*.java         # 读模型
├── mapper/cashier/MerchantNotify*Mapper.java
├── service/MerchantNotifyQueryService.java
├── controller/AdminMerchantNotifyController.java
└── kit/MerchantNotifyMaskKit.java              # 脱敏

payflow-admin-client/
├── pages/admin/merchant-notifies/index.vue     # 列表
├── components/merchant-notifies/DetailDrawer.vue
└── api/merchantNotify.ts

sql/
├── schema/payflow_cashier.sql                  # 全量 DDL 增量
├── migrations/2026-05-22_merchant_notify_tables.sql
└── seed/payflow_cashier_seed.sql, payflow_admin_seed.sql
```

**Structure Decision**: 不新增 Maven 模块。写库逻辑封装为 cashier 内 `MerchantNotifyRecordService`，避免 admin 跨服务写交易库。脱敏 Kit 可后续抽到 `payflow-common`，首版放 admin-server 保持最小范围。

## Complexity Tracking

> 当前无宪法违规，无需豁免。

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|

## Phase 0: Research

已完成：[research.md](./research.md)

关键决策：
- 新表 `cashier_merchant_notify` / `cashier_merchant_notify_attempt`，不复用 `cashier_callback_logs`。
- 汇总键 `(order_id, notify_type)`；类型由 `refundId` 判定 PAYMENT/REFUND。
- 写库点在 `MerchantNotifyWorker`；读库走 admin cashier 数据源。
- 展示脱敏、报文 32KB 截断；保留 `orders.notify_status` 兼容。

## Phase 1: Design & Contracts

已完成：
- [data-model.md](./data-model.md) — 表结构、状态机、索引、校验规则。
- [contracts/api-contracts.md](./contracts/api-contracts.md) — 列表/详情/by-order API、前端路由、菜单 seed。
- [quickstart.md](./quickstart.md) — 验收步骤与成功标准映射。

## Post-Design Constitution Check

| # | 宪法原则 | 状态 | 说明 |
|---|----------|------|------|
| 1 | I. 模块边界纪律 | PASS | 读写分离符合模块职责。 |
| 2 | II. 支付渠道抽象 | N/A | |
| 3 | III. 数据库分区 | PASS | cashier_ 前缀、双库菜单分离。 |
| 4 | IV. API 响应规范 | PASS | 契约已定义。 |
| 5 | V. 密钥与配置安全 | PASS | 契约含脱敏字段说明。 |
| 6 | 编码规范 | PASS | |
| 7 | 数据库访问 | PASS | 分页与索引已设计。 |
| 8 | 安全编码 | PASS | merchantScope + 越权契约。 |
| 9 | 测试规范 | PASS | quickstart 含 E2E/日志项。 |

**Gate Result**: ALL PASS

## Phase 2: Planning Handoff

下一阶段 `/speckit-tasks` 建议按以下工作包拆分：

1. **DDL 与实体**：迁移脚本、schema 全量、cashier/admin 实体与 Mapper。
2. **写库服务**：`MerchantNotifyRecordService` + `MerchantNotifyWorker` 集成（含 NOT_CONFIGURED、截断、状态更新）。
3. **查询 API**：`AdminMerchantNotifyController` + `MerchantNotifyQueryService` + 脱敏 Kit + 商户隔离。
4. **前端页面**：列表筛选、详情抽屉、API 封装、`data-table` 样式。
5. **菜单权限种子**：`sys_menus`、`sys_role_menu`、i18n 文案（如有）。
6. **演示数据**：seed 中失败重试样例订单。
7. **测试**：Worker 单测、API 隔离测试、quickstart 走查、按需 Playwright。
8. **可选 P3**：订单详情「查看商户回调」跳转按钮。
