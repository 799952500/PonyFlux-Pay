# PonyFlux-Pay 全系统优化报告

> 审查时间：2026-05-05 12:30  
> 审查范围：6模块（common/payment-core/payment-wechat/payment-alipay/cashier-server/admin-server）  
> 审查维度：后端架构 / 安全 / 数据库 / 编码规范

---

## 一、P0 级（必须修复，影响安全或数据正确性）

### P0-01 🔴 CORS 配置 `allowedOrigins("*")` + `allowCredentials` 风险
- **文件**：`admin-server/.../config/SecurityConfig.java`
- **问题**：`allowedOrigins("*")` 允许任意域名跨域访问管理后台 API，若前端携带 Cookie/JWT，可被 CSRF 攻击窃取管理员 Token
- **修复**：改为显式白名单（开发环境 localhost:3001，生产环境配域名）

### P0-02 🔴 敏感密钥明文存储在数据库
- **文件**：`payment_methods` 表的 `mch_key`/`app_secret`/`cert_password`，`payment_accounts` 同理
- **问题**：微信/支付宝商户密钥明文存 MySQL，一旦数据库泄露即全盘失控
- **修复**：入库前 AES-256 加密，读取时解密；或使用 Vault/KMS

### P0-03 🔴 Admin 异常处理器泄露内部错误信息
- **文件**：`admin-server/.../exception/GlobalExceptionHandler.java`
- **问题**：`handleRuntime` 返回 `ex.getMessage()` 给前端，可能泄露 SQL、堆栈、内部路径
- **修复**：生产环境返回通用消息，详细错误只记日志

### P0-04 🔴 RefundServiceImpl 渠道退款逻辑硬编码依赖
- **文件**：`cashier-server/.../service/impl/RefundServiceImpl.java`
- **问题**：直接注入 `WxPayNativeHandler` + `AliPayQrHandler`，未使用策略模式，新增渠道需改 Service 代码
- **修复**：统一使用 `PayChannelPaymentOpenServiceLocator` 定位退款处理器，与支付下单一致

---

## 二、P1 级（强烈建议，影响可维护性/健壮性）

### P1-01 🟠 JwtUtils 重复实现
- **文件**：cashier-server 和 admin-server 各有一个 `JwtUtils`
- **问题**：逻辑类似但实现不同（一个静态工具类，一个 Spring Bean），维护成本翻倍
- **修复**：抽到 `payflow-common` 模块，统一为可注入的 Bean

### P1-02 🟠 GlobalExceptionHandler 不统一
- **问题**：cashier 用 `R<T>` 包装（code/message/data），admin 用内部类 `ApiResponse`（code/message/data），两套响应结构
- **修复**：统一到 `payflow-common` 的 `R<T>`，admin 删除内部 `ApiResponse`

### P1-03 🟠 风控金额比较用 BigDecimal 存在精度风险
- **文件**：`RiskCheckServiceImpl.java`
- **问题**：数据库存分为 BIGINT，风控阈值 threshold 存为 DECIMAL（元），比较时做了 `centsToYuan` 转换，浮点精度可能出错
- **修复**：风控阈值统一存为分（BIGINT），比较直接用 Long，避免任何 BigDecimal/Double 转换

### P1-04 🟠 数据库 Schema 不一致
- **问题**：Schema SQL 中表名 `admin_channels` / `admin_merchants` 等，但实际数据库表名是 `channels` / `merchants`（无 admin_ 前缀）。Entity @TableName 也指向无前缀名
- **修复**：统一 Schema SQL 与实际表名，添加注释标注

### P1-05 🟠 merchant_payment_methods.merchant_id 类型不一致
- **Schema**：定义为 `BIGINT`，实际数据用 `VARCHAR(64)`（商户号如 M100001）
- **修复**：Schema SQL 中改为 `VARCHAR(64)` 与实体对齐

### P1-06 🟠 订单表缺少 merchant_order_no 索引
- **问题**：商户按自己的订单号查询时（防重+查询），走全表扫描
- **修复**：添加 `idx_merchant_order_no (merchant_id, merchant_order_no)` 联合索引

### P1-07 🟠 PaymentServiceImpl.updateOrderStatus 先更新再查缓存回填
- **问题**：`updateOrderStatus` 中先 updateById 再 evict+重查回填，存在短暂不一致窗口
- **修复**：改为先删缓存再更新DB（Cache-Aside 标准模式），或用 `@CacheEvict` 注解

### P1-08 🟠 时间戳校验用服务端时间
- **文件**：`MerchantSignatureInterceptor.java`
- **问题**：`System.currentTimeMillis() / 1000` 取服务端时间，分布式部署时各节点时钟偏差可能误拒
- **修复**：可接受，但建议加监控告警；或在 Redis 中维护统一时钟

---

