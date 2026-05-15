# Research Report: 生产环境加固

**Feature**: 005-production-hardening
**Date**: 2026-05-15

## 1. 认证与授权加固

### Decision: 将 MerchantController 移至 `/api/v1/admin/merchants` 路径或注册拦截器

**Rationale**: 当前 `MerchantController` 映射到 `/api/v1/merchants`（无 `/admin` 段），不在 `JwtInterceptor` 的 `/api/v1/admin/**` 保护范围内。两个选择：A) 将路径改为 `/api/v1/admin/merchants` 并与 `AdminMerchantController` 合并；B) 在 `SecurityConfig.addInterceptors()` 中为 `/api/v1/merchants/**` 单独注册 JWT 拦截器。方案 A 更简单且消除重复。

**Alternatives considered**: 方案 B（单独注册）保护了路径但保留了 /api/v1/merchants 和 /api/v1/admin/merchants 两个独立控制器，增加维护成本。选择方案 A。

### Decision: 使用自定义注解 + 拦截器实现 RBAC

**Rationale**: 项目不使用 Spring Security，而是基于 JWT 拦截器 + Servlet 请求属性。不引入 Spring Security（会大幅增加复杂度）。替代方案：创建 `@RequireRole` 自定义注解 + 新增 `RoleBasedInterceptor`，在 JWT 拦截器的下游执行角色检查。`SUPER_ADMIN` 拥有全部权限，`ADMIN` 角色的写操作受限于非敏感资源。

**Alternatives considered**: 引入 Spring Security + `@PreAuthorize`——功能强大但太重，需要彻底改造 30+ 个 Controller。选择轻量级自定义注解方案，与现有拦截器模式一致。

### Decision: JWT 密钥无环境变量时启动失败

**Rationale**: 在 `JwtProperties` 中移除硬编码默认值，改为 `@NotEmpty` + `@Validated` 强制校验。当 `jwt.secret` 为空或等于不安全默认值时，启动时抛出 `IllegalStateException`。生产 profile 已正确配置无默认值；需要在 `application.yml`（dev）中也移除硬编码值以保持一致性。

**Alternatives considered**: 使用 `@PostConstruct` 验证——更灵活但不够即时。选择 `@ConfigurationProperties` + `@Validated`，因为它在 Bean 创建时失败，阻止服务完全启动。

## 2. 敏感字段过滤

### Decision: Jackson `@JsonIgnore` 注解 + 序列化 Mixin 结合

**Rationale**: 项目当前没有任何 Jackson 敏感字段保护。简单方案：在实体字段（`merchantKey`、`appSecret`、`mchKey`、`certPassword`）上添加 `@JsonIgnore`。但 `merchantKey` 有时需要在后台创建/更新操作中反序列化（仅不序列化输出）。最终方案：使用 Jackson `@JsonProperty(access = WRITE_ONLY)`——允许反序列化（接收输入），禁止序列化（输出）。

**Alternatives considered**: 全局 Mixin 类——对于所有 Controller 的全局过滤不够灵活。`@JsonProperty(access = WRITE_ONLY)` 更精确地表达了语义。

### Decision: 渠道密钥 AES 加密使用自定义 MyBatis TypeHandler

**Rationale**: 项目已有 `AesEncryptor`（AES-256-GCM）但从未使用。需要创建一个 `EncryptedStringTypeHandler` 实现 `BaseTypeHandler<String>`，在 `setNonNullParameter` 中加密、在 `getNullableResult` 中解密。主密钥从 `CryptoProperties.masterKey` 读取。应用到实体字段：`@TableField(typeHandler = EncryptedStringTypeHandler.class)`。

**Alternatives considered**: Service 层手动加解密——容易遗漏（P0-02 的根因）。TypeHandler 在 MyBatis 持久化层自动执行，覆盖所有读/写路径。

## 3. 支付数据完整性

### Decision: 修复 payAmount——从 Payment 获取实际金额

**Rationale**: `PayNotifyService.handlePaymentSuccess()` 第 71 行传递 `null` 给 `payAmount` 参数，导致订单 `payAmount` 始终为 0。实际支付金额已在 `Payment` 实体中。修复：在 `handlePaymentSuccess()` 中，将 `updateOrderStatus(orderId, Order.STATUS_PAID, null)` 改为传递 `payment.getAmount()`。

**Alternatives considered**: 在 `OrderServiceImpl.updateOrderStatus()` 内部查询 Payment 金额——增加不必要的 DB 查询。直接传递已有的 `payment.getAmount()` 最简单。

