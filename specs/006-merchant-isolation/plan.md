# Implementation Plan: 商户数据隔离与水平越权急修

**Branch**: `006-merchant-isolation` | **Date**: 2026-05-18 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/006-merchant-isolation/spec.md`

**Note**: 本模板由 `/speckit-plan` 命令填充。所有设计产物使用中文撰写。

## Summary

本特性（Phase 0）针对收银台服务已存在的 **水平越权（IDOR/BOLA）** 漏洞进行紧急封堵，并建立三层纵深防御：

1. **Web 层**：统一 `MerchantContext`；`merchantId` 字段与认证上下文不一致 → HTTP 403 + `5101`；资源 ID 越权 → 统一 HTTP 404 + `5102`。
2. **资源层**：`MerchantResourceOwnershipInterceptor` 对 `orderId`/`paymentId`/`refundId`/`linkId` 做所有权校验。
3. **持久层**：`MerchantScopeInterceptor` 对商户业务表 SELECT/UPDATE/DELETE 自动追加 `merchant_id` 条件，INSERT 强制/校验 `merchant_id`。

同步交付 **安全审计全栈**：`cashier_security_audit` 表 + 异步写入 + admin-server 分页 API + admin-client 列表页。

**工期估算**：8–10 个工作日（含全栈审计与 ≥30 条安全自动化用例），高于最初「仅修越权 3–5 天」估计，因澄清会话确认包含管理端 UI。

## Technical Context

**Language/Version**: Java 17 / TypeScript 5.3+ / Vue 3.4
**Primary Dependencies**: Spring Boot 3.2.5 / MyBatis-Plus 3.5.7 / Element Plus 2.5+ / SpringDoc OpenAPI 2.5.0
**Storage**: MySQL 8.x（`payflow_cashier` 新增审计表；admin 通过既有 cashier 数据源只读查询）/ Redis（既有 JWT 黑名单、限流）
**Testing**: JUnit 5 + Mockito + `@SpringBootTest`；`MerchantIsolationSecurityTest` 专项类；admin-client Vitest（列表页 smoke）
**Target Platform**: Linux Server (JVM 17) / Web Browser
**Project Type**: web-service + web-app（Maven 多模块 + Vue 3 前端）
**Performance Goals**: 拦截器链额外开销 P95 < 5ms；审计异步写入不阻塞拒绝路径；admin 审计列表查询 P95 < 1s（百万行量级依赖索引）
**Constraints**: 不得破坏存量商户集成（merchantId 与上下文一致时行为不变）；公开端点白名单不变；错误响应不可区分「不存在」与「无权限」
**Scale/Scope**: 修改约 35–50 个文件；涉及 `payflow-cashier-server`、`payflow-admin-server`、`payflow-admin-client`；可选在 `payflow-common` 增加共享常量

## Constitution Check

*GATE: Phase 0 研究前通过。Phase 1 设计完成后复查。*

| # | 宪法原则 | 状态 | 说明 |
|---|----------|------|------|
| 1 | I. 模块边界纪律 | PASS | 隔离逻辑在 cashier-server；审计查询在 admin-server + admin-client；不在 payment-channels 引入业务依赖 |
| 2 | II. 支付渠道抽象 | N/A | 不涉及支付渠道改造 |
| 3 | III. 数据库分区 | PASS | 新表 `cashier_security_audit` 使用 `cashier_` 前缀，Flyway 路径 `db/migration/cashier/` |
| 4 | IV. API 响应规范 | PASS | 403/404 均返回 cashier `R<T>` 或 admin `Map{code,message,data}` 统一格式 |
| 5 | V. 密钥与配置安全 | PASS | 审计表不存 appSecret；MerchantContext 只读；系统模式豁免需 PR 说明 |
| 6 | 编码规范 | PASS | 中文 Javadoc；`@RequiredArgsConstructor`；类成员顺序合规 |
| 7 | 数据库访问 | PASS | 审计查询分页 `maxLimit`；Mapper 使用 Lambda；禁止 `${}` |
| 8 | 安全编码 | PASS | 本特性核心即为安全编码；审计异步写入 fail-open 仅记日志 |
| 9 | 测试规范 | PASS | 新增 ≥30 安全用例 + 回归；目标模块覆盖率 ≥80% |

**Gate Result**: ALL PASS

**Phase 1 复查**: 数据模型与契约已对齐 spec 澄清结论（403/404 分流、INSERT 覆盖、全栈审计），无新增违规项。

## Project Structure

### Documentation (this feature)

```text
specs/006-merchant-isolation/
├── plan.md              # 本文件
├── research.md          # Phase 0 技术决策
├── data-model.md        # Phase 1 数据模型
├── quickstart.md        # Phase 1 验证手册
├── contracts/           # Phase 1 API 契约
│   └── api-contracts.md
└── tasks.md             # Phase 2（/speckit-tasks 生成）
```

### Source Code（本特性涉及）

```text
payflow-cashier-server/
├── src/main/java/com/payflow/cashier/
│   ├── context/                    # 新增 MerchantContext、MerchantScopeHolder
│   ├── middleware/                 # 新增/改造拦截器
│   │   ├── MerchantContextInterceptor.java      # 统一上下文注入
│   │   ├── MerchantIdBindingInterceptor.java    # 请求体 merchantId 校验
│   │   ├── MerchantResourceOwnershipInterceptor.java
│   │   └── (改造) JwtAuthInterceptor、MerchantSignatureInterceptor
│   ├── mybatis/                    # MerchantScopeInnerInterceptor
│   ├── service/SecurityAuditService.java
│   ├── entity/SecurityAuditEntity.java
│   ├── mapper/SecurityAuditMapper.java
│   └── controller/                 # 改造 5 个 Controller
├── src/main/resources/db/migration/cashier/
│   └── V4__cashier_security_audit.sql
└── src/test/java/.../MerchantIsolationSecurityTest.java

