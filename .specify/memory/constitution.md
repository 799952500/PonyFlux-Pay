# PonyFlux-Pay 项目宪法

<!--
  Sync Impact Report
  ==================
  Version change: 1.1.0 → 2.0.0 (MAJOR — 新增 8 个章节，从 5 个核心原则扩展为 13 个章节的完整编码规范体系)

  Added sections:
    - 编码规范（命名/格式/注释/类成员顺序/POJO）
    - 集合与并发处理
    - 数据库访问规范
    - 安全编码规范
    - 异常与日志规范
    - 测试规范（含 Definition of Done）
    - 自动化执行
    - 前端规范

  Modified principles:
    - 原则 I-V 全部增强：增加正例/反例代码片段、违规案例和修复方案
    - 开发工作流：增加测试门禁步骤
    - 治理：增加 Code Review Checklist 和自动化映射表

  Templates requiring updates:
    - .specify/templates/spec-template.md ⚠ 需更新（增加 Constitution Compliance 段落）
    - .specify/templates/plan-template.md ⚠ 需更新（Constitution Check 具象化）
    - .specify/templates/tasks-template.md ⚠ 需更新（模块边界分组）
    - .specify/templates/checklist-template.md ⚠ 需更新（检查类别对齐）
    - .specify/templates/constitution-template.md ⚠ 需更新（章节结构同步）
-->

## 核心原则

### I. 模块边界纪律 [强制]

所有代码必须放在正确的 Maven 模块中。项目采用 7 个顶层模块 + 1 个渠道聚合器（含 3 个子模块）的层级结构：

| 模块 | 职责范围 | 禁止行为 |
|------|----------|----------|
| `payflow-common` | 仅限共享工具、异常（`BizException`）、加密（`AesEncryptor`）、常量 | 禁止放 Spring Bean、业务逻辑、实体类 |
| `payflow-payment-core` | 支付 SPI：`PayStrategy` 接口、`PayMethod` 枚举、DTO。零 Spring 依赖 | 禁止加入渠道特有逻辑 |
| `payflow-payment-channels/` | 聚合器 POM（packaging=pom），管理所有渠道子模块 | 禁止直接包含 Java 源码 |
| `payflow-payment-channels/payflow-payment-wechat` | 微信支付 API 处理器 | 禁止直接引用 cashier/admin 实体类 |
| `payflow-payment-channels/payflow-payment-alipay` | 支付宝 API 处理器 | 同上 |
| `payflow-payment-channels/payflow-payment-union` | 银联/云闪付 API 处理器 | 同上 |
| `payflow-cashier-server` | 商户端支付处理、订单管理 | 禁止放管理后台逻辑 |
| `payflow-admin-server` | 后台运营管理、商户配置、对账 UI | 禁止放支付处理逻辑 |
| `payflow-recon-server` | 仅对账引擎（账单下载/解析/比对） | 禁止放管理后台 UI 逻辑 |
| `payflow-sdk-java` | 轻量 HMAC-SHA256 签名工具，零依赖 | 禁止引入 Spring 或数据库依赖 |

**正例**：
```java
// ✅ payflow-cashier-server 中的支付服务只调用 PayStrategy 接口
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PayStrategyLocator payStrategyLocator; // 通过 Locator 解析
    // 不直接注入 WxPayNativeHandler
}
```

**反例**：
```java
// ❌ 在 cashier 中直接注入渠道 Handler（违反模块边界）
@Autowired
private WxPayNativeHandler wxPayNativeHandler; // P0-04 根因
```

**理由**：模块边界防止循环依赖，确保各模块可独立测试和部署。违反此原则曾导致 P0-04（RefundServiceImpl 硬编码渠道依赖）。

---

### II. 支付渠道抽象 [强制]

所有支付渠道交互必须通过策略模式：

1. `PayStrategyRegistry`（由 Spring 注入的 `List<PayStrategy>` 构建）按 `PayMethod` 映射策略。
2. `PayChannelPaymentOpenServiceLocator` 按渠道代码定位服务。
3. 各策略将调用委托给 `payflow-payment-*` 模块的 Handler。

