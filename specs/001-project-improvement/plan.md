# Implementation Plan: 项目健壮性改进计划

**Branch**: `001-project-improvement` | **Date**: 2026-05-10 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-project-improvement/spec.md`

## Summary

对 PonyFlux-Pay 项目进行全面健壮性改进，覆盖 12 个用户故事（P1-P3），从阻断性 bug 修复（微信回调崩溃、异常泄露、MD5 密码）到基础设施完善（Docker、Flyway、CI 测试、健康检查）。

## Technical Context

**Language/Version**: Java 17 (backend), TypeScript + Vue 3.4 (frontend)
**Primary Dependencies**: Spring Boot 3.2.5, MyBatis-Plus 3.5.7, Spring Security (BCryptPasswordEncoder), Flyway 10.x, SpringDoc OpenAPI 2.5.0, Spring Boot Actuator, JJWT 0.12.5, Redis (Jedis), Docker
**Storage**: MySQL 8.0 (`payflow_admin` + `payflow_cashier`), Redis 7
**Testing**: JUnit 5 + Mockito + `spring-boot-starter-test` (new coverage for common + payment-core)
**Target Platform**: Linux server (Docker containers)
**Project Type**: Multi-module Maven monorepo (9 backend modules + 2 Vue SPAs)
**Performance Goals**: N/A (健壮性/安全性改进，非性能优化)
**Constraints**: 所有变更必须向后兼容；双数据源不能破坏；前端 Axios baseURL 不变；API 响应格式不变
**Scale/Scope**: 3 后端服务 + 2 前端 SPA，约 12 个改进点

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 原则 | 状态 | 验证说明 |
|------|------|----------|
| I. 模块边界纪律 | ✅ 通过 | WxPayNotifyHelper 修复在 payflow-payment-wechat 模块；密码和 JWT 修改在 cashier-server；异常处理在各 server；前端仅改 admin/cashier-client |
| II. 支付渠道抽象 | ✅ 通过 | APIv3 密钥通过 `ChannelConfigHolder.getChannelConfig()` 获取，不直接引用实体 |
| III. 数据库分区 | ✅ 通过 | Flyway 按库分 migration 目录；密码字段在 `cashier_merchants`；不对跨库查询 |
| IV. API 响应规范 | ✅ 通过 | 异常处理器统一返回 `{code, message, data}` 格式 |
| V. 密钥安全 | ✅ 通过 | bcrypt 替换 MD5；JWT 密钥从环境变量注入；API 密钥从 ChannelConfig JSON 读取；prod 配置无明文 |

**Gate Result**: 全部通过，无需 Complexity Tracking 豁免。

## Project Structure

### Documentation (this feature)

```text
specs/001-project-improvement/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit-tasks)
```

### Source Code (repository root) — Changes Map

```text
# === 后端变更 ===
payflow-common/
└── src/test/java/com/payflow/common/      # [NEW] AesEncryptor 单元测试

payflow-payment-core/
└── src/test/java/com/payflow/payment/     # [NEW] PayStrategyRegistry 测试

payflow-payment-wechat/
└── src/main/java/.../WxPayNotifyHelper.java  # [FIX] 实现 APIv3 密钥读取

payflow-cashier-server/
├── src/main/java/.../exception/
│   └── GlobalExceptionHandler.java        # [FIX] RuntimeException → 通用消息
├── src/main/java/.../service/impl/
│   └── AuthServiceImpl.java              # [FIX] MD5 → bcrypt + 兼容升级
├── src/main/java/.../util/
│   └── JwtUtils.java                     # [FIX] 添加 jti 声明
├── src/main/java/.../middleware/
│   └── JwtAuthInterceptor.java           # [FIX] 检查 JWT 黑名单
├── src/main/java/.../controller/
│   └── AuthController.java               # [NEW] POST /auth/logout
├── src/main/resources/
│   ├── application-prod.yml              # [NEW] 生产配置模板
│   └── db/migration/cashier/             # [NEW] Flyway 迁移
└── pom.xml                                # [FIX] 添加 Flyway + Actuator 依赖

payflow-admin-server/
├── src/main/java/.../exception/
│   └── GlobalExceptionHandler.java       # [FIX] 确认无泄露
├── src/main/java/.../util/
│   └── JwtUtils.java                     # [FIX] 添加 jti + 登出端点
├── src/main/java/.../interceptor/
│   └── JwtInterceptor.java               # [FIX] 检查 JWT 黑名单
├── src/main/java/.../controller/         # [FIX] 添加 @Tag/@Operation 注解
├── src/main/resources/
│   ├── application-prod.yml              # [NEW] 生产配置模板
│   └── db/migration/admin/               # [NEW] Flyway 迁移
└── pom.xml                                # [FIX] 添加 SpringDoc + Flyway 依赖

payflow-recon-server/
├── src/main/java/.../exception/
│   └── GlobalExceptionHandler.java       # [FIX] RuntimeException → 通用消息
├── src/main/resources/
│   ├── application-prod.yml              # [NEW] 生产配置模板
│   └── db/migration/recon/               # [NEW] Flyway 迁移
└── pom.xml                                # [FIX] 添加 Flyway + Actuator 依赖

# === 前端变更 ===
payflow-admin-client/
├── src/api/request.ts                    # [FIX] fetch() → Axios
├── src/pages/admin/reconcile/*.vue       # [FIX] 添加 catch 错误处理
├── src/pages/admin/onboarding.vue        # [FIX] 添加 catch 错误处理
├── src/locales/zh-CN.ts                  # [FIX] 扩展 i18n 键值
├── src/locales/en-US.ts                  # [FIX] 扩展 i18n 键值
└── src/pages/admin/*.vue                 # [FIX] 硬编码中文 → $t()

payflow-cashier-client/
├── src/locales/                          # [NEW] i18n 文件
├── src/main.ts                           # [FIX] 引入 vue-i18n
└── src/pages/**/*.vue                    # [FIX] 硬编码中文 → $t()

# === 基础设施 ===
Dockerfile                                 # [NEW] 多阶段构建 ×3
docker-compose.yml                        # [NEW] 完整编排
.github/workflows/ci.yml                  # [FIX] 移除 -DskipTests，添加 mvn test
```

**Structure Decision**: 使用现有 Maven 多模块 + Vue 3 SPA 结构。新增文件按约定放置（Flyway 迁移在 `src/main/resources/db/migration/{库名}/`，测试在 `src/test/`）。

## Complexity Tracking

> 无违规项，无需豁免。
