# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]
**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

**Note**: 本模板由 `/speckit-plan` 命令填充。执行工作流见 `.specify/templates/plan-template.md`。

## Summary

[从 spec.md 提取：主要需求 + 研究方向的技术结论]

## Technical Context

**Language/Version**: Java 17 / [其他语言]
**Primary Dependencies**: Spring Boot 3.2.5 / MyBatis-Plus 3.5.7 / Vue 3.4 / [其他]
**Storage**: MySQL（`payflow_admin` + `payflow_cashier` 双库）/ Redis / [其他]
**Testing**: JUnit 5 + Mockito / Testcontainers / Vitest
**Target Platform**: Linux Server (JVM 17) / Web Browser
**Project Type**: [web-service / web-app / library / cli / mobile-app]
**Performance Goals**: [e.g., 1000 req/s, 10000 concurrent]
**Constraints**: [e.g., <200ms p95, <100MB memory]
**Scale/Scope**: [e.g., 10k users, 1M LOC, 50 screens]

## Constitution Check

*GATE: 必须在 Phase 0 研究前通过。Phase 1 设计完成后复查。*

| # | 宪法原则 | 状态 | 说明 |
|---|----------|------|------|
| 1 | I. 模块边界纪律 — 代码是否放在正确的 Maven 模块中？ | [ ] PASS / [ ] VIOLATION | |
| 2 | II. 支付渠道抽象 — 是否通过 Locator/Registry 访问渠道？ | [ ] PASS / [ ] VIOLATION / [ ] N/A | |
| 3 | III. 数据库分区 — 新增表前缀是否为 admin_/cashier_/recon_？ | [ ] PASS / [ ] VIOLATION / [ ] N/A | |
| 4 | IV. API 响应规范 — 是否返回统一 `{ code, message, data }` 格式？ | [ ] PASS / [ ] VIOLATION | |
| 5 | V. 密钥与配置安全 — 敏感数据是否加密存储、密钥来自环境变量？ | [ ] PASS / [ ] VIOLATION / [ ] N/A | |
| 6 | 编码规范 — 命名/格式/注释/成员顺序是否合规？ | [ ] PASS / [ ] VIOLATION | |
| 7 | 数据库访问 — 禁止 SELECT *、禁止 ${}、分页设限？ | [ ] PASS / [ ] VIOLATION / [ ] N/A | |
| 8 | 安全编码 — 日志脱敏/参数校验/防重放是否到位？ | [ ] PASS / [ ] VIOLATION | |
| 9 | 测试规范 — DoD 六条件、80% 覆盖率、按需 Playwright E2E 与后台日志闭环是否满足？ | [ ] PASS / [ ] VIOLATION | |

**Gate Result**: [ALL PASS / VIOLATIONS FOUND — 必须在 Phase 0 前解决]

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # 本文件（/speckit-plan 输出）
├── research.md          # Phase 0 输出
├── data-model.md        # Phase 1 输出
├── quickstart.md        # Phase 1 输出
├── contracts/           # Phase 1 输出（如有接口契约）
└── tasks.md             # Phase 2 输出（/speckit-tasks 生成）
```

### Source Code (repository root)

```text
# PonyFlux-Pay 项目为 Maven 多模块 + 多前端结构

# Java 后端：
payflow-common/              # 共享工具、异常、加密、常量
payflow-payment-core/        # 支付 SPI 接口、枚举、DTO
payflow-payment-channels/    # 渠道聚合器 POM
├── payflow-payment-wechat/  # 微信支付处理器
├── payflow-payment-alipay/  # 支付宝处理器
└── payflow-payment-union/   # 银联/云闪付处理器
payflow-cashier-server/      # 收银台服务（port 3002）
payflow-admin-server/        # 管理后台（port 3003）
payflow-recon-server/        # 对账引擎（port 3004）
payflow-sdk-java/            # HMAC-SHA256 签名 SDK

# 前端：
payflow-admin-client/        # 管理后台前端（Vue 3 dev :3001）
payflow-cashier-client/      # 收银台前端（Vue 3 dev :5173）
```

**Structure Decision**: [说明本功能涉及哪些模块，如果新增模块需要说明理由]

## Complexity Tracking

> **仅在 Constitution Check 有违规且必须豁免时填写**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 第4个项目] | [当前需求] | [为什么3个项目不够] |