**规则**：
- 禁止在 Service 中直接注入具体的渠道 Handler，必须使用 Locator/Registry。
- 新增支付渠道必须实现 `PayStrategy`，并注册为 Spring Bean，命名为 `{payMethodCode小写}PayStrategy`。
- 渠道配置必须通过 `ChannelConfigHolder.getChannelConfig()`（JSON 字符串）传递，支付模块禁止直接引用实体类。
- 回调通知处理采用分离模式：`{channel}PaymentOpenService` 负责支付/退款操作，`{channel}OpenService` 负责异步通知解析。

**正例**：
```java
// ✅ 通过 Locator 获取渠道服务
PayStrategy strategy = payStrategyLocator.locate(payMethod);
PayResult result = strategy.execute(payContext);
```

**反例**：
```java
// ❌ 硬编码渠道依赖
if ("wechat".equals(channel)) {
    wxPayNativeHandler.handle(data); // 直接注入 Handler
}
```

**理由**：直接注入渠道 Handler（P0-04）造成硬编码依赖，新增或移除支付渠道需要修改业务代码。

---

### III. 数据库分区 [强制]

系统使用两个 MySQL 数据库，表归属严格分离：

| 数据库 | 用途 | 表前缀 | 示例表 |
|--------|------|--------|--------|
| `payflow_admin` | 运营配置 | `admin_` | `admin_merchants`、`admin_channels`、`admin_users`、`admin_roles` |
| `payflow_admin` | 对账数据 | `recon_` | `recon_task`、`recon_bill_record`、`recon_diff` |
| `payflow_cashier` | 交易数据 | `cashier_` | `cashier_orders`、`cashier_payments`、`cashier_refunds` |

**规则**：
- admin-server 使用双数据源。主数据源（admin）必须使用手动 `MapperFactoryBean` 注册——禁止在主数据源上使用 `@MapperScan`。
- admin-server 中的 cashier mapper 必须放在 `mapper.cashier` 子包，并使用限定包的 `@MapperScan`。
- 新建实体必须遵循对应数据库的前缀约定。
- 禁止在应用层做跨库 JOIN 或关联查询，使用分开的查询。

**正例**：
```java
// ✅ 双数据源正确配置：admin 主数据源手动注册 Mapper
@Bean
public MapperFactoryBean<MerchantMapper> merchantMapper(SqlSessionFactory sqlSessionFactory) {
    MapperFactoryBean<MerchantMapper> factory = new MapperFactoryBean<>(MerchantMapper.class);
    factory.setSqlSessionFactory(sqlSessionFactory);
    return factory;
}
```

**反例**：
```java
// ❌ 在主数据源上使用 @MapperScan（导致 MyBatis-Plus 自动配置冲突）
@MapperScan("com.ponyflux.admin.mapper") // 会与 cashier 数据源冲突
@Configuration
public class DataSourceConfig { }
```

---

### IV. API 响应规范 [强制]

所有后端控制器必须返回统一格式：

```json
{ "code": 0, "message": "success", "data": { ... } }
```

**规则**：
- `code = 0` 表示成功。非零为错误码（对账模块：7500-7599）。
- 使用 `Map<String, Object>` 或 `payflow-common` 中的 `R<T>` 包装类。
- 禁止在各模块中自创响应包装类。
- 前端 Axios 拦截器会自动解包 `data`——修改响应结构将破坏所有前端代码。
- 全局异常处理器必须返回此格式。禁止将堆栈信息或内部错误信息泄露到前端。

**正例**：
```java
// ✅ 统一响应格式
@GetMapping("/order/{id}")
public ResponseEntity<Map<String, Object>> getOrder(@PathVariable Long id) {
    OrderDTO order = orderService.getById(id);
    return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", order));
}
```

**反例**：
```java
// ❌ 自创响应格式（P1-02：系统中已存在两套响应包装）
@GetMapping("/order/{id}")
public ApiResponse<OrderDTO> getOrder(@PathVariable Long id) { // 自创包装类
    return ApiResponse.success(orderService.getById(id));
}
```

---

### V. 密钥与配置安全 [强制]

**规则**：
- 商户 API 密钥（`mch_key`、`app_secret`、`cert_password`）入库前必须使用 `AesEncryptor`（AES-256-GCM）加密存储。
- 主密钥必须通过环境变量或外部配置（`payflow.crypto.master-key`）注入，禁止硬编码。
- JWT 密钥必须从配置文件（application-remote.yml）读取，禁止使用硬编码默认值。
- CORS `allowedOrigins` 必须使用显式白名单，禁止 `"*"` 与 `allowCredentials(true)` 同时使用。
- 内部服务令牌（`X-Payflow-Internal-Token`）在生产环境必须可轮换。

