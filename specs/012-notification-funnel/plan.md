# Implementation Plan: 通知中心与支付漏斗真实化

**Branch**: `012-notification-funnel` | **Date**: 2026-05-26 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/012-notification-funnel/spec.md`

**Note**: 本模板由 `/speckit-plan` 命令填充。执行工作流见 `.specify/templates/plan-template.md`。

## Summary

将当前管理后台中"只有样子"的通知中心和支付漏斗两个功能改造为真实可用状态。

- **通知中心**：新建 `admin_notifications` 表，实现站内通知 CRUD API（列表/未读计数/标记已读/批量已读），在退款审批、流失超时、导出完成、对账差异、回调失败 5 类业务事件触发时通过 `NotificationService` 异步写入通知。前端改造顶栏 Bell 为真实 badge + 下拉预览面板，通知列表页替换当前占位，支持分类 Tab 和筛选。
- **支付漏斗**：改造 `AdminInsightsController.funnel()` 从硬编码 0 升级为直查 `cashier_orders` 聚合统计，按 CREATED→PAYING→PAID 三阶段和流失支路（FAILED/CLOSED/EXPIRED）返回真实数据。前端替换 `<pre>` JSON 为 ECharts 漏斗图 + 筛选器。

技术方案零 NEEDS CLARIFICATION。

## Technical Context

**Language/Version**: Java 17 / TypeScript 5.x
**Primary Dependencies**: Spring Boot 3.2.5 / MyBatis-Plus 3.5.7 / Vue 3.4 / Element Plus 2.5+ / ECharts 5.5 / Pinia 2.1
**Storage**: MySQL（`payflow_admin` + `payflow_cashier` 双库）/ Redis
**Testing**: JUnit 5 + Mockito / Vitest / Playwright
**Target Platform**: Linux Server (JVM 17) / Web Browser
**Project Type**: web-app（多模块支付网关 + 管理后台 SPA）
**Performance Goals**: 通知列表 ≤1.5s / 漏斗首屏 ≤2s / 轮询 badge ≤60s
**Constraints**: 异步通知写入不阻塞主事务；单次分页 ≤500 行
**Scale/Scope**: demo 环境 ~1 万通知/用户、~10 万订单

## Constitution Check

*GATE: 必须在 Phase 0 研究前通过。Phase 1 设计完成后复查。*

| # | 宪法原则 | 状态 | 说明 |
|---|----------|------|------|
| 1 | I. 模块边界纪律 — 代码是否放在正确的 Maven 模块中？ | [x] PASS | 所有后端改动落在 `payflow-admin-server`（通知 Controller/Service/Entity/Mapper + 漏斗 Service）。cashier 数据通过现有二级数据源 `OrderMapper`（`com.payflow.admin.mapper.cashier` 包）查询。不新建模块，不修改 `payflow-cashier-server`。 |
| 2 | II. 支付渠道抽象 — 是否通过 Locator/Registry 访问渠道？ | [x] N/A | 不涉及支付渠道操作或 `PayStrategy` 调用 |
| 3 | III. 数据库分区 — 新增表前缀是否为 admin_/cashier_/recon_？ | [x] PASS | 通知表命名 `admin_notifications`，落在 `payflow_admin` 库。不在 `payflow_cashier` 增表。 |
| 4 | IV. API 响应规范 — 是否返回统一 `{ code, message, data }` 格式？ | [x] PASS | 所有新 API 端点返回 `ResponseEntity<Map<String, Object>>` 包含 `code=0, message="success", data={...}`。已有的 `AdminInsightsController.funnel()` 当前直接返回裸 Map——需改为包含 code/message/data 的统一格式。 |
| 5 | V. 密钥与配置安全 — 敏感数据是否加密存储、密钥来自环境变量？ | [x] N/A | 不涉及密钥或敏感数据存储 |
| 6 | 编码规范 — 命名/格式/注释/成员顺序是否合规？ | [x] PASS | Entity/Service/Controller/DTO 命名遵循 UpperCamelCase；Lombok `@Data`/`@RequiredArgsConstructor`；中文 Javadoc；K&R 大括号 |
| 7 | 数据库访问 — 禁止 SELECT *、禁止 ${}、分页设限？ | [x] PASS | 通知列表使用 LambdaQueryWrapper + 明确字段；漏斗聚合使用 `@Select` 注解的具名 SQL；分页通过 MyBatis-Plus `Page` + `maxLimit(500)` |
| 8 | 安全编码 — 日志脱敏/参数校验/防重放是否到位？ | [x] PASS | 通知列表接口复用 `AdminRequestContext.merchantScope()` 强制数据隔离；page/size 参数校验上限；通知 link 字段不含敏感信息 |
| 9 | 测试规范 — DoD 六条件、80% 覆盖率、按需 Playwright E2E 与后台日志闭环是否满足？ | [x] PASS | 计划包含：通知 Service 单元测试（去重/异步写入）、漏斗聚合 SQL 断言、前端顶栏 Bell + 漏斗页 Playwright E2E |

**Gate Result**: ALL PASS

## Project Structure

### Documentation (this feature)

```text
specs/012-notification-funnel/
├── plan.md              # 本文件（/speckit-plan 输出）
├── research.md          # Phase 0 输出
├── data-model.md        # Phase 1 输出
├── quickstart.md        # Phase 1 输出
├── contracts/           # Phase 1 输出（API 契约）
└── tasks.md             # Phase 2 输出（/speckit-tasks 生成）
```

### Source Code (repository root)

```text
# 本特性涉及的模块：