## 三、P2 级（建议优化，提升代码质量）

### P2-01 🟡 OrderServiceImpl 构造器注入7个依赖
- **问题**：7个依赖注入，类职责过重（订单CRUD + 缓存 + MQ + 风控 + 远程配置）
- **修复**：拆分为 OrderCommandService（写操作）和 OrderQueryService（读操作）

### P2-02 🟡 RefundServiceImpl.refund() 与 executeApprovedRefund() 退款渠道调用代码重复
- **修复**：提取 `doChannelRefund(Refund, Payment, PayChannelAccount)` 私有方法

### P2-03 🟡 R 类不实现 Serializable
- **问题**：`serialVersionUID` 声明了但未实现 `Serializable` 接口
- **修复**：要么去掉 `serialVersionUID`，要么实现 `Serializable`

### P2-04 🟡 DashboardAggregationService 文件路径错误
- **问题**：Service 接口在 `service/` 目录但 impl 文件不在 `service/impl/` 目录
- **修复**：移到正确目录结构

### P2-05 🟡 缺少请求参数校验注解
- **问题**：CreateOrderRequest 等DTO缺少 `@NotBlank`/`@NotNull`/`@Min` 等 Bean Validation 注解
- **修复**：补全 JSR-380 校验注解，Controller 加 `@Valid`

### P2-06 🟡 魔法值散落
- **问题**：如 `30` 分钟超时、`5` 分钟时间戳容差等硬编码数字
- **修复**：提取为配置项或常量

### P2-07 🟡 日志级别不统一
- **问题**：部分用 `log.warn` 记录正常业务流程（如缓存失败），部分用 `log.error` 记录可预期的异常
- **修复**：正常业务降级用 `log.warn`，真正的系统异常用 `log.error`

---

## 四、数据库优化

### DB-01 缺失索引
```sql
-- 订单表：商户+商户订单号联合查询（防重+查询）
ALTER TABLE cashier_orders ADD INDEX idx_merchant_merchant_order (merchant_id, merchant_order_no);

-- 支付记录表：按渠道+状态查询
ALTER TABLE cashier_payments ADD INDEX idx_pay_channel_status (pay_channel, status);

-- 退款表：按商户+状态查询
ALTER TABLE cashier_refunds ADD INDEX idx_order_status (order_id, status);

-- 审计日志：按用户+时间查询
ALTER TABLE admin_audit_logs ADD INDEX idx_username_created (username, created_at);
```

### DB-02 字段类型优化
```sql
-- 风控阈值：DECIMAL → BIGINT（统一用分）
ALTER TABLE risk_rules MODIFY COLUMN threshold BIGINT NOT NULL COMMENT '阈值(分)';

-- 商户签名密钥长度不足
ALTER TABLE cashier_merchants MODIFY COLUMN app_secret VARCHAR(512) DEFAULT NULL COMMENT '商户签名密钥';
```

### DB-03 NOT NULL 约束加强
- `cashier_orders.amount` 应为 NOT NULL
- `cashier_payments.payment_id` 已有 UNIQUE 但 `order_id`/`pay_channel` 允许 NULL
- `cashier_channels.channel_code` 应为 NOT NULL

---

## 五、实施优先级与分工

| 优先级 | 编号 | 任务 | 执行 Agent | 预计工时 |
|--------|------|------|-----------|---------|
| P0 | 01 | CORS 白名单 | 🔒 安全工程师 | 15min |
| P0 | 02 | 密钥加密存储 | 🔒 安全工程师 | 60min |
| P0 | 03 | 异常信息脱敏 | 🔒 安全工程师 | 15min |
| P0 | 04 | 退款策略模式重构 | 🏗️ 后端架构师 | 45min |
| P1 | 01 | JwtUtils 统一 | 🏗️ 后端架构师 | 30min |
| P1 | 02 | 响应结构统一 | 🏗️ 后端架构师 | 30min |
| P1 | 03 | 风控金额比较统一 | 🏗️ 后端架构师 | 20min |
| P1 | 05 | merchant_id 类型修复 | 🗄️ 数据库优化师 | 10min |
| P1 | 06 | 添加缺失索引 | 🗄️ 数据库优化师 | 10min |
| P2 | 05 | DTO 校验注解 | 👁️ 代码审查师 | 20min |
| P2 | 02 | 退款代码去重 | 👁️ 代码审查师 | 15min |

---

## 六、结论

系统整体架构合理，策略模式路由设计清晰。主要问题集中在：
1. **安全**：CORS 全开放 + 密钥明文 + 异常信息泄露（3个P0）
2. **一致性**：两套 JwtUtils / 两套响应结构 / Schema 与实际不符
3. **数据库**：缺少关键索引、金额类型不统一

建议按 P0 → P1 → P2 顺序修复。