**正例**：
```java
// ✅ 主密钥从环境变量注入
@Value("${payflow.crypto.master-key}")
private String masterKey; // 来自启动参数或 k8s secret
```

**反例**：
```java
// ❌ 硬编码主密钥（P0-02 数据库密钥明文）
private static final String MASTER_KEY = "my-secret-key-12345"; // 硬编码
```

---

## 编码规范

### 命名规范 [强制]

| 类型 | 规范 | 示例 |
|------|------|------|
| 类名 | UpperCamelCase | `PaymentOrderService` |
| 方法/变量 | lowerCamelCase | `getOrderByNo()` |
| 常量 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| 包名 | 全小写单数 | `com.ponyflux.payment` |
| 枚举类名 | Enum 后缀 | `OrderStatusEnum` |
| 枚举成员 | 全大写下划线 | `SUCCESS`、`WAIT_PAY` |
| 抽象类 | Abstract/Base 前缀 | `BaseEntity`、`AbstractPayHandler` |
| 异常类 | Exception 结尾 | `PaymentException`、`BizException` |
| POJO | 后缀规约 | `xxxDO`（数据对象）、`xxxDTO`（传输对象）、`xxxVO`（展示对象） |

**布尔变量命名** [强制]：
```java
// ✅ 正确：禁止 is 前缀（POJO 除外，但推荐统一不加）
private boolean deleted;
private boolean active;

// ❌ 错误：is 前缀导致部分框架序列化问题
private boolean isDeleted;
```

### 代码格式 [强制]

- **缩进**：4 个空格（禁止 Tab）
- **大括号**：K&R 风格（埃及括号），即使单行语句也必须使用 `{}`
- **单行字符数**：≤ 120 字符
- **导入声明**：禁止通配符导入 `import java.util.*`，必须显式导入
- **文件编码**：UTF-8，LF 换行

**正例**：
```java
// ✅ K&R 大括号 + 显式导入
import java.util.List;
import java.util.Map;
public class OrderService {
    public void process(OrderDTO dto) {
        if (dto.getAmount() == null) {
            throw new BizException("金额不能为空");
        }
    }
}
```

**反例**：
```java
// ❌ 通配符导入 + 缺少大括号
import java.util.*;
if (dto.getAmount() == null) throw new BizException("金额不能为空");
```

### 注释规范 [强制]

- **类 Javadoc**：所有公共类必须有，说明功能、作者
- **公共方法 Javadoc**：必须有，说明参数、返回值、异常
- **TODO 格式**：`// TODO(作者): 待办事项 (JIRA-编号)`
- **注释语言**：使用中文（代码标识符使用英文）

**正例**：
```java
/**
 * 支付订单服务，负责订单创建、查询和状态流转。
 */
public class PaymentOrderService {
    /**
     * 根据订单号查询支付订单。
     *
     * @param orderNo 订单编号
     * @return 订单详情
     * @throws BizException 订单不存在时抛出
     */
    public OrderDTO getByOrderNo(String orderNo) { ... }
}
```

### 类成员顺序 [强制]

```
常量 → 静态变量 → 静态代码块 → 实例变量 → 构造器 → 公共方法 → 私有方法
```

```java
// ✅ 正确的成员顺序
public class PaymentHandler {
    // 1. 常量
    private static final int MAX_RETRY = 3;

    // 2. 静态变量
    private static Logger log = LoggerFactory.getLogger(PaymentHandler.class);

    // 3. 实例变量
    private final PayChannelService payChannelService;

    // 4. 构造器（@RequiredArgsConstructor 推荐）
    public PaymentHandler(PayChannelService payChannelService) {
        this.payChannelService = payChannelService;
    }

    // 5. 公共方法
    public PayResult handle(PayContext ctx) { ... }

    // 6. 私有方法
    private void validateParams(PayContext ctx) { ... }
}
```

### POJO 规范 [强制]

- **POJO 属性必须使用包装类型**（`Integer` 而非 `int`），以区分"未传"和"0"
- **禁止给 POJO 属性设默认值**
- **Long 型 ID 必须使用 `@JsonSerialize(using = ToStringSerializer.class)`**，防止前端精度丢失
- **构造器注入**：使用 `@RequiredArgsConstructor` + `final` 字段，禁止 `@Autowired` 字段注入

