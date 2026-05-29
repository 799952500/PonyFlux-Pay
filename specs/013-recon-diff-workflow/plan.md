# Implementation Plan: 对账差异处置工作流升级

**Branch**: `013-recon-diff-workflow` | **Date**: 2026-05-28 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/013-recon-diff-workflow/spec.md`

**Note**: 本模板由 `/speckit-plan` 命令填充。执行工作流见 `.specify/templates/plan-template.md`。

## Summary

将现有对账差异（`recon_diff`）从“列表展示 + 手工标记处理”升级为“工单 + SLA + 归因看板 + 长尾追踪 + 报告订阅”的业务闭环，确保每条差异都有责任人、有处置时限、有升级机制、有可运营统计。

- **差异工单化**：为每条差异建立一对一的工单扩展信息（负责人、状态机、SLA 截止时间、升级信息）；支持自动派单、认领、改派、进度推进、终态处置；全链路写入 `recon_handler_audit` 留痕。
- **SLA 监控与升级**：按差异类型配置 SLA；临近超时提醒负责人，超时自动升级到 `recon:manage` 角色并推送通知；支持临时关闭自动升级但保留提醒。
- **自动归因看板**：按渠道×差异类型矩阵、趋势、TOP N（商户/账号/类型）、处置时效分布；支持下钻到工单列表；30 天≤1 万条差异 P95 ≤2 秒。
- **长尾差异追踪**：按账龄 bucket 展示待处置差异；≥7 天长尾每日摘要推送；支持批量挂账（`ACCEPTED_LOSS`）并留痕。
- **报告订阅**：支持日报/周报订阅，生成快照并通过通知中心推送，口径与看板一致。

本期实现边界：不引入新服务/新 MQ/邮件短信通道；不做差异自动修复；不修改 `payflow-recon-server` 侧对账引擎。

## Technical Context

**Language/Version**: Java 17 / TypeScript 5.x
**Primary Dependencies**: Spring Boot 3.2.5 / MyBatis-Plus 3.5.7 / Vue 3.4 / Element Plus 2.5+ / Pinia 2.1 / ECharts 5.5（看板可视化）
**Storage**: MySQL（`payflow_admin` + `payflow_cashier` 双库）/ Redis
**Testing**: JUnit 5 + Mockito / Testcontainers / Vitest / Playwright
**Target Platform**: Linux Server (JVM 17) / Web Browser
**Project Type**: web-app（多模块支付网关 + 管理后台 SPA）
**Performance Goals**: 工单列表 ≤1.5s / 归因看板（30 天≤1 万条差异）P95 ≤2s / SLA 扫描周期 ≤5 分钟
**Constraints**: 不能破坏现有 `recon_diff` 查询/处理接口；所有操作必须遵循 `merchantScope` 数据隔离；不引入跨库 JOIN；分页 size 上限 ≤500
**Scale/Scope**: demo 环境 ~1 万条差异、~100 个商户、~20 个对账员

## Constitution Check

*GATE: 必须在 Phase 0 研究前通过。Phase 1 设计完成后复查。*

| # | 宪法原则 | 状态 | 说明 |
|---|----------|------|------|
| 1 | I. 模块边界纪律 — 代码是否放在正确的 Maven 模块中？ | [x] PASS | 后端全部落在 `payflow-admin-server`：对账属于管理后台职能；不修改 `payflow-recon-server` 引擎。前端落在 `payflow-admin-client` 对账模块页面。 |
| 2 | II. 支付渠道抽象 — 是否通过 Locator/Registry 访问渠道？ | [x] N/A | 不涉及支付渠道调用或 `PayStrategy`。 |
| 3 | III. 数据库分区 — 新增表前缀是否为 admin_/cashier_/recon_？ | [x] PASS | 新增表全部落在 `payflow_admin` 库，使用 `recon_` 命名空间（`recon_diff_assignment` 等）。不在 `payflow_cashier` 增表。 |
| 4 | IV. API 响应规范 — 是否返回统一 `{ code, message, data }` 格式？ | [x] PASS | 所有新增 Controller 端点返回 `ResponseEntity<Map<String, Object>>` 且包含 `code/message/data`。 |
| 5 | V. 密钥与配置安全 — 敏感数据是否加密存储、密钥来自环境变量？ | [x] N/A | 不新增密钥存储；仅新增 SLA/订阅配置与统计快照。 |
| 6 | 编码规范 — 命名/格式/注释/成员顺序是否合规？ | [x] PASS | 遵循现有 admin-server 代码风格；公共类/方法提供中文 Javadoc；不使用原始类型与不安全泛型。 |
| 7 | 数据库访问 — 禁止 SELECT *、禁止 ${}、分页设限？ | [x] PASS | MyBatis-Plus LambdaQuery + 明确字段；所有分页 size 上限 ≤500；不使用 `${}`。 |
| 8 | 安全编码 — 日志脱敏/参数校验/防重放是否到位？ | [x] PASS | 关键入参使用 Bean Validation；所有查询必须应用 `AdminRequestContext.merchantScope`；审计留痕覆盖所有写操作。 |
| 9 | 测试规范 — DoD 六条件、80% 覆盖率、按需 Playwright E2E 与后台日志闭环是否满足？ | [x] PASS | 计划包含：派单/状态机/SLA 计算单元测试，核心链路集成测试，以及对账工单页 + 长尾页 + 订阅页 Playwright 验证。 |

**Gate Result**: ALL PASS

## Project Structure

### Documentation (this feature)

```text
specs/013-recon-diff-workflow/
├── plan.md              # 本文件（/speckit-plan 输出）
├── research.md          # Phase 0 输出
├── data-model.md        # Phase 1 输出
├── quickstart.md        # Phase 1 输出
├── contracts/           # Phase 1 输出（如有接口契约）
└── tasks.md             # Phase 2 输出（/speckit-tasks 生成）
```

### Source Code (repository root)

```text
# 本特性涉及的模块：