### Decision: 退款竞态修复——乐观锁 + 数据库唯一约束

**Rationale**: `RefundServiceImpl.sumRefundedAmount()` 进行普通SELECT（无FOR UPDATE），且实体无`@Version`。修复：A) 在 `Refund` 实体添加 `@Version` 乐观锁字段；B) 在 `validateRefundAmount()` 中使用 `SELECT SUM(refund_amount) FROM cashier_refunds WHERE payment_id = ? AND status = 'SUCCESS' FOR UPDATE` 悲观锁。选择 A（乐观锁）因为它不会阻塞并发退款请求——如果版本冲突，第二次写入失败并向调用方返回错误。

**Alternatives considered**: `SELECT ... FOR UPDATE`——更简单但锁定所有对同一 payment 的退款查询。乐观锁提供更好的并发性能，适合退款这种相对低频的操作。

### Decision: 修复 aggregateRefunds 列名——`r.refund_channel` → `r.pay_channel`

**Rationale**: `cashier_refunds` 表中的实际列名是 `pay_channel`（来自 `sql/cashier/schema.sql` 第 148 行），不是 `refund_channel`。这是拼写错误。只需更正 SQL 中的列引用。

## 4. 支付回调验签

### Decision: 支付宝——实现完整 RSA 公钥验签

**Rationale**: `AliPayNotifyHelper.parseNotify()` 仅检查签名字段是否为空（第 47 行），未针对支付宝公钥进行实际签名验证。修复：A) 从 `ChannelConfigHolder` 获取支付宝公钥；B) 使用 `java.security.Signature` 对通知参数进行 RSA-SHA256 验签。验证失败返回 "fail"（支付宝要求的格式），验证通过后才处理支付结果。

**Alternatives considered**: 使用支付宝 SDK 的 `AlipaySignature.rsaCheckV1()`——如果已在 classpath 中，更简单且久经考验。但项目使用自有的 HTTP 客户端调用，引入 SDK 会增加依赖。选择自实现——只需标准的 `java.security` API。

## 5. 服务容错

### Decision: RestTemplate 配置连接超时 5s + 读取超时 30s

**Rationale**: 两个服务的 `RestTemplateConfig` 均未配置超时，使用 JDK `HttpURLConnection`（无限超时）。修复：使用 `HttpComponentsClientHttpRequestFactory`（Apache HttpClient 5，已在 classpath），配置 `setConnectTimeout(5s)` 和 `setReadTimeout(30s)`。

**Alternatives considered**: 切换到 `WebClient`（Spring WebFlux）——更现代但需要重写所有 RestTemplate 调用。`RestTemplate` + `HttpComponentsClientHttpRequestFactory` 对现有代码改变最小。

### Decision: Redis 不可用时安全拦截器 fail-close

**Rationale**: 当前 `MerchantRateLimitInterceptor`、`PaymentIdempotencyInterceptor`、`JwtAuthInterceptor` 在 Redis 异常时 fail-open（静默放行）。对于安全关键路径，应改为 fail-close（拒绝请求 + 明确错误）。实现：将 `catch (Exception e) { return true; }` 改为 `catch (Exception e) { log.error(...); throw new BizException(5000, "服务暂不可用"); }`。

**Alternatives considered**: 保持 fail-open 但增加熔断计数器——增加复杂性但方案相同。Fail-close 是安全系统的标准原则。

### Decision: 支付路由注册表 5 分钟定时刷新

**Rationale**: `PayChannelAccountRegistry` 只在 `@PostConstruct` 时加载，之后永不过期。添加 Spring `@Scheduled(fixedDelay = 300_000)` 方法调用现有的 `refresh()` 方法，每 5 分钟自动重载。需要在某个 `@Configuration` 类上添加 `@EnableScheduling`。

### Decision: Graceful shutdown——Spring Boot 配置 + Bean 清理

**Rationale**: 当前 shutdown hook 仅打印日志。修复：A) 在 `application.yml` 中添加 `server.shutdown=graceful` + `spring.lifecycle.timeout-per-shutdown-phase=30s`；B) 为 `ThreadPoolTaskExecutor` 配置 `setWaitForTasksToCompleteOnShutdown(true)` + `setAwaitTerminationSeconds(30)`。

## 6. 商户管控

### Decision: 在 PaymentSignatureInterceptor 中添加商户状态检查