**正例**：
```java
@Data
@RequiredArgsConstructor
public class PaymentOrderDTO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String orderNo;
    private Long amount;        // 包装类型 Long
    private Integer status;     // 包装类型 Integer，无默认值
}
```

**反例**：
```java
// ❌ 字段注入 + 基本类型 + 默认值
public class PaymentOrderDTO {
    @Autowired
    private PayService service; // 字段注入禁止
    private long amount;        // 基本类型禁止
    private int status = 0;     // 默认值禁止
}
```

---

## 集合与并发处理

### 集合操作 [强制]

- 集合初始化时必须指定初始容量：`new ArrayList<>(128)`、`new HashMap<>(32)`
- 判空使用 `isEmpty()` 而非 `size() == 0`
- **禁止**在 `foreach` 循环中修改集合（add/remove），改用 `Iterator` 或 `removeIf()`
- `Arrays.asList()` 返回的集合不可增删，需包装：`new ArrayList<>(Arrays.asList("a", "b"))`
- IN 查询元素数不超过 1000

**正例**：
```java
// ✅ 正确初始化容量 + Iterator 删除
List<String> list = new ArrayList<>(128);
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    if (shouldRemove(it.next())) it.remove();
}
```

**反例**：
```java
// ❌ 未指定容量 + foreach 中修改
List<String> list = new ArrayList<>();
for (String s : list) {
    if (shouldRemove(s)) list.remove(s); // ConcurrentModificationException!
}
```

### 并发处理 [强制]

- **严禁使用 `Executors` 创建线程池**，必须通过 `ThreadPoolExecutor` 显式指定参数
- `ThreadLocal` 变量必须在 `finally` 中 `remove()`，防止内存泄漏
- 优先使用 `java.util.concurrent` 包（`ConcurrentHashMap`、`CopyOnWriteArrayList`）而非自建锁
- 高并发支付场景使用 Redis 分布式锁或数据库乐观锁

**正例**：
```java
// ✅ 显式指定线程池参数
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    10, 50, 60L, TimeUnit.SECONDS,
    new LinkedBlockingQueue<>(1000),
    new ThreadFactoryBuilder().setNameFormat("payment-%d").build(),
    new ThreadPoolExecutor.CallerRunsPolicy()
);
```

**反例**：
```java
// ❌ Executors 创建线程池（无界队列风险）
ExecutorService pool = Executors.newFixedThreadPool(10);
Executors.newCachedThreadPool(); // 无限创建线程
```

**ThreadLocal 清理** [强制]：
```java
// ✅ 正确：finally 中清理
ThreadLocal<PayContext> ctxHolder = new ThreadLocal<>();
try {
    ctxHolder.set(ctx);
    process();
} finally {
    ctxHolder.remove(); // 必须清理
}

// ❌ 错误：ThreadLocal 未清理，在线程池场景导致内存泄漏
ctxHolder.set(ctx);
process();
// 未 remove
```

---

## 数据库访问规范

### 实体类规范 [强制]

MyBatis-Plus 实体类必须包含以下注解和配置：

```java
// ✅ 正确的实体类配置
@Data
@TableName("cashier_orders")
public class PaymentOrder extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    private String orderNo;
    private Long amount;        // 金额用 Long（分）
    private Integer status;     // 配合 IEnum 枚举
}
```

### 查询规范 [强制]

- **禁止 `SELECT *`**，只查必要字段
- 分页查询必须设置 `maxLimit`（≤ 500）：`pagination.setMaxLimit(500L)`
- 使用 Lambda 方式引用列名：`LambdaQueryWrapper<PaymentOrder> qw = new LambdaQueryWrapper<>(); qw.eq(PaymentOrder::getOrderNo, orderNo);`
- 手写 SQL 需手动加 `deleted=0` 条件

**正例**：
```java
// ✅ Lambda 引用列名 + 分页限制
paymentOrderMapper.selectPage(
    new Page<>(page, size),
    new LambdaQueryWrapper<PaymentOrder>()
        .eq(PaymentOrder::getStatus, OrderStatusEnum.SUCCESS)
        .ge(PaymentOrder::getCreateTime, startTime)
);
```