payflow-admin-server/                         # 后端主战场（对账差异工作流）
├── src/main/java/com/payflow/admin/
│   ├── controller/
│   │   └── AdminReconController.java          # [扩展] 新增工单/指派/SLA/看板/报告相关端点
│   ├── service/
│   │   ├── AdminReconQueryService.java        # [扩展] 工单列表、长尾、看板聚合查询
│   │   ├── ReconDiffWorkflowService.java      # [新增] 派单/状态机/改派/挂账/升级编排
│   │   ├── ReconSlaService.java               # [新增] SLA 规则管理、due_at 计算、扫描逻辑
│   │   └── ReconReportService.java            # [新增] 订阅管理、快照生成、推送
│   ├── entity/recon/
│   │   ├── ReconDiffEntity.java               # [已有] 差异实体（保持兼容）
│   │   ├── ReconDiffAssignmentEntity.java     # [新增] 工单扩展一对一表
│   │   ├── ReconDiffSlaRuleEntity.java        # [新增] SLA 规则
│   │   ├── ReconReportSubscriptionEntity.java # [新增] 订阅
│   │   └── ReconReportSnapshotEntity.java     # [新增] 报告快照
│   ├── mapper/recon/
│   │   ├── ReconDiffAssignmentMapper.java     # [新增]
│   │   ├── ReconDiffSlaRuleMapper.java        # [新增]
│   │   ├── ReconReportSubscriptionMapper.java # [新增]
│   │   └── ReconReportSnapshotMapper.java     # [新增]
│   ├── task/
│   │   ├── ReconSlaScanTask.java              # [新增] due-soon/overdue 扫描与升级
│   │   ├── ReconLongTailDigestTask.java       # [新增] 长尾摘要推送
│   │   └── ReconReportScheduleTask.java       # [新增] 日报/周报快照生成与推送
│   └── enums/
│       └── NotificationTypeEnum.java          # [扩展] 新增 recon 工作流通知类型

payflow-admin-client/                         # 前端主战场
├── src/api/admin.ts                           # [扩展] recon 工作流相关 API 封装
└── src/pages/admin/reconcile/                 # [扩展] 新增/改造工单、看板、长尾、订阅页

payflow-admin-server/src/main/resources/db/migration/admin/
└── Vxx__recon_diff_workflow.sql               # [新增] Flyway 迁移（新表 + 索引）

sql/schema/payflow_admin.sql                   # [修改] 追加 recon_* 新表 DDL
sql/seed/payflow_admin_seed.sql                # [修改] 追加 SLA 规则/订阅/示例差异数据
```

**Structure Decision**: 不新增 Maven 模块。对账差异处置属于管理后台运营治理能力，所有新逻辑落在 `payflow-admin-server`，并复用 012 通知中心能力。对账引擎（下载/解析/比对）仍由 `payflow-recon-server` 承担，本特性不修改其职责边界。

## Complexity Tracking

> 无宪法违规豁免需求。
