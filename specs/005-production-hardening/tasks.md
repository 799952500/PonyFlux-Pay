# Tasks: 生产环境加固

**Input**: Design documents from `/specs/005-production-hardening/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: 本功能涉及安全加固，测试任务包含在内（spec 明确要求 SC-012：39 个现有测试 + 新增安全测试满足 80% 覆盖率）。

**Organization**: 任务按用户故事分组。模块边界：correct Maven module placement per Constitution Principle I.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行执行（不同文件，无依赖）
- **[Story]**: 归属的用户故事（US1-US10）
- 任务描述包含精确的文件路径

## Path Conventions

### 后端模块

| 模块 | 实际包路径 | 用途 |
|------|-----------|------|
| payflow-common | `payflow-common/src/main/java/com/payflow/common/` | 共享工具、异常、加密、常量 |
| payflow-payment-core | `payflow-payment-core/src/main/java/com/payflow/payment/core/` | 支付 SPI、枚举、DTO |
| payflow-payment-alipay | `payflow-payment-channels/payflow-payment-alipay/src/main/java/com/payflow/payment/alipay/` | 支付宝处理器 |
| payflow-cashier-server | `payflow-cashier-server/src/main/java/com/payflow/cashier/` | 收银台服务 |
| payflow-admin-server | `payflow-admin-server/src/main/java/com/payflow/admin/` | 管理后台 |

测试路径使用对应的 `src/test/java/com/payflow/...`。

---

## Phase 1: Setup（共享基础设施）

**Purpose**: 项目初始化和依赖配置

- [ ] T001 创建数据库迁移文件 `sql/migrations/2026-05-15_production-hardening.sql`——包含 `cashier_refunds.version` 列、复合索引 `(payment_id, status)`、唯一约束修复
- [ ] T002 [P] 添加 Micrometer Prometheus 依赖到父 POM `pom.xml`（`micrometer-registry-prometheus`）
- [ ] T003 [P] 在 `payflow-cashier-server/src/main/resources/application.yml` 和 `payflow-admin-server/src/main/resources/application.yml` 中配置 `server.shutdown=graceful` + `spring.lifecycle.timeout-per-shutdown-phase=30s`

---

## Phase 2: Foundational（阻断性前置条件）

**Purpose**: 必须在所有用户故事之前完成的核心基础设施

**⚠️ CRITICAL**: 在本阶段完成之前，不得开始任何用户故事工作。

- [ ] T004 创建 `EncryptedStringTypeHandler` 在 `payflow-common/src/main/java/com/payflow/common/crypto/EncryptedStringTypeHandler.java`——实现 `BaseTypeHandler<String>`，使用 AES-256-GCM 自动加解密
- [ ] T005 [P] 在 `payflow-admin-server/src/main/java/com/payflow/admin/config/SecurityConfig.java` 添加 `SecurityHeadersFilter` Bean——为所有 HTTP 响应添加安全头（`X-Content-Type-Options`、`X-Frame-Options`、`Strict-Transport-Security`）
- [ ] T006 [P] 创建 `TraceFilter` 在 `payflow-common/src/main/java/com/payflow/common/trace/TraceFilter.java`——从请求头或 UUID 生成 Trace ID，设置到 MDC，响应头回传 `X-Trace-Id`
- [ ] T007 将 `MerchantController`（`/api/v1/merchants`）路径改为 `/api/v1/admin/merchants` 并与 `AdminMerchantController` 合并、去重——或直接在 `SecurityConfig.addInterceptors()` 中为 `/api/v1/merchants/**` 注册 `jwtInterceptor`
- [ ] T008 [P] 修复 `RestTemplateConfig` 超时配置：`payflow-cashier-server/src/main/java/com/payflow/cashier/config/RestTemplateConfig.java` 和 `payflow-admin-server/src/main/java/com/payflow/admin/config/RestTemplateConfig.java`——添加 `HttpComponentsClientHttpRequestFactory`，connectTimeout=5s，readTimeout=30s
- [ ] T009 [P] 配置 HikariCP 连接池参数到所有 `application.yml`——`maximum-pool-size=20`、`minimum-idle=5`、`connection-timeout=5000`、`idle-timeout=300000`、`max-lifetime=600000`

**Checkpoint**: 基础设施就绪 — 可以开始并行实现用户故事

---

## Phase 3: User Story 1 - 系统管理员安全管控 (Priority: P1) 🎯 MVP

**Goal**: 修复管理后台认证和授权——MerchantController 鉴权 + JWT 密钥强制 + 角色级权限控制

**Independent Test**: `curl -s -o /dev/null -w "%{http_code}" http://localhost:3003/api/v1/merchants` 返回 401；非 SUPER_ADMIN 创建管理员返回 403

### Tests for User Story 1

- [ ] T010 [P] [US1] 认证拦截器测试 `payflow-admin-server/src/test/java/com/payflow/admin/interceptor/JwtInterceptorTest.java`——验证未认证请求 401、有效 Token 放行、过期 Token 拒绝
- [ ] T011 [P] [US1] 角色授权测试 `payflow-admin-server/src/test/java/com/payflow/admin/interceptor/RoleBasedInterceptorTest.java`——验证 ADMIN 角色创建用户返回 403、SUPER_ADMIN 放行

### Implementation for User Story 1

- [ ] T012 [US1] 修改 `payflow-admin-server/src/main/java/com/payflow/admin/config/SecurityConfig.java`——在 `addInterceptors()` 中为 `/api/v1/merchants/**` 注册 `jwtInterceptor`（或合并 MerchantController 到 admin 路径）
- [ ] T013 [US1] 创建 `@RequireRole` 注解 `payflow-admin-server/src/main/java/com/payflow/admin/security/RequireRole.java`——定义 `value()` 默认为 `SUPER_ADMIN`
- [ ] T014 [US1] 创建 `RoleBasedInterceptor` `payflow-admin-server/src/main/java/com/payflow/admin/interceptor/RoleBasedInterceptor.java`——读取 `request.getAttribute("role")`，校验是否匹配方法注解的角色要求
- [ ] T015 [US1] 在 `SysUserController.create()` 和 `resetPassword()` 方法上添加 `@RequireRole(SUPER_ADMIN)` 注解——`payflow-admin-server/src/main/java/com/payflow/admin/controller/SysUserController.java`
- [ ] T016 [US1] 在 `SystemConfigController` 的写操作方法上添加 `@RequireRole(SUPER_ADMIN)`——`payflow-admin-server/src/main/java/com/payflow/admin/controller/SystemConfigController.java`
- [ ] T017 [US1] 在 `SysRoleController.create()` 和角色权限修改方法上添加 `@RequireRole(SUPER_ADMIN)`——`payflow-admin-server/src/main/java/com/payflow/admin/controller/SysRoleController.java`
- [ ] T018 [US1] 修改 `payflow-admin-server/src/main/java/com/payflow/admin/config/JwtProperties.java`——`secret` 字段移除硬编码默认值，添加 `@NotEmpty`；添加 `@PostConstruct` 校验（生产环境启动失败如果未设置）
- [ ] T019 [US1] 修改 `payflow-admin-server/src/main/resources/application.yml`——移除 `${JWT_SECRET:...}` 中的硬编码默认值
- [ ] T020 [US1] 修改 `payflow-cashier-server/src/main/java/com/payflow/cashier/config/PayflowProperties.java`——JWT secret 字段移除硬编码默认值

**Checkpoint**: 管理后台所有端点需认证，超管权限受保护，JWT 密钥无环境变量时启动失败

---

## Phase 4: User Story 2 - 支付数据完整性保障 (Priority: P1)

**Goal**: 修复 payAmount 始终为 0、退款并发竞态、aggregateRefunds 列名错误

**Independent Test**: 完成支付后查询订单 payAmount 为实际金额；并发 2 笔 60 元退款（原支付 100 元）仅 1 笔成功；Dashboard 聚合刷新不报错

### Tests for User Story 2

- [ ] T021 [P] [US2] 退款并发测试 `payflow-cashier-server/src/test/java/com/payflow/cashier/service/impl/RefundServiceImplTest.java`——验证并发退款超额拒绝
- [ ] T022 [P] [US2] 订单状态并发测试 `payflow-cashier-server/src/test/java/com/payflow/cashier/task/OrderTimeoutTaskTest.java`——验证支付回调优先于过期扫描

### Implementation for User Story 2

- [ ] T023 [US2] 修复 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/PayNotifyService.java` 第 71 行——`updateOrderStatus(orderId, Order.STATUS_PAID, null)` → `updateOrderStatus(orderId, Order.STATUS_PAID, payment.getAmount())`
- [ ] T024 [US2] 在 `payflow-cashier-server/src/main/java/com/payflow/cashier/entity/Refund.java` 添加 `@Version private Integer version` 乐观锁字段
- [ ] T025 [US2] 修复 `payflow-admin-server/src/main/java/com/payflow/admin/mapper/cashier/OrderMapper.java` 第 105 行——`r.refund_channel` → `r.pay_channel`
- [ ] T026 [US2] 修改 `payflow-cashier-server/src/main/java/com/payflow/cashier/task/OrderTimeoutTask.java`——在 UPDATE 操作中添加 `status = 'CREATED' OR status = 'PAYING'` 条件，防止覆盖已支付的订单状态
- [ ] T027 [US2] 在 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/RefundServiceImpl.java` 的 `finalizeRefund()` 方法中增加退款总额二次校验（`validateRefundAmount`）——`sumRefundedAmount()` 需包含当前退款记录

**Checkpoint**: payAmount 正确填充，退款并发安全，对账聚合查询正常

---

## Phase 5: User Story 3 - 支付回调安全验证 (Priority: P1)

**Goal**: 支付宝异步回调实现完整 RSA 公钥验签；微信和银联验签覆盖率确认

**Independent Test**: 向支付宝回调端点发送无效签名获得 "fail" 响应；有效签名正常处理

### Tests for User Story 3

- [ ] T028 [P] [US3] 支付宝回调验签测试 `payflow-cashier-server/src/test/java/com/payflow/cashier/sdk/alipay/AliPayNotifyHelperTest.java`——验证无效签名拒绝、空签名拒绝、有效签名通过

### Implementation for User Story 3

- [ ] T029 [US3] 修复 `payflow-cashier-server/src/main/java/com/payflow/cashier/sdk/alipay/AliPayNotifyHelper.java`——实现完整 RSA-SHA256 签名验证：获取支付宝公钥 → 构建待签名字符串 → `java.security.Signature` 验签
- [ ] T030 [US3] 确认微信支付回调验签完整性——检查 `payflow-payment-wechat` 的 `WxPayNotifyHandler` 或通知解析代码，确保 APIv3 签名验证已正确实现
- [ ] T031 [US3] 确认银联回调验签完整性——检查 `payflow-payment-union` 的通知解析代码，确保 RSA-SHA256 验签已正确实现

**Checkpoint**: 三大渠道回调验签覆盖率 100%

---

## Phase 6: User Story 4 - 服务高可用与容错 (Priority: P1)

**Goal**: Redis fail-close、路由注册表自动刷新、优雅关闭、连接池配置

**Independent Test**: Redis 故障时支付请求返回 503；禁用路由账户后 5 分钟内停止接收流量；SIGTERM 优雅退出

### Tests for User Story 4

- [ ] T032 [P] [US4] Redis fail-close 测试 `payflow-cashier-server/src/test/java/com/payflow/cashier/middleware/MerchantRateLimitInterceptorTest.java`——验证 Redis 异常时拒绝请求（而非放行）
- [ ] T033 [P] [US4] 路由注册表刷新测试 `payflow-cashier-server/src/test/java/com/payflow/cashier/registry/PayChannelAccountRegistryTest.java`——验证 refresh() 更新账户列表

### Implementation for User Story 4

- [ ] T034 [US4] 修改 `payflow-cashier-server/src/main/java/com/payflow/cashier/middleware/MerchantRateLimitInterceptor.java` 第 43 行——`catch (Exception e) { return true; }` → `catch (Exception e) { throw new BizException(5000, "服务暂不可用"); }`
- [ ] T035 [US4] 修改 `payflow-cashier-server/src/main/java/com/payflow/cashier/middleware/PaymentIdempotencyInterceptor.java` 第 44 行——同上 fail-close 改造
- [ ] T036 [US4] 修改 `payflow-cashier-server/src/main/java/com/payflow/cashier/middleware/JwtAuthInterceptor.java` 第 80-84 行——JWT 黑名单 Redis 查询异常时 fail-close
- [ ] T037 [US4] 在 `payflow-cashier-server/src/main/java/com/payflow/cashier/registry/PayChannelAccountRegistry.java` 添加 `@Scheduled(fixedDelay = 300_000)` 方法调用 `refresh()`
- [ ] T038 [US4] 修改 `payflow-cashier-server/src/main/java/com/payflow/cashier/CashierApplication.java`——`taskExecutor` 添加 `setWaitForTasksToCompleteOnShutdown(true)` + `setAwaitTerminationSeconds(30)`
- [ ] T039 [US4] 修复 `payflow-admin-server/src/main/java/com/payflow/admin/interceptor/JwtInterceptor.java` 第 69-72 行——JWT 黑名单 Redis 查询异常时 fail-close（与 cashier JWT 拦截器一致）

**Checkpoint**: 系统在依赖故障时安全降级而非静默放行；路由热更新；优雅关闭

---

## Phase 7: User Story 5 - 商户生命周期管控 (Priority: P2)

**Goal**: 被暂停/关闭的商户立即停止收款能力

**Independent Test**: 将商户设为 SUSPENDED，使用其密钥发起支付请求返回 5001

### Tests for User Story 5

- [ ] T040 [P] [US5] 商户状态检查测试 `payflow-cashier-server/src/test/java/com/payflow/cashier/middleware/MerchantSignatureInterceptorTest.java`——验证非 ACTIVE 商户被拒绝

### Implementation for User Story 5

- [ ] T041 [US5] 修改 `payflow-cashier-server/src/main/java/com/payflow/cashier/middleware/MerchantSignatureInterceptor.java` 的 `getMerchantAppSecret()` 方法——在查询或校验环节添加 `status = 'ACTIVE'` 过滤/校验，非 ACTIVE 抛出 `BizException(5001, "商户已暂停服务")`
- [ ] T042 [US5] 修改 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/RefundServiceImpl.java`——在商户退款入口处校验商户状态（如拦截器未覆盖）

**Checkpoint**: 暂停商户 100% 无法创建新支付或退款

---

## Phase 8: User Story 6 - 密钥与凭证安全管理 (Priority: P2)

**Goal**: 敏感字段数据库加密存储 + API 响应自动脱敏 + 内部 Token 生产强制

**Independent Test**: API 响应不含 `merchantKey`/`appSecret`/`mchKey`/`certPassword`；数据库存储为密文；无 INTERNAL_TOKEN 环境变量时生产启动失败

### Tests for User Story 6

- [ ] T043 [P] [US6] TypeHandler 加密测试 `payflow-common/src/test/java/com/payflow/common/crypto/EncryptedStringTypeHandlerTest.java`——验证加密→存储→读取→解密往返正确
- [ ] T044 [P] [US6] 敏感字段脱敏测试 `payflow-admin-server/src/test/java/com/payflow/admin/security/SensitiveFieldFilterTest.java`——验证序列化排除敏感字段

### Implementation for User Story 6

- [ ] T045 [US6] 在 `payflow-admin-server/src/main/java/com/payflow/admin/entity/ChannelAccount.java` 的 `appSecret`、`mchKey`、`certPassword` 字段添加 `@JsonProperty(access = WRITE_ONLY)` + `@TableField(typeHandler = EncryptedStringTypeHandler.class)`
- [ ] T046 [US6] 在 `payflow-admin-server/src/main/java/com/payflow/admin/entity/Channel.java` 的 `apiKey` 字段添加同上注解
- [ ] T047 [US6] 在 `payflow-admin-server/src/main/java/com/payflow/admin/entity/Merchant.java` 的 `merchantKey` 字段添加 `@JsonProperty(access = WRITE_ONLY)` + `@TableField(typeHandler = EncryptedStringTypeHandler.class)`
- [ ] T048 [US6] 在 `payflow-admin-server/src/main/java/com/payflow/admin/entity/PaymentAccount.java` 的 `appSecret`、`mchKey`、`certPassword` 字段添加同上注解
- [ ] T049 [US6] 修改 `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminChannelAccountController.java` 的 `toSafeMap()` 方法——移除 `appId`、`mchId`、`configJson` 的暴露（与 `PaymentAccountController.toSafeMap()` 保持一致）
- [ ] T050 [US6] 修改三个服务的 `application-prod.yml`——`INTERNAL_TOKEN` 移除硬编码默认值（`${INTERNAL_TOKEN}` 无 `:default`），确保生产部署时必须设置
- [ ] T051 [US6] 移除 `payflow-cashier-server/src/main/resources/application.yml` 第 86-90 行的硬编码商户密钥列表——替换为环境变量引用或外部配置

**Checkpoint**: 敏感字段在 DB 加密存储、API 响应中排除、内部 Token 生产强制

---

## Phase 9: User Story 7 - Webhook 通知投递 (Priority: P2)

**Goal**: 支付成功后向商户 notifyUrl 发送 HTTP 通知，失败自动重试

**Independent Test**: 配置测试 HTTP 端点，支付成功后验证收到 POST 通知

### Tests for User Story 7

- [ ] T052 [P] [US7] Webhook 投递测试 `payflow-cashier-server/src/test/java/com/payflow/cashier/webhook/WebhookDispatchServiceTest.java`——验证 HTTP POST 发送、重试逻辑、MAX 5 次重试后标记 FAILED

### Implementation for User Story 7

- [ ] T053 [US7] 创建 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/WebhookDeliveryService.java`——接口：`deliver()`、`retry()`、`getByOrderId()`
- [ ] T054 [US7] 创建 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/WebhookDeliveryServiceImpl.java`——实现 HTTP POST + HMAC-SHA256 签名 + 重试记录持久化
- [ ] T055 [US7] 创建 `payflow-cashier-server/src/main/java/com/payflow/cashier/mapper/WebhookDeliveryLogMapper.java`——MyBatis-Plus Mapper 接口（如尚不存在）
- [ ] T056 [US7] 创建 `payflow-cashier-server/src/main/java/com/payflow/cashier/mapper/MerchantWebhookEndpointMapper.java`——MyBatis-Plus Mapper 接口（如尚不存在）
- [ ] T057 [US7] 重写 `payflow-cashier-server/src/main/java/com/payflow/cashier/webhook/WebhookDispatchService.java` 的 `publish()` 方法——替换仅打日志的空实现：查询商户 webhook 端点 → @Async 执行 HTTP POST → 记录投递日志
- [ ] T058 [US7] 创建 `payflow-cashier-server/src/main/java/com/payflow/cashier/task/WebhookRetryTask.java`——@Scheduled 每 1 分钟扫描 `webhook_delivery_log` 中 status=PENDING 且 next_retry_at < now 的记录，执行重试
- [ ] T059 [US7] 在 `PayNotifyService.handlePaymentSuccess()` 中补全 webhook payload——包含完整订单信息（orderId、paymentId、amount、currency、status、channelTransactionId、paidAt）

**Checkpoint**: 支付成功后商户收到 Webhook 通知；失败自动重试 5 次

---

## Phase 10: User Story 8 - 防刷与暴力破解防护 (Priority: P2)

**Goal**: 商户登录频率限制 + 滑动窗口限流 + CSV 注入防护 + 安全响应头

**Independent Test**: 连续 10 次错误登录后被锁定；CSV 导出文件以 `=` 开头的值被转义为文本

### Tests for User Story 8

- [ ] T060 [P] [US8] 登录限流测试 `payflow-cashier-server/src/test/java/com/payflow/cashier/service/impl/AuthServiceImplTest.java`——验证 5 次失败后 15 分钟锁定
- [ ] T061 [P] [US8] CSV 注入防护测试 `payflow-admin-server/src/test/java/com/payflow/admin/controller/AdminOrderControllerTest.java`——验证公式字符转义

### Implementation for User Story 8

- [ ] T062 [US8] 在 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/AuthServiceImpl.java` 的 `login()` 方法添加 Redis 登录频率限制——key=`login:attempts:{phone}`，达到 5 次后锁定 15 分钟（key=`login:locked:{phone}`，TTL=15min）
- [ ] T063 [US8] 修改 `payflow-cashier-server/src/main/java/com/payflow/cashier/middleware/MerchantRateLimitInterceptor.java`——改用 Redis ZSET 滑动窗口算法（ZADD + ZREMRANGEBYSCORE + ZCARD）
- [ ] T064 [US8] 修改 `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminOrderController.java` 的 `csvEscape()` 方法——添加公式注入防护：值以 `=`、`+`、`-`、`@` 开头时，添加单引号前缀 `'`
- [ ] T065 [US8] 在所有 Controller 中应用 `csvEscape()` 修复——`AdminExportController.java` 和其他 CSV 导出端点
- [ ] T066 [US8] 验证 `payflow-admin-server/src/main/java/com/payflow/admin/config/SecurityConfig.java` 的 CORS 配置——确保 `allowedOrigins` 白名单无通配符 `"*"`（已确认安全 `allowedOrigins.toArray()` 来自配置）

**Checkpoint**: 登录防暴力破解、速率限制滑动窗口、CSV 安全导出、安全头就位

---

## Phase 11: User Story 9 - 可观测性基础建设 (Priority: P3)

**Goal**: 业务指标暴露、Trace ID 传播、渠道健康检查

**Independent Test**: `/actuator/metrics` 返回支付相关指标；某渠道不可用时健康检查报告 DOWN

### Tests for User Story 9

- [ ] T067 [P] [US9] 健康端点测试 `payflow-admin-server/src/test/java/com/payflow/admin/health/ChannelHealthIndicatorTest.java`

### Implementation for User Story 9

- [ ] T068 [US9] 在 `payflow-cashier-server/src/main/java/com/payflow/cashier/metrics/PaymentMetrics.java` 创建自定义 Micrometer 指标——`payment.success.count`（Counter）、`payment.failure.count`（Counter）、`payment.channel.duration`（Timer），按渠道分标签
- [ ] T069 [US9] 在 `PaymentServiceImpl.dispatchToHandler()` 和 `PayNotifyService` 中埋点——成功/失败计数器、渠道延迟 Timer
- [ ] T070 [US9] 创建 `payflow-common/src/main/java/com/payflow/common/trace/TraceRestTemplateInterceptor.java`——RestTemplate 拦截器，自动在 HTTP 请求头中转发 `X-Trace-Id`
- [ ] T071 [US9] 在 `payflow-admin-server/src/main/java/com/payflow/admin/config/RestTemplateConfig.java` 和 `payflow-cashier-server/src/main/java/com/payflow/cashier/config/RestTemplateConfig.java` 中注册 `TraceRestTemplateInterceptor`
- [ ] T072 [US9] 创建 `payflow-admin-server/src/main/java/com/payflow/admin/health/ChannelHealthIndicator.java`——Spring Boot `HealthIndicator`，检查各支付渠道 API 可达性
- [ ] T073 [US9] 在所有 `application.yml` 中启用 Actuator 端点——`management.endpoints.web.exposure.include=health,info,metrics,prometheus`

**Checkpoint**: 指标端点可用、Trace ID 跨服务传播、渠道健康可检测

---

## Phase 12: User Story 10 - 输入校验与数据规范 (Priority: P3)

**Goal**: Map 请求体替换为 DTO + @Valid 校验 + 软删除 + SQL 注入修复

**Independent Test**: 发送负数金额的支付请求返回校验错误；管理员"删除"商户后记录标记为 DELETED 而非消失

### Tests for User Story 10

- [ ] T074 [P] [US10] DTO 校验测试 `payflow-admin-server/src/test/java/com/payflow/admin/controller/AdminMerchantControllerTest.java`——验证缺失必填字段返回 400 + 具体字段名

### Implementation for User Story 10

- [ ] T075 [US10] 创建 `UpdateMerchantRequest` DTO `payflow-admin-server/src/main/java/com/payflow/admin/dto/UpdateMerchantRequest.java`——带 `@NotBlank`、`@Email`、`@Size` 等校验注解，替代 `AdminMerchantController.updateMerchant()` 中的 `Map<String, Object>` 参数
- [ ] T076 [US10] 创建校验 DTO 替代其他 3 个 `Map<String, Object>` 控制器——`AdminRiskController`、`MerchantPaymentRouteController`、`MerchantPaymentMethodController`
- [ ] T077 [US10] 在核心实体类添加 `@NotNull`/`@NotBlank` 等校验注解——`Order.amount`、`Order.subject`、`Payment.amount`、`Refund.refundAmount`
- [ ] T078 [US10] 修复 `payflow-admin-server/src/main/java/com/payflow/admin/service/AdminRefundService.java` 第 110-118 行 `applyMerchantScope()` 方法——字符串拼接 SQL 改为 MyBatis-Plus 参数化查询 `w.inSql(...)` + 占位符
- [ ] T079 [US10] 修改 `payflow-admin-server/src/main/java/com/payflow/admin/service/impl/MerchantServiceImpl.java` 的 `delete()` 方法——从物理删除 (`deleteById`) 改为软删除（设置 status='DELETED'）
- [ ] T080 [US10] 修复 `payflow-admin-server/src/main/java/com/payflow/admin/service/FeeRateService.java` 第 205 行——将 `.last("LIMIT ...")` 字符串拼接改为 MyBatis-Plus `Page` 对象分页

**Checkpoint**: 所有用户输入有校验、商户软删除、SQL 注入风险点修复

---

## Phase 13: Polish & Cross-Cutting Concerns

**Purpose**: 影响多个用户故事的改进和最终验证

- [ ] T081 [P] 更新 `docs/CONTRACT_MATRIX.md`——记录本次加固中新增/修改的 API 端点契约
- [ ] T082 [P] 运行 quickstart.md 验证——执行所有验证步骤，确认系统行为符合预期
- [ ] T083 宪法合规检查——验证所有强制规则通过：模块边界、支付渠道抽象、API 响应格式、密钥安全、SQL 安全、安全编码、测试覆盖率
- [ ] T084 运行全部测试 `mvn -B test`——确认 39 个现有测试 + 新增测试全部通过，JaCoCo 覆盖率 ≥ 80%
- [ ] T085 前端构建验证 `cd payflow-admin-client && npm run build && cd ../payflow-cashier-client && npm run build`——确认构建成功
- [ ] T086 [P] 移除 `payflow-cashier-server/src/main/resources/application.yml` 中 `spring.sql.init.mode: always`（dev 环境数据损坏风险）
- [ ] T087 [P] 修复 `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/ReceiptServiceImpl.java` 中 `RECEIPT_SEQ` 的 JVM 重启 ID 重复问题——添加基础分布式保障或文档标注限制

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 无依赖 — 可立即开始
- **Foundational (Phase 2)**: 依赖 Setup 完成 — **阻断所有用户故事**
- **User Stories (Phase 3-12)**: 全部依赖 Foundational 完成
  - P1 用户故事（US1-US4）：Foundational 完成后可并行开始
  - P2 用户故事（US5-US8）：可与 P1 并行
  - P3 用户故事（US9-US10）：可与 P1/P2 并行
- **Polish (Phase 13)**: 依赖所有用户故事完成

### User Story Dependencies

- **US1 (安全管控)**: Foundational 完成后可开始 — 无其他故事依赖
- **US2 (数据完整性)**: Foundational 完成后可开始 — 独立于 US1
- **US3 (回调验签)**: Foundational 完成后可开始 — 独立于 US1/US2
- **US4 (高可用)**: Foundational 完成后可开始 — 独立于 US1-US3
- **US5 (商户管控)**: Foundational 完成后可开始 — 依赖 US1 的 JWT 加固（SecurityConfig 变更）
- **US6 (密钥安全)**: Foundational 完成后可开始 — 依赖 Phase 2 的 T004（EncryptedStringTypeHandler）
- **US7 (Webhook)**: Foundational 完成后可开始 — 独立于其他故事
- **US8 (防刷)**: Foundational 完成后可开始 — 独立于其他故事
- **US9 (可观测性)**: Foundational 完成后可开始 — 独立于其他故事
- **US10 (输入校验)**: Foundational 完成后可开始 — 独立于其他故事

### Within Each User Story

- 测试优先（T01x 编号任务先于实现）
- 模型/DTO 优先于 Service
- Service 优先于 Controller
- 核心实现优先于集成

### Module Boundary Order

涉及多个 Maven 模块时，按依赖顺序实施：
```
payflow-common → payflow-payment-channels/* → payflow-cashier-server / payflow-admin-server
```

---

## Implementation Strategy

### MVP First (P1 User Stories)

1. 完成 Phase 1: Setup（DB 迁移 + 依赖 + 基础配置）
2. 完成 Phase 2: Foundational（加密 TypeHandler + 安全头 + Trace + 超时 + 连接池）
3. 完成 Phase 3: US1（安全管控）— MVP 安全门
4. 完成 Phase 4: US2（数据完整性）— MVP 支付核心
5. 完成 Phase 5: US3（回调验签）— MVP 回调安全
6. 完成 Phase 6: US4（高可用）— MVP 运维
7. **STOP and VALIDATE**: 独立测试所有 P1 故事
8. 验收通过后继续 P2

### Incremental Delivery

1. Setup + Foundational → 基础就绪
2. + P1 Stories (US1-US4) → 独立测试 → 安全 + 数据完整性基线
3. + P2 Stories (US5-US8) → 独立测试 → 商户管控 + 密钥 + Webhook + 防刷
4. + P3 Stories (US9-US10) → 独立测试 → 可观测性 + 输入校验
5. + Polish (Phase 13) → 最终验证

### Parallel Team Strategy

多开发者时：
1. 团队共同完成 Setup + Foundational
2. Foundational 完成后并行分配：
   - 开发者 A: US1（安全管控）+ US6（密钥安全）— admin-server 为主
   - 开发者 B: US2（数据完整性）+ US3（回调验签）+ US4（高可用）— cashier-server 为主
   - 开发者 C: US5（商户管控）+ US7（Webhook）+ US8（防刷）— cashier-server 为主
3. 各故事独立完成后集成测试