**反例**：
```java
// ❌ SELECT * + 字符串硬编码列名
baseMapper.selectList(new QueryWrapper<PaymentOrder>()
    .select("*")
    .eq("status", 1)); // 字符串硬编码列名
```

### SQL 安全 [强制]

- **禁止 XML 中使用 `${}`**（SQL 注入风险），仅 `ORDER BY` 动态排序且白名单校验后例外
- 必须配置 `BlockAttackInnerInterceptor` 防全表更新/删除
- 批量操作单次 ≤ 500 条
- 多表 JOIN ≤ 3 张表，超过则分两次查询在 Service 层组装
- Like 查询只用右模糊：`likeRight("name", "张")`，禁止左模糊导致索引失效

**正例**：
```java
// ✅ MyBatisPlusConfig 插件配置
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
    pagination.setMaxLimit(500L);
    interceptor.addInnerInterceptor(pagination);
    interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
    interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor()); // 防全表更新
    return interceptor;
}
```

**反例**：
```xml
<!-- ❌ XML 中使用 ${} （SQL 注入）-->
<select id="getByColumn">
    SELECT * FROM cashier_orders ORDER BY ${orderBy}
</select>
```

---

## 安全编码规范

### 数据加密 [强制]

- 敏感字段（商户密钥、手机号、身份证号、银行卡号）使用 AES-256-GCM 加密存储
- 主密钥通过环境变量 `payflow.crypto.master-key` 注入，禁止硬编码
- 密码字段禁止以 `password/passwd/pwd` 命名，统一使用 `credentialToken`

```java
// ✅ 使用 AesEncryptor 加密商户密钥
String encrypted = aesEncryptor.encrypt(mchKey);
merchant.setMchKey(encrypted);
merchantMapper.insert(merchant);
```

### 日志脱敏 [强制]

禁止在日志中打印以下敏感信息：
- 手机号：显示 `139****1219`
- 身份证号：显示 `330***********1234`
- 银行卡号：显示 `6222****1234`
- 密码/密钥：完全屏蔽

```java
// ✅ 脱敏工具方法
public static String maskPhone(String phone) {
    return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
}
log.info("用户{}支付成功，金额{}分", maskPhone(phone), amount);
```

### 防重放与幂等性 [强制]

- 商户 API 请求使用 HMAC-SHA256 签名 + 时间戳（允许 ±5 分钟偏差）+ Nonce 防重放
- 支付接口使用 Redis 幂等锁：`redisTemplate.opsForValue().setIfAbsent("payment_lock:" + orderNo, "1", 10, TimeUnit.MINUTES)`
- 数据库订单号建唯一索引作为最后防线

```java
// ✅ 时间戳 + Nonce 防重放
if (Math.abs(System.currentTimeMillis() - timestamp) > 300_000) {
    throw new BizException("请求已过期");
}
if (redisTemplate.opsForValue().setIfAbsent("nonce:" + nonce, "1", 6, TimeUnit.MINUTES)) {
    proceed();
} else {
    throw new BizException("重复请求");
}
```

### 参数校验 [强制]

- 所有 Controller 入参 DTO 必须使用 Bean Validation：`@NotBlank`、`@Size`、`@Email`、`@Positive` 等
- `@ConfigurationProperties` 类必须加 `@Validated` 确保启动时配置校验
- Page size 必须校验上限（`@Max(100)`）
- orderBy 参数必须白名单校验

---

## 异常与日志规范

### 异常处理 [强制]

- 使用 `@RestControllerAdvice` 全局异常处理器统一处理，禁止各 Controller 自行 try-catch
- 业务异常统一抛出 `BizException`（来自 `payflow-common`）
- 错误码遵守分配范围，禁止复用：

| 范围 | 模块 |
|------|------|
| 0 | 成功 |
| 1xxx | 管理后台 / 认证 |
| 2xxx | 收银台 / 支付 |
| 3xxx | 订单 |
| 4xxx | 退款 |
| 5xxx | 商户 |
| 6xxx | 渠道 / 路由 |
| 6100-6199 | 银联渠道 |
| 7500-7599 | 对账 |

```java
// ✅ 使用 BizException + 错误码
throw new BizException(2001, "订单不存在：" + orderNo);
```

### 日志规范 [强制]

