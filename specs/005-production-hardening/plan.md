# Implementation Plan: 生产环境加固

**Branch**: `005-production-hardening` | **Date**: 2026-05-15 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/005-production-hardening/spec.md`

**Note**: 本模板由 `/speckit-plan` 命令填充。执行工作流见 `.specify/templates/plan-template.md`。

## Summary

本次生产环境加固基于对系统 4 个维度（安全/业务逻辑/运维/数据完整性）的全面扫描，共发现 70+ 个具体问题，归纳为 10 个用户故事和 31 条功能需求。核心目标：修复未鉴权接口、JWT 密钥硬编码、支付回调验签缺失、数据完整性缺陷（payAmount=0、退款竞态、SQL 列名错误）、HTTP 超时缺失、Webhook 空实现、密钥明文存储等关键问题，使系统达到商用安全标准。

## Technical Context

**Language/Version**: Java 17 / TypeScript 5.3+ / Vue 3.4
**Primary Dependencies**: Spring Boot 3.2.5 / MyBatis-Plus 3.5.7 / Element Plus 2.5+ / ECharts 5.5
**Storage**: MySQL 8.x（`payflow_admin` + `payflow_cashier` 双库）/ Redis
**Testing**: JUnit 5 + Mockito / (前端) Vitest + Vue Test Utils
**Target Platform**: Linux Server (JVM 17) / Web Browser
**Project Type**: web-service + web-app (Maven 多模块 + Vue 3 前端)
**Performance Goals**: 支付请求 P95 < 2s，Webhook 首次投递 < 1min，优雅关闭 < 30s
**Constraints**: 不得破坏现有 API 契约；向后兼容现有前端；敏感字段脱敏必须在序列化层统一处理
**Scale/Scope**: 12 个 Maven 模块 + 2 个前端项目，修改约 40-60 个文件

## Constitution Check

*GATE: 必须在 Phase 0 研究前通过。Phase 1 设计完成后复查。*

| # | 宪法原则 | 状态 | 说明 |
|---|----------|------|------|
| 1 | I. 模块边界纪律 — 代码是否放在正确的 Maven 模块中？ | PASS | 认证/授权修复在 admin-server；支付完整性在 cashier-server + admin-server；验签在 payment-channels；Webhook 在 cashier-server |
| 2 | II. 支付渠道抽象 — 是否通过 Locator/Registry 访问渠道？ | PASS | 修复 AliPayNotifyHelper 验签需通过支付宝公钥配置；渠道验签修复不改变 Locator/Strategy 模式 |
| 3 | III. 数据库分区 — 新增表前缀是否为 admin_/cashier_/recon_？ | PASS | Webhook 投递记录表使用 `cashier_` 前缀（属交易数据）；无新增 admin_ 表 |
| 4 | IV. API 响应规范 — 是否返回统一 `{ code, message, data }` 格式？ | PASS | 所有新增/修改的 Controller 端点保持统一响应格式；安全拦截器返回标准错误码 |
| 5 | V. 密钥与配置安全 — 敏感数据是否加密存储、密钥来自环境变量？ | PASS | 本功能直接实施密钥安全加固：AES-256-GCM 加密渠道密钥、JWT 密钥环境变量强制、内部 Token 生产环境校验 |
| 6 | 编码规范 — 命名/格式/注释/成员顺序是否合规？ | PASS | 新增代码遵循 lowerCamelCase、K&R 大括号、Javadoc 中文注释 |
| 7 | 数据库访问 — 禁止 SELECT *、禁止 ${}、分页设限？ | PASS | 修复 `AdminRefundService.applyMerchantScope()` 中的字符串拼接 SQL（改用参数化查询）；修复 `aggregateRefunds` 错误列名 |
| 8 | 安全编码 — 日志脱敏/参数校验/防重放是否到位？ | PASS | 本功能核心目标：新增敏感字段脱敏、DTO 校验注解、CSV 注入防护、安全响应头 |
| 9 | 测试规范 — DoD 五条件 + 80% 覆盖率是否满足？ | PASS | 新增功能均编写单元测试；修复现有测试回归；安全测试覆盖认证/授权/验签路径 |

**Gate Result**: ALL PASS

## Project Structure

### Documentation (this feature)

```text
specs/005-production-hardening/
├── plan.md              # 本文件（/speckit-plan 输出）
├── research.md          # Phase 0 输出
├── data-model.md        # Phase 1 输出
├── quickstart.md        # Phase 1 输出
├── contracts/           # Phase 1 输出（如有接口契约）
└── tasks.md             # Phase 2 输出（/speckit-tasks 生成）
```

### Source Code (repository root)

```text
# Java 后端：
payflow-common/              # 共享工具、异常、加密、常量
payflow-payment-core/        # 支付 SPI 接口、枚举、DTO
payflow-payment-channels/    # 渠道聚合器 POM
├── payflow-payment-wechat/  # 微信支付处理器
├── payflow-payment-alipay/  # 支付宝处理器（验签修复）
└── payflow-payment-union/   # 银联/云闪付处理器
payflow-cashier-server/      # 收银台服务（商户管控、Webhook、容错）
payflow-admin-server/        # 管理后台（认证授权、敏感字段过滤）
payflow-recon-server/        # 对账引擎
payflow-sdk-java/            # HMAC-SHA256 签名 SDK

# 前端：
payflow-admin-client/        # 管理后台前端（安全头、敏感字段展示）
payflow-cashier-client/      # 收银台前端
```

**Structure Decision**: 本功能不新增模块，所有修改均在现有模块内进行。Webhook 投递记录实体和 Mapper 放在 `payflow-cashier-server`（属交易数据）。敏感字段过滤逻辑放在 `payflow-admin-server` 的序列化配置层。

## Complexity Tracking

> **仅在 Constitution Check 有违规且必须豁免时填写**

无违规项。
