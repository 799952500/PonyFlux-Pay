# Implementation Plan: 商户数据隔离治理

**Branch**: `008-merchant-data-isolation` | **Date**: 2026-05-21 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/008-merchant-data-isolation/spec.md`

**Note**: 本计划由 `/speckit-plan` 命令填充。执行工作流见 `.specify/templates/plan-template.md`。

## Summary

本功能围绕“商户管理员只能运维授权商户数据、系统管理员保留平台级治理能力、全局配置不误隔离”展开。计划先建立数据分类与授权范围基线，再覆盖后台、收银、对账、前端管理页面、导出、统计、批量和异步入口，形成可验证的数据隔离治理闭环。研究结论选择“商户级 / 全局级 / 系统审计 / 待人工确认”分类模型，并以服务端授权范围作为唯一可信隔离边界。

## Technical Context

**Language/Version**: Java 17 / TypeScript  
**Primary Dependencies**: Spring Boot 3.2.5 / MyBatis-Plus 3.5.7 / Vue 3.4 / Element Plus / Axios  
**Storage**: MySQL（`payflow_admin` + `payflow_cashier` 双库）/ Redis  
**Testing**: JUnit 5 + Mockito / Testcontainers / Vitest / Playwright  
**Target Platform**: Linux Server (JVM 17) / Web Browser  
**Project Type**: web-service + web-app  
**Performance Goals**: 商户管理员常用页面 90% 以上无需手工选择商户即可自动限定授权范围；系统管理员 3 分钟内定位商户级数据归属、风险状态和最近关键操作记录。  
**Constraints**: 不改变统一响应结构；不破坏 admin/cashier/recon 数据库分区；不将全局配置强制私有化；跨商户拒绝不得泄露目标数据是否存在；涉及 UI 或跨服务流程必须按需执行 Playwright 与后台日志闭环。  
**Scale/Scope**: 覆盖 admin-server、cashier-server、recon-server、admin-client 中订单、支付、退款、渠道账号、路由、商户配置、风控、对账、统计、导出、审计、用户权限等商户相关入口。

## Constitution Check

*GATE: 必须在 Phase 0 研究前通过。Phase 1 设计完成后复查。*

| # | 宪法原则 | 状态 | 说明 |
|---|----------|------|------|
| 1 | I. 模块边界纪律 — 代码是否放在正确的 Maven 模块中？ | PASS | 计划按 admin/cashier/recon/common/frontend 职责拆分，禁止跨模块直接引用实体。 |
| 2 | II. 支付渠道抽象 — 是否通过 Locator/Registry 访问渠道？ | PASS | 不新增渠道；支付与退款隔离检查保持现有 Locator/Registry 与渠道抽象边界。 |
| 3 | III. 数据库分区 — 新增表前缀是否为 admin_/cashier_/recon_？ | PASS | 如需治理记录或补充字段，按运营配置、交易、对账归属分别落在对应库和前缀。 |
| 4 | IV. API 响应规范 — 是否返回统一 `{ code, message, data }` 格式？ | PASS | 新增或调整后台契约继续使用统一业务响应结构。 |
| 5 | V. 密钥与配置安全 — 敏感数据是否加密存储、密钥来自环境变量？ | PASS | 商户密钥、渠道账号密钥、回调配置等按商户级敏感数据处理，输出脱敏。 |
| 6 | 编码规范 — 命名/格式/注释/成员顺序是否合规？ | PASS | 后续任务按项目 Java/TypeScript 规范执行。 |
| 7 | 数据库访问 — 禁止 SELECT *、禁止 ${}、分页设限？ | PASS | 商户隔离查询必须使用明确字段、授权范围条件和分页上限。 |
| 8 | 安全编码 — 日志脱敏/参数校验/防重放是否到位？ | PASS | 重点防水平越权、参数绕过、日志泄露和跨商户批量操作。 |
| 9 | 测试规范 — DoD 六条件、80% 覆盖率、按需 Playwright E2E 与后台日志闭环是否满足？ | PASS | 计划覆盖单元、集成、多商户验收、Playwright 与日志闭环。 |

**Gate Result**: ALL PASS

## Project Structure

### Documentation (this feature)

```text
specs/008-merchant-data-isolation/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── api-contracts.md
└── tasks.md
```

### Source Code (repository root)

```text
# Java 后端：
payflow-common/              # 共享异常、加密、常量；必要时放跨模块安全上下文通用类型
payflow-payment-core/        # 支付 SPI；不放商户隔离业务实现
payflow-payment-channels/    # 渠道处理器；不直接引用 admin/cashier 实体
payflow-cashier-server/      # 商户签名、订单、支付、退款、回调、Payment Link、收银端安全审计
payflow-admin-server/        # 后台用户权限、商户/渠道配置、订单/退款/支付查询、对账 UI、统计、导出、审计
payflow-recon-server/        # 对账任务、账单、差异、处理审计与商户归属一致性
payflow-sdk-java/            # 外部商户签名 SDK；通常不涉及后台数据隔离