- **ERROR**：需要人工介入的故障（支付失败、数据库连接失败、第三方 API 错误）
- **WARN**：可恢复的异常（重试成功、降级触发、配置缺失有默认值）
- **INFO**：关键业务流程节点（订单创建、支付回调、对账结果）
- **DEBUG**：开发调试信息（不打印到生产日志）
- **TraceId 全链路追踪** [强制]：使用 MDC 设置 `traceId`，所有日志消息自动包含

```java
// ✅ 登录日志级别使用
log.info("订单创建成功 orderNo={}, amount={}, mchId={}", orderNo, amount, mchId);
log.warn("支付回调验签失败，准备重试 orderNo={}", orderNo);
log.error("支付渠道异常 orderNo={}", orderNo, exception);
```

---

## 测试规范

### Definition of Done [强制]

一个功能/任务只有在以下 **5 项条件全部满足** 时才能标记为"已完成"：

1. ✅ 代码通过 Code Review
2. ✅ 单元测试全部通过
3. ✅ 集成测试全部通过
4. ✅ 相关文档已更新（`docs/CONTRACT_MATRIX.md`、CLAUDE.md、迁移 SQL）
5. ✅ 宪法合规检查通过（Constitution Check 门禁）

任何一项不满足，功能不得合并、不得部署、不得标记为完成。

### 覆盖率要求 [强制]

- **最低行覆盖率：80%**，按 Maven 模块聚合计算（如 `payflow-cashier-server` 模块整体达到 80%）
- 前端项目同理按项目聚合计算（`admin-client`、`cashier-client` 各达 80%）
- 覆盖率结果通过 JaCoCo 报告，CI 流水线检查

```xml
<!-- JaCoCo 配置示例 -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <goals><goal>check</goal></goals>
            <configuration>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 单元测试 [强制]

- 使用 JUnit 5 + Mockito
- Service 层方法必须覆盖正常流程、异常流程、边界条件
- 禁止 Mock 数据库——存储层使用真实嵌入式 DB（H2）或 Testcontainers
- 测试方法使用 `@DisplayName` 中文描述

```java
@Test
@DisplayName("支付成功——正常参数返回成功结果")
void shouldReturnSuccessWhenPayWithValidParams() {
    when(payStrategy.execute(any())).thenReturn(PayResult.success());
    PayResult result = paymentService.pay(dto);
    assertEquals(PayStatus.SUCCESS, result.getStatus());
}
```

### 集成测试 [推荐]

- 使用 Testcontainers 启动真实 MySQL/Redis 容器
- 核心支付流程（下单→支付→回调→对账）必须覆盖完整链路
- 容器复用：`TESTCONTAINERS_REUSE_ENABLE=true`

### 前端测试 [推荐]

- 使用 Vitest + Vue Test Utils
- API Mock 使用 MSW（Mock Service Worker）
- 关键用户流程必须有测试（收银台下单、管理后台配置保存）

---

## 自动化执行

### IDE 插件 [强制]

开发者必须安装以下 IDE 插件：

| 插件 | 用途 | 安装方式 |
|------|------|----------|
| **Alibaba Java Coding Guidelines** | 实时扫描编码规范违规 | IDEA 插件市场 |
| **Checkstyle-IDEA** | Google + 项目自定义检查规则 | IDEA 插件市场 + 导入 `checkstyle.xml` |
| **SonarLint** | 实时代码异味检测 | IDEA 插件市场 |

前端（VS Code）：

| 插件 | 用途 |
|------|------|
| **ESLint** | TypeScript/Vue 代码规范检查 |
| **Prettier** | 代码自动格式化 |
| **Vue - Official** | Vue 3 语法支持 |

### CI 流水线质量门禁 [强制]

CI 流水线按以下顺序执行，任一门禁失败则构建终止：

```
Checkstyle → PMD/SpotBugs → SonarQube → JaCoCo (≥80%) → Maven Test
```

- **Checkstyle**：基于 `google_checks.xml` 定制，检查命名、格式、导入
- **SonarQube**：必须通过 Quality Gate（Bug = 0, Vulnerability = 0, Coverage ≥ 80%）
- **JaCoCo**：行覆盖率 < 80% 时构建失败

### Git Hooks [推荐]

```bash
# pre-commit: 快速 Checkstyle 检查（仅检查变更文件）
mvn checkstyle:check -pl $(git diff --name-only HEAD | grep -oP 'payflow-\w+' | head -1)
```

---

## 前端规范

### Vue 3 编码规范 [强制]

- **组件文件名**：PascalCase（`PaymentForm.vue`），模板中使用 kebab-case（`<payment-form />`）
- **脚本语法**：使用 `<script setup lang="ts">`（Composition API 优先）
- **Props 定义**：必须声明类型和默认值
- **Emit 定义**：使用 `defineEmits<{ ... }>()` TypeScript 声明

```vue
<!-- ✅ 正确 -->
<script setup lang="ts">
interface Props { orderNo: string; amount?: number; }
const props = withDefaults(defineProps<Props>(), { amount: 0 });
const emit = defineEmits<{ (e: 'confirm', orderNo: string): void }>();
</script>
```

### TypeScript 类型规范 [强制]

- **禁止 `any` 滥用**：仅在确实无法确定类型时使用，并加 `// eslint-disable-next-line @typescript-eslint/no-explicit-any`
- 接口定义优先于 type 别名（需要联合类型时用 type）
- API 响应类型必须与后端的 DTO 严格对应