payflow-admin-server/
├── src/main/java/com/payflow/admin/
│   ├── controller/AdminSecurityAuditController.java
│   ├── service/AdminSecurityAuditService.java
│   ├── mapper/cashier/SecurityAuditMapper.java   # cashier 数据源
│   └── entity/cashier/SecurityAuditEntity.java
└── src/test/java/.../AdminSecurityAuditControllerTest.java

payflow-admin-client/
├── src/views/security-audit.vue    # 新增
├── src/api/securityAudit.ts        # 新增
└── src/router/index.ts             # 注册路由 + 菜单种子
```

**Structure Decision**: 不新增 Maven 模块。`MerchantContext` 放在 cashier-server 内（本特性核心）；若后续 Phase 3a 子账号需跨模块复用，再抽取至 `payflow-common`。admin 侧审计 Mapper 复用既有 `CashierDataSourceConfig` 双数据源模式（与 `OrderMapper` 相同）。

## Implementation Phases（实施分期）

### Wave A：上下文与 Web 层封堵（第 1–3 天）

| 任务 | 说明 |
|------|------|
| A1 | 新增 `MerchantContext`（ThreadLocal + `clear()`）；`authMode`: JWT / HMAC / INTERNAL |
| A2 | `MerchantContextInterceptor`：在 JWT/HMAC 拦截器之后运行，将 `request.getAttribute("merchantId")` 写入 Context |
| A3 | `MerchantIdBindingInterceptor`：解析 query/body 中的 `merchantId`，不一致 → 403 + `5101` + 触发审计 |
| A4 | 修复 `OrderController.createOrder`：删除「以传入 merchantId 为准」逻辑；统一 `MerchantContext.getMerchantId()` |
| A5 | `MerchantResourceOwnershipInterceptor`：配置路径模式 → 资源类型 → 查表得 `merchant_id` → 比对 |
| A6 | 改造 `RefundController`、`PaymentController`、`MerchantQueryController`、`PaymentLinkController` |

**资源所有权查表映射**：

| 路径变量 | 表 | 主键列 |
|----------|-----|--------|
| `orderId` | `cashier_orders` | `order_id` |
| `paymentId` | `cashier_payments` | `payment_id` |
| `refundId` | `cashier_refunds` | `refund_id` |
| `linkId` | `cashier_payment_link` | `link_id` |

### Wave B：持久层纵深防御（第 4–5 天）

| 任务 | 说明 |
|------|------|
| B1 | 新增 `MybatisPlusConfig`（cashier-server 当前无 InnerInterceptor 配置） |
| B2 | 实现 `MerchantScopeInnerInterceptor`（JSQLParser 改写 WHERE / INSERT 列） |
| B3 | `MerchantScopeHolder`：`runInSystemMode(Runnable)` / `runWithMerchant(merchantId, Runnable)` + try-finally |
| B4 | 梳理并标注 ≤10 处系统模式豁免点（回调、internal token、XXL 任务、测试） |
| B5 | DEBUG 日志：原始 SQL vs 改写后 SQL（仅 dev/staging 默认开启） |

**受保护表清单**（`merchant_id` 列存在）：

- `cashier_orders`
- `cashier_payments`
- `cashier_refunds`
- `cashier_webhook_endpoint`（若存在）
- `cashier_webhook_delivery`（若存在）
- `cashier_payment_link`

### Wave C：安全审计全栈（第 6–8 天）

| 任务 | 说明 |
|------|------|
| C1 | Flyway `V4__cashier_security_audit.sql` |
| C2 | `SecurityAuditService.recordDenied(...)` 异步（`@Async` 或 `ApplicationEventPublisher`） |
| C3 | admin `AdminSecurityAuditController` + `@RequireRole({"RISK","SUPER_ADMIN"})` |
| C4 | admin-client `security-audit.vue`：筛选 + 分页表格 |
| C5 | 菜单种子 SQL / 文档：「系统管理 → 安全审计」 |

### Wave D：测试、文档与 CI（第 9–10 天）

| 任务 | 说明 |
|------|------|
| D1 | `MerchantIsolationSecurityTest`：≥30 用例（跨商户读/写/merchantId 不一致/兼容路径） |
| D2 | 更新 `docs/CONTRACT_MATRIX.md` 错误码 5101–5103、新 admin API |
| D3 | Swagger 注解：`merchantId` 字段标记 `deprecated` |
| D4 | CI：新增脚本或 ArchUnit 规则检查 Controller `@PathVariable *Id` 覆盖（FR-020） |
| D5 | 全量 `mvn -B test` 回归 |

## Complexity Tracking

无宪法违规项。

## Risk & Mitigation

| 风险 | 缓解 |
|------|------|
| JSQLParser 改写复杂 SQL 失败 | 白名单仅覆盖简单 Mapper；复杂手写 XML 显式写 `merchant_id`；单测覆盖 |
| 系统模式遗漏导致回调失败 | 回调入口统一 `MerchantScopeHolder.runInSystemMode`；集成测试覆盖 notify |
| 统一 404 影响商户排错 | 对外 message 统一；审计表记真实原因 `5103`；管理端可查 |
| ThreadLocal 泄漏 | `MerchantContextInterceptor.afterCompletion` 与 `Holder.clear()` 双保险 |
| admin 跨库查询性能 | 索引 `(merchant_id, created_at)`；分页强制 limit ≤100 |

## Dependencies

- 可与 `005-production-hardening` 并行，但 **JWT fail-close** 改造后需重跑本特性安全测试
- 不依赖 Phase 1 手续费核算引擎
- 后续 Phase 3a 子账号将把 `MerchantContext` 扩展 `userId`/`roles`，本 Phase 预留字段但不实现