payflow-admin-server/                         # 后端主战场
├── src/main/java/com/payflow/admin/
│   ├── controller/
│   │   ├── AdminNotificationController.java  # [改造] 扩展为完整通知 CRUD
│   │   └── AdminInsightsController.java      # [改造] 漏斗真实统计
│   ├── service/
│   │   ├── NotificationService.java          # [新增] 通知写入/查询/标记已读
│   │   └── FunnelService.java                # [新增] 漏斗聚合逻辑
│   ├── entity/
│   │   └── Notification.java                 # [新增] 站内通知实体
│   ├── mapper/
│   │   └── NotificationMapper.java           # [新增] 通知 Mapper
│   ├── mapper/cashier/
│   │   └── OrderMapper.java                  # [修改] 新增漏斗聚合 SQL
│   ├── dto/
│   │   ├── NotificationDTO.java              # [新增] 通知列表 DTO
│   │   └── FunnelResult.java                 # [新增] 漏斗结果 DTO
│   └── task/
│       ├── ChurnDetectionTask.java           # [修改] 超时预警写入通知
│       └── NotificationCleanupTask.java      # [新增] 90天已读通知清理
├── src/main/resources/db/migration/admin/
│   └── V9__notification_center.sql           # [新增] Flyway 迁移

payflow-admin-client/                         # 前端主战场
├── src/api/admin.ts                          # [修改] 新增通知 API 封装
├── src/pages/admin/
│   ├── layout.vue                            # [修改] 顶栏 Bell 动态化
│   ├── notifications.vue                     # [改造] 完整通知列表页
│   └── insights-funnel.vue                   # [改造] ECharts 漏斗图
├── src/components/
│   └── NotificationPopover.vue               # [新增] 铃铛下拉面板
└── src/composables/
    └── useNotification.ts                    # [新增] 通知轮询 composable

sql/schema/payflow_admin.sql                  # [修改] 追加 admin_notifications DDL
sql/seed/payflow_admin_seed.sql               # [修改] 追加通知 demo 数据
```

**Structure Decision**: 不新增 Maven 模块。通知中心和漏斗均为 admin-server 运营管理职能，符合模块 I 原则。漏斗查询通过既有 `CashierDataSourceConfig` + `OrderMapper` 跨库读取 cashier 数据，不引入新数据源。

## Complexity Tracking

> 无宪法违规豁免需求。