**Rationale**: 当前 `MerchantSignatureInterceptor.getMerchantAppSecret()` 查询商户但不检查状态。被暂停商户仍可正常收款。修复：在查询中添加 `status = 'ACTIVE'` 条件，或查询后校验状态字段。如果商户非 ACTIVE，抛出 `BizException(5001, "商户已暂停")`。

**Alternatives considered**: 在 `PaymentServiceImpl.createPayment()` 中检查——有效但拦截器层面更早拒绝更高效。

## 7. Webhook 通知

### Decision: 实现全功能 Webhook——HTTP POST + 指数退避重试 + 持久化

**Rationale**: 当前 `WebhookDispatchService.publish()` 仅打印日志。DDL 中已有 `merchant_webhook_endpoint` 和 `webhook_delivery_log` 表。实现方案：
1. `@Async` 执行 HTTP POST 到商户 `notifyUrl`，携带 JSON payload 和 HMAC-SHA256 签名头
2. 首次失败后按 1min/5min/15min/30min/1h 间隔重试（最多 5 次）
3. 每次投递结果记录到 `webhook_delivery_log`
4. 所有重试失败后标记为 FAILED，支持管理后台手动补发

**Alternatives considered**: RocketMQ 延迟消息重试——更好但默认关闭。Spring Retry + 数据库持久化作为通用实现，不依赖 MQ。

## 8. 防刷与安全防护

### Decision: 登录频率限制——Redis 计数器 + 锁定机制

**Rationale**: 当前 merchant 登录无任何频率限制。实现：A) `login:attempts:{phone}` key，5 次失败后设置 `login:locked:{phone}` key（TTL 15 分钟）；B) 管理员登录在验证码之上增加 IP 速率限制。

### Decision: 速率限制改用滑动窗口

**Rationale**: 当前固定窗口在分钟边界有突发问题（1200 请求/2 秒）。改用 Redis ZSET 滑动窗口：`ZADD + ZREMRANGEBYSCORE + ZCARD`。更精确但 Redis 操作成本略高。

### Decision: CSV 公式注入防护

**Rationale**: 在 `csvEscape()` 中，如果值以 `=`、`+`、`-`、`@` 开头，添加单引号前缀（Excel/Google Sheets 强制文本模式）。

### Decision: 安全响应头——Filter 统一添加

**Rationale**: 创建 `SecurityHeadersFilter`，在响应中添加：`X-Content-Type-Options: nosniff`、`X-Frame-Options: DENY`、`Strict-Transport-Security: max-age=31536000; includeSubDomains`（仅 HTTPS）。

## 9. 可观测性

### Decision: 集成 Micrometer + Prometheus 端点

**Rationale**: 添加 `micrometer-registry-prometheus` 依赖到父 POM。在 `application.yml` 中添加 `management.endpoints.web.exposure.include=health,info,metrics,prometheus`。自定义指标：支付成功/失败计数器（`payment.success.count`、`payment.failure.count`）、渠道延迟 Timer（`payment.channel.duration`）。

### Decision: Trace ID 通过 Filter + MDC 传播

**Rationale**: 创建 `TraceFilter`，检查请求头 `X-Trace-Id`，不存在则生成 UUID。将 ID 放入 MDC，通过 `RestTemplate` 拦截器转发到下游服务。所有 SLF4J 日志自动包含 Trace ID。

## 10. 数据规范

### Decision: 参数化 `applyMerchantScope` SQL

**Rationale**: 当前用字符串拼接构建 `IN (...)` 子查询（SQL 注入风险）。改用 MyBatis-Plus `w.inSql("order_id", "SELECT order_id FROM cashier_orders WHERE merchant_id IN (" + placeholders + ")")` 配合参数化占位符。

### Decision: DTO + `@Valid` 替代 `Map<String, Object>` 请求体

**Rationale**: 为接受 `Map` 的 4 个控制器创建具体 DTO（如 `AdminMerchantController` 的 `UpdateMerchantRequest`），添加 Bean Validation 注解。Controller 方法参数添加 `@Valid`。

### Decision: 商户软删除

**Rationale**: 将 `MerchantServiceImpl.delete()` 从 `deleteById(id)`（物理删除）改为 `update(Wrappers.lambdaUpdate(Merchant.class).set(Merchant::getStatus, "DELETED").eq(Merchant::getId, id))`。或者添加 `@TableLogic` + `deleted` 字段。考虑到已有 `status` 字段定义 ACTIVE/SUSPENDED/CLOSED，选择利用现有 `status` 字段，添加 DELETED 状态值并更新 `delete()` 方法。
