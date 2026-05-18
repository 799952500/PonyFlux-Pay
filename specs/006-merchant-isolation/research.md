# 研究报告：商户数据隔离与水平越权急修

**Feature**: 006-merchant-isolation  
**Date**: 2026-05-18

## 1. 认证上下文统一

### Decision: 引入 `MerchantContext`（ThreadLocal）替代分散的 `request.getAttribute("merchantId")`

**Rationale**: 当前 `JwtAuthInterceptor` 与 `MerchantSignatureInterceptor` 均使用相同属性名 `merchantId`，但 Controller 各自读取，且 `OrderController` 允许请求体覆盖。统一 Context 后，绑定校验、所有权拦截、MyBatis 拦截器均可从同一来源读取，并在 `afterCompletion` 清理。

**Alternatives considered**:
- 继续仅用 `HttpServletRequest` 属性 — 无法在 MyBatis 层访问，且易遗漏清理。
- Spring Security `SecurityContextHolder` — 引入过重，与项目现有 JWT 拦截器模式不一致。

### Decision: 拦截器执行顺序

```
MerchantSignatureInterceptor / JwtAuthInterceptor  (认证)
  → MerchantContextInterceptor                     (写入 Context)
  → MerchantIdBindingInterceptor                   (merchantId 字段校验)
  → MerchantResourceOwnershipInterceptor           (资源所有权)
  → (业务) MerchantRateLimit / PaymentIdempotency
```

**Rationale**: 先认证再授权；`merchantId` 字段校验在所有权之前，可快速拒绝明显伪造身份请求（403），减少无效 DB 查询。

## 2. 错误响应策略（已澄清）

### Decision: 双轨响应

| 场景 | HTTP | 业务码 | 对外 message |
|------|------|--------|--------------|
| 请求体/query `merchantId` ≠ 上下文 | 403 | 5101 | 商户身份与请求不匹配 |
| 资源不存在或资源 ID 越权（含跨商户写） | 404 | 5102 | 请求的资源不存在 |
| 审计内部记录写操作越权 | — | 5103 | 不返回给客户端 |

**Rationale**: 403 仅用于「身份字段伪造」，不暴露资源是否存在；资源级越权统一 404 防止 BOLA 枚举。

**Alternatives considered**: 全部使用 403 — 易被攻击者用于探测资源 ID 是否存在。

## 3. 资源所有权拦截

### Decision: 专用 `MerchantResourceOwnershipInterceptor` + 查表服务 `ResourceOwnershipService`

**Rationale**: 避免在每个 Controller 重复校验。拦截器根据 URI 模板匹配路径变量，调用轻量 `SELECT merchant_id FROM ... WHERE id = ?`（仅查一列，走主键索引）。

**Alternatives considered**:
- AOP `@PreAuthorize` — 需 Spring Security。
- 仅在 Service 层校验 — 易遗漏新增 Controller 端点。

### Decision: 资源不存在与无权限均返回 404

**Rationale**: 查表结果为 null（不存在）或 `merchant_id` 不匹配（无权限）时，均抛出相同 `BizException(5102, "请求的资源不存在")`，HTTP 404。

## 4. MyBatis 持久层隔离

### Decision: 使用 MyBatis-Plus `InnerInterceptor` + JSQLParser 实现 `MerchantScopeInnerInterceptor`

**Rationale**: cashier-server 当前**未配置** `MybatisPlusInterceptor`（代码库检索无匹配）。可新建 `MybatisPlusConfig`，注册自定义 `InnerInterceptor`，与项目已依赖的 MP 3.5.7 一致。参考 `TenantLineInnerInterceptor` 实现思路，但自定义以支持 INSERT 校验/补全。

**Alternatives considered**:
- 原生 MyBatis `Interceptor`（`StatementHandler`）— 需手写 SQL 解析，维护成本高。
- 仅依赖业务代码 — spec 明确要求纵深防御（US3）。

### Decision: INSERT 行为

- 无 `MerchantContext` 且非系统模式 → 拒绝执行（`BizException`）。
- 有上下文：SQL 已含 `merchant_id` → 校验相等；未含 → JSQLParser 注入列与值。

### Decision: 系统模式 `MerchantScopeHolder.runInSystemMode(Runnable)`

**Rationale**: try-finally 设置 `ThreadLocal<Boolean> SKIP=true`，满足 FR-009。豁免点控制在 ≤10 处并在 PR 描述理由。

**已知豁免场景**:
1. `PayNotifyController` / 渠道回调解析 payment
2. admin-server 带 `X-Payflow-Internal-Token` 的内部调用（若在 cashier 落库）
3. XXL-Job / 定时关单查单
4. 单元测试 `@BeforeEach` 显式开启

## 5. 安全审计存储与查询

### Decision: 表位于 `payflow_cashier.cashier_security_audit`；admin 通过既有 cashier 数据源只读

**Rationale**: 越权事件发生在 cashier 请求路径，数据归属交易库。admin-server 已有 `CashierDataSourceConfig` + `mapper.cashier` 包（如 `OrderMapper`），新增 `SecurityAuditMapper` 同模式即可，**禁止跨库 JOIN**。

**Alternatives considered**:
- 同步写入 admin 库 — 双写一致性复杂。
- 仅日志不落库 — 无法满足 US4 管理端查询。

### Decision: 异步写入

**Rationale**: FR-014 要求写入失败不阻断拒绝路径。使用 `@Async` 或 Spring Event + `@EventListener` 异步插入。

### Decision: 告警阈值可配置

默认 `payflow.security.audit.alert-threshold=20`，窗口 `alert-window-minutes=5`，超限打 WARN 日志（对接现有日志/指标，本阶段不强制 Prometheus 规则）。

## 6. API 覆盖范围（已澄清）

### Decision: Phase 0 一次性覆盖

- JWT: `/api/v1/orders/**`
- HMAC: `/api/v1/merchant/**`、`/api/v1/payments/**`、`/api/v1/refunds/**`、`/api/v1/payment-links/**`
- 白名单: `/api/v1/auth/**`、`/api/v1/cashier/**`、`/api/v1/public/**`、`/notify/**`、`/api/v1/callbacks/**`、`/swagger-ui/**`、`/actuator/**`

**注意**: `WebMvcConfig` 中 JWT 仅注册 `orders/**`，payments/refunds 走 HMAC — 与 spec 一致，无需给 orders 加 HMAC。

## 7. 管理端 RBAC

### Decision: 复用 `@RequireRole` + `RoleBasedInterceptor`

**Rationale**: `SystemConfigController` 等已使用 `@RequireRole({"SUPER_ADMIN"})`。审计 API 使用 `@RequireRole({"RISK", "SUPER_ADMIN"})`。

## 8. CI 静态检查（FR-020）

### Decision: Phase 0 采用 ArchUnit 或 Maven 插件扫描规则（二选一，plan 推荐 ArchUnit 测试类）

**Rationale**: 在 `payflow-cashier-server` 的 test 源中增加 `MerchantControllerArchTest`：凡 `@RestController` 且方法含 `@PathVariable` 且参数名匹配 `.*Id$`，则类必须在 `MerchantResourceOwnershipInterceptor` 的路径配置表中，或方法调用 `ResourceOwnershipService`。

**Alternatives considered**: Checkstyle 自定义 — 表达力不足。

## 9. 文档语言

### Decision: 本特性所有 spec-kit 产物使用中文（澄清会话确认）

专业术语、代码标识符、URL 路径保留原文。