```typescript
// ✅ 正确
interface PaymentOrder {
  id: string;       // Long 序列化为 string
  orderNo: string;
  amount: number;   // 前端显示用元，后端传分
  status: OrderStatus;
}
```

### Pinia 状态管理 [推荐]

- Store 命名：`useXxxStore`（如 `useOrderStore`、`useUserStore`）
- 每个 Store 职责单一，超过 300 行需拆分

### API 调用规范 [强制]

- Axios 拦截器自动从响应 `{ code, message, data }` 中提取 `data`
- 所有 API 调用使用相对路径（`/api/v1/...`）
- 错误在拦截器中统一处理，调用方只需处理业务逻辑

**前端覆盖率** [强制]：与后端一致，最低行覆盖率 80%，按项目聚合计算（admin-client、cashier-client 各达 80%）。

---

## 技术约束

### 技术栈（锁定）

| 层级 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 17 |
| 框架 | Spring Boot | 3.2.5 |
| ORM | MyBatis-Plus | 3.5.7 |
| 构建 | Maven | （父 POM 统一管理） |
| 前端框架 | Vue 3 + TypeScript | 3.4 |
| 前端构建 | Vite | 5 |
| UI 组件库 | Element Plus | 2.5+ |
| 前端状态管理 | Pinia | 2.1 |
| 前端 HTTP | Axios | 1.6 |
| 消息队列 | RocketMQ | （可选，默认关闭） |
| 缓存 | Redis | — |
| 任务调度 | XXL-Job | 2.4.1（可选，默认关闭） |
| API 文档 | SpringDoc OpenAPI | 2.5.0 |

**规则**：
- 禁止引入替代的 ORM 框架、构建工具或前端框架。
- 可选依赖（RocketMQ、XXL-Job）必须使用 `@ConditionalOnProperty` 做特性开关。
- `admin-client` 和 `cashier-client` 的公共前端依赖版本必须保持一致。

### 端口分配

| 服务 | 端口 |
|------|------|
| `payflow-cashier-server` | 3002 |
| `payflow-admin-server` | 3003 |
| `payflow-recon-server` | 3004 |
| `payflow-admin-client`（开发） | 3001 |
| `payflow-cashier-client`（开发） | 5173 |

### 实体与 DTO 分离

- **实体类**（`entity/` 包）：MyBatis-Plus `@TableName` 映射，禁止直接在 Controller 中暴露。
- **DTO**（`dto/` 包）：API 契约，仅定义前端需要的字段。
- **Service 层**负责 Entity ↔ DTO 转换。Controller 只接收和返回 DTO。

### 金额约定

所有金额以 **Long 型整数（分/fen）** 存储和传输。风控阈值和金额比较必须直接用分，禁止使用浮点类型做元/分转换。

---

## 开发工作流

### 新增支付渠道的标准流程

1. 在 `payflow-payment-channels/` 下创建 `payflow-payment-{channel}` 子模块
2. 在 `payflow-core` 的 `PayMethod` 枚举中新增常量
3. 在 `payflow-cashier-server` 中创建策略 + 开放服务 + 回调处理
4. 将渠道种子数据添加到 `sql/migrations/` 和 `sql/full-reseed-payflow-demo.sql`
5. 在 `payflow-admin-client` 中新增渠道配置页面
6. 如需对账：新增账单解析器和 `ReconChannelOpenService` 实现
7. 更新 `docs/CONTRACT_MATRIX.md`
8. 更新 `PayChannelOpenServiceLocator.toBeanName()` 添加渠道映射