# 前端：
payflow-admin-client/        # 商户管理员与系统管理员的数据可见性、筛选、导出、治理页面
payflow-cashier-client/      # 收银台关键流程回归验证

# 数据库与脚本：
sql/schema/                  # payflow_admin / payflow_cashier 完整 DDL
sql/seed/                    # 多商户验收数据
sql/migrations/              # 隔离治理增量迁移
scripts/                     # demo 数据重置和验证脚本
```

**Structure Decision**: 本功能不新增顶层模块；后续实现应在现有 admin-server、cashier-server、recon-server 和 admin-client 内按业务职责修改。公共安全能力仅在确需跨服务复用时进入 `payflow-common`，不得把业务查询逻辑放入 common。

## Complexity Tracking

> 当前 Constitution Check 无违规，不需要豁免。

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|

## Phase 0: Research

已完成：[research.md](./research.md)

关键决策：
- 采用“商户级 / 全局级 / 系统审计 / 待人工确认”的数据分类基线。
- 以后台授权范围作为服务端可信边界，前端传参只能缩小范围。
- 治理范围覆盖 admin-server、cashier-server、recon-server 与 admin-client。
- 接口契约按数据范围、治理检查、业务入口、审计反馈记录。
- 验证策略采用多商户样本、入口矩阵、Playwright 与日志闭环。
- 存量数据缺失归属时默认限制访问或进入人工确认。

## Phase 1: Design & Contracts

已完成：
- [data-model.md](./data-model.md) — 定义商户、授权主体、商户级数据资源、全局配置、隔离检查项、操作审计记录及状态流转。
- [contracts/api-contracts.md](./contracts/api-contracts.md) — 定义后台数据范围、隔离检查结果、全局配置访问、审计反馈和验收入口矩阵。
- [quickstart.md](./quickstart.md) — 定义多商户验收、系统管理员治理、存量缺口、全局配置和 E2E 日志闭环验证路径。

## Post-Design Constitution Check

| # | 宪法原则 | 状态 | 说明 |
|---|----------|------|------|
| 1 | I. 模块边界纪律 | PASS | 设计明确不新增顶层模块，职责落在现有服务和前端项目。 |
| 2 | II. 支付渠道抽象 | PASS | 渠道账号、路由、支付与退款只做隔离约束，不绕过支付抽象。 |
| 3 | III. 数据库分区 | PASS | 数据模型按 admin/cashier/recon 归属设计，避免跨库 JOIN 要求。 |
| 4 | IV. API 响应规范 | PASS | 契约保留 `{ code, message, data }` 响应。 |
| 5 | V. 密钥与配置安全 | PASS | 契约要求敏感配置商户级隔离和脱敏输出。 |
| 6 | 编码规范 | PASS | 后续任务按项目规范拆分。 |
| 7 | 数据库访问 | PASS | 契约要求授权范围条件、分页限制和无跨商户批量越权。 |
| 8 | 安全编码 | PASS | 设计覆盖水平越权、参数绕过、安全拒绝和审计。 |
| 9 | 测试规范 | PASS | quickstart 明确 Playwright、后台日志和多商户验收闭环。 |

**Gate Result**: ALL PASS

## Phase 2: Planning Handoff

下一阶段 `/speckit-tasks` 应按以下工作包生成任务：
1. 数据清单与分类：扫描 admin/cashier/recon 表、Mapper、Controller、导出、统计、异步任务，建立隔离检查项。
2. 授权范围与上下文：统一后台商户授权范围解析，区分系统管理员和商户管理员。
3. 商户级查询整改：订单、支付、退款、渠道账号、路由、对账、风控、统计、导出、批量操作全部应用服务端范围限制。
4. 全局配置白名单：明确字典、基础渠道、菜单模板、系统配置等全局数据，并处理敏感摘要。
5. 存量数据治理：缺失归属的数据进入人工确认或限制访问，补充迁移与种子数据。
6. 前端体验：商户管理员自动限定范围，系统管理员可按商户筛选并看到归属。
7. 测试验收：单元测试、集成测试、多商户越权测试、Playwright 和后台日志闭环。
