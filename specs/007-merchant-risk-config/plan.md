# Implementation Plan: 商户级风控配置

**Branch**: `007-merchant-risk-config` | **Date**: 2026-05-20 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/007-merchant-risk-config/spec.md`

**Note**: 本模板由 `/speckit-plan` 命令填充。执行工作流见 `.specify/templates/plan-template.md`。

## Summary

将现有全局风控升级为商户级风控：管理员可管理平台规则并指定全商户或部分商户生效；商户用户只能管理仅对自己生效的自建规则；支付请求在创建订单前只评估对当前商户生效的启用规则，并记录命中与配置变更审计。技术结论：沿用现有 `risk_rules` / `cashier_risk_rules` 风控能力，补齐归属来源、作用范围、命中记录、审计记录和商户端隔离 API；阈值继续按分存储与比较，避免 BigDecimal 元/分转换风险。

## Technical Context

**Language/Version**: Java 17 / TypeScript 5.x
**Primary Dependencies**: Spring Boot 3.2.5 / MyBatis-Plus 3.5.7 / Vue 3.4 / Element Plus / Axios
**Storage**: MySQL（`payflow_admin` + `payflow_cashier` 双库）/ Redis（配置刷新、可选缓存）
**Testing**: JUnit 5 + Mockito / H2 或 Testcontainers / Vitest + Vue Test Utils
**Target Platform**: Linux Server (JVM 17) / Web Browser
**Project Type**: web-service + web-app
**Performance Goals**: 100 个商户、1,000 条启用规则下，95% 支付请求风控评估保持用户无明显等待感；命中记录 1 分钟内可查询。
**Constraints**: 商户隔离 100% 生效；金额阈值统一 Long 分；所有 API 返回 `{ code, message, data }`；不得泄露敏感规则细节；分页上限不超过 500。
**Scale/Scope**: 覆盖 admin-server 管理端 API、cashier-server 支付前风控执行、admin-client 风控页面、SQL schema/seed/migration、契约文档与测试。

## Constitution Check

*GATE: 必须在 Phase 0 研究前通过。Phase 1 设计完成后复查。*

| # | 宪法原则 | 状态 | 说明 |
|---|----------|------|------|
| 1 | I. 模块边界纪律 — 代码是否放在正确的 Maven 模块中？ | [x] PASS / [ ] VIOLATION | 管理与配置在 `payflow-admin-server`，支付前风控执行在 `payflow-cashier-server`，前端在 `payflow-admin-client`，不新增模块。 |
| 2 | II. 支付渠道抽象 — 是否通过 Locator/Registry 访问渠道？ | [x] PASS / [ ] VIOLATION / [ ] N/A | 风控在进入支付渠道前执行，不直接注入任何渠道 Handler，也不修改渠道抽象。 |
| 3 | III. 数据库分区 — 新增表前缀是否为 admin_/cashier_/recon_？ | [x] PASS / [ ] VIOLATION / [ ] N/A | 配置主数据仍在 admin 库；新增审计/命中表使用 `admin_` 前缀；如保留 cashier 运行时规则镜像，使用 `cashier_` 前缀。 |
| 4 | IV. API 响应规范 — 是否返回统一 `{ code, message, data }` 格式？ | [x] PASS / [ ] VIOLATION | 所有新增管理端与商户端 API 统一响应结构。 |
| 5 | V. 密钥与配置安全 — 敏感数据是否加密存储、密钥来自环境变量？ | [x] PASS / [ ] VIOLATION / [ ] N/A | 不新增密钥；规则表达式、阈值和命中摘要按敏感运营配置处理，限制权限并审计。 |
| 6 | 编码规范 — 命名/格式/注释/成员顺序是否合规？ | [x] PASS / [ ] VIOLATION | DTO/VO/Entity/Service 命名按现有规范，公共类和公共方法补中文 Javadoc。 |
| 7 | 数据库访问 — 禁止 SELECT *、禁止 ${}、分页设限？ | [x] PASS / [ ] VIOLATION / [ ] N/A | 使用 MyBatis-Plus Lambda 查询，分页设置上限，动态排序白名单。 |
| 8 | 安全编码 — 日志脱敏/参数校验/防重放是否到位？ | [x] PASS / [ ] VIOLATION | Controller DTO 使用 Bean Validation；商户 ID 从认证上下文获取；日志不打印完整请求敏感字段。 |
| 9 | 测试规范 — DoD 五条件 + 80% 覆盖率是否满足？ | [x] PASS / [ ] VIOLATION | 计划覆盖 Service 权限边界、规则范围匹配、支付拦截、审计与前端关键交互测试。 |

**Gate Result**: ALL PASS

## Post-Design Constitution Check

| # | 宪法原则 | 状态 | 说明 |
|---|----------|------|------|
| 1 | I. 模块边界纪律 | [x] PASS | `data-model.md` 与 `contracts/api.md` 均按 admin 配置、cashier 执行、admin-client 展示分层，无新增跨模块直接依赖。 |
| 2 | II. 支付渠道抽象 | [x] PASS | 设计只在订单创建前进行风控，不进入渠道 Handler，不改变 `PayStrategy` / Locator / Registry。 |
| 3 | III. 数据库分区 | [x] PASS | 数据模型明确 admin 配置/审计与 cashier 执行上下文，新增表命名按 `admin_` 或 `cashier_` 前缀。 |
| 4 | IV. API 响应规范 | [x] PASS | `contracts/api.md` 明确所有接口返回 `{ code, message, data }`。 |
| 5 | V. 密钥与配置安全 | [x] PASS | 契约要求不返回敏感规则细节，命中记录请求摘要需脱敏。 |
| 6 | 编码规范 | [x] PASS | 后续任务将按 DTO/VO/Entity 分离和中文 Javadoc 执行。 |
| 7 | 数据库访问 | [x] PASS | 计划要求分页限制、Lambda 查询、动态排序白名单、无跨库 JOIN。 |
| 8 | 安全编码 | [x] PASS | 商户 API 明确从认证上下文派生 merchantId，拒绝或忽略越权字段。 |
| 9 | 测试规范 | [x] PASS | quickstart 与 spec 覆盖权限边界、范围匹配、支付拦截、审计查询。 |

**Post-Design Gate Result**: ALL PASS

## Project Structure

### Documentation (this feature)

```text
specs/007-merchant-risk-config/
├── plan.md              # 本文件（/speckit-plan 输出）
├── research.md          # Phase 0 输出
├── data-model.md        # Phase 1 输出
├── quickstart.md        # Phase 1 输出
├── contracts/           # Phase 1 输出（API 契约）
└── tasks.md             # Phase 2 输出（/speckit-tasks 生成）
```

### Source Code (repository root)

```text
payflow-admin-server/
├── src/main/java/com/payflow/admin/controller/AdminRiskController.java
├── src/main/java/com/payflow/admin/entity/RiskRule.java
├── src/main/java/com/payflow/admin/mapper/RiskRuleMapper.java
├── src/main/java/com/payflow/admin/service/**/Risk*.java
└── src/main/java/com/payflow/admin/redis/CashierConfigRefreshPublisher.java

payflow-cashier-server/
├── src/main/java/com/payflow/cashier/service/impl/RiskCheckServiceImpl.java
├── src/main/java/com/payflow/cashier/service/impl/RiskRuleServiceImpl.java
├── src/main/java/com/payflow/cashier/entity/RiskRule.java
├── src/main/java/com/payflow/cashier/risk/RiskQlEvaluator.java
└── src/main/java/com/payflow/cashier/redis/CashierConfigRefreshSubscriber.java

payflow-admin-client/
├── src/pages/admin/risk.vue
├── src/api/admin.ts
└── src/types/index.ts

sql/
├── schema/payflow_admin.sql
├── seed/payflow_admin_seed.sql
└── migrations/YYYYMMDD-merchant-risk-config.sql
```

**Structure Decision**: 不新增 Maven 模块。`payflow-admin-server` 负责风控配置、作用范围、审计、查询契约；`payflow-cashier-server` 负责订单创建前实时规则筛选和拦截；`payflow-admin-client` 扩展现有风控配置页面；SQL 变更维护全量 schema、seed 与增量 migration。

## Complexity Tracking

> **仅在 Constitution Check 有违规且必须豁免时填写**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