### 数据库迁移规范

1. 在 `sql/migrations/` 中创建时间戳命名的 SQL 文件（格式：`YYYY-MM-DD_描述.sql`）
2. Schema 变更必须向后兼容（增量式）
3. 更新 `sql/full-reseed-payflow-demo.sql` 以反映新 Schema
4. 在相关架构文档中记录新表和字段

### 测试门禁流程 [强制]

```
编码完成 → Code Review → 单元测试通过 → 集成测试通过
    → JaCoCo 覆盖率 ≥ 80% → 宪法合规检查 → 文档更新确认
    → 合并到主分支
```

以上任一步骤失败，代码不得合并、不得标记为已完成。

---

## 治理

### 修订流程

1. 提议的变更必须附带理由和影响分析文档。
2. 对"不可协商"原则的修改需要 MAJOR 版本号递增。
3. 新增原则或章节需要 MINOR 版本号递增。
4. 文字澄清和措辞修正需要 PATCH 版本号递增。

### 版本规则

- **MAJOR**：向后不兼容的治理变更、删除或重新定义原则。
- **MINOR**：新增原则或章节、实质性扩展指导内容。
- **PATCH**：澄清、错别字修正、非语义优化。

### 合规审查

#### Code Review Checklist

**每次 Code Review 必须逐项检查：**

**模块边界（原则 I）**
- [ ] 代码位于正确的 Maven 模块中
- [ ] 无跨模块直接依赖（cashier 不直接引用 admin 实体类）
- [ ] payflow-common 中无 Spring Bean 或业务逻辑

**支付渠道抽象（原则 II）**
- [ ] 无直接注入具体渠道 Handler
- [ ] 新支付方式实现了 `PayStrategy` 接口
- [ ] 渠道配置通过 `ChannelConfigHolder` 传递

**API 响应（原则 IV）**
- [ ] Controller 返回统一格式 `{ code, message, data }`
- [ ] 无自创响应包装类
- [ ] 异常通过全局处理器统一返回格式

**安全合规（原则 V + 安全编码）**
- [ ] 敏感字段使用 AES-256-GCM 加密存储
- [ ] 日志中无敏感信息（手机号/密码/密钥）
- [ ] JWT 密钥/主密钥来自配置文件，非硬编码
- [ ] CORS 白名单配置正确

**SQL 安全（数据库访问）**
- [ ] 无 XML 中的 `${}`
- [ ] 无 `SELECT *`
- [ ] 批量操作有数量限制

**测试合规（测试规范）**
- [ ] 新增业务逻辑有对应单元测试
- [ ] JaCoCo 覆盖率 ≥ 80%
- [ ] Definition of Done 五项条件全部满足

#### 自动化规则映射表

| 规则 | 检查工具 | 规则标识 |
|------|----------|----------|
| 命名规范（类/方法/常量） | Alibaba Java Coding Guidelines | NamingConventionRule |
| 通配符导入禁止 | Checkstyle | AvoidStarImport |
| 大括号必须 | Checkstyle | NeedBraces |
| 单行 ≤ 120 字符 | Checkstyle | LineLength |
| `Executors` 禁用 | Alibaba Java Coding Guidelines | ThreadPoolCreationRule |
| `SELECT *` 禁止 | SonarQube | squid:S2201 |
| XML `${}` 禁止 | SonarQube | squid:S3649 |
| 日志脱敏 | SonarQube（自定义规则） | custom:LogMaskRule |
| JaCoCo 覆盖率 | JaCoCo Maven Plugin | LINE COVEREDRATIO ≥ 0.80 |

### 合规执行

- 所有功能计划（`plan.md`）在进入 Phase 0 前，必须通过 Constitution Check 门禁。
- CI 流水线阻断任何违反[强制]规则的代码合并。
- [推荐]级别规则违反需在 Code Review 中提供书面豁免理由。
- `CLAUDE.md` 文件在运行时开发指导上优先于本宪法。两者如有冲突，必须通过修订其中一份来解决。

**版本**: 2.0.0 | **批准日期**: 2026-05-10 | **最后修订**: 2026-05-13
