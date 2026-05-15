# Data Model: 生产环境加固

**Feature**: 005-production-hardening
**Date**: 2026-05-15

## 实体变更概览

| 实体 | 变更类型 | 所属模块 | 数据库 | 说明 |
|------|----------|----------|--------|------|
| `Merchant` (admin) | 修改 | admin-server | payflow_admin | 添加 `@JsonIgnore` 到 `merchantKey` 字段 |
| `ChannelAccount` (admin) | 修改 | admin-server | payflow_admin | 添加 `@JsonIgnore` 到 `appSecret`/`mchKey`/`certPassword`；添加 TypeHandler 加密 |
| `Channel` (admin) | 修改 | admin-server | payflow_admin | `apiKey` 字段添加 TypeHandler 加密 |
| `PaymentAccount` (admin) | 修改 | admin-server | payflow_admin | 添加敏感字段过滤 |
| `Order` (cashier) | 修改 | cashier-server | payflow_cashier | `payAmount` 修复：支付成功时填充 |
| `Refund` (cashier) | 修改 | cashier-server | payflow_cashier | 添加 `@Version` 乐观锁字段 |
| `WebhookDeliveryLog` | 现有（无代码使用） | cashier-server | payflow_cashier | 已有 DDL 和实体，需创建 Service/Mapper |
| `MerchantWebhookEndpoint` | 现有（无代码使用） | cashier-server | payflow_cashier | 已有 DDL 和实体，需创建 Service/Mapper |

## 实体详细设计

### 1. Merchant（admin-server）——敏感字段脱敏

```java
// 现有字段变更
public class Merchant extends BaseEntity {
    // ...
    @JsonProperty(access = Access.WRITE_ONLY)  // 反序列化时接受，序列化时排除
    private String merchantKey;
    // ...
}
```

**变更理由**: `merchantKey` 是商户 HMAC 签名密钥，绝不应在 API 响应中泄露。`WRITE_ONLY` 允许创建/更新操作接收该值（从请求体反序列化），但在序列化响应时自动排除。

### 2. ChannelAccount（admin-server）——加密 + 脱敏

```java
public class ChannelAccount extends BaseEntity {
    // ...
    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    @JsonProperty(access = Access.WRITE_ONLY)
    private String appSecret;

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    @JsonProperty(access = Access.WRITE_ONLY)
    private String mchKey;

    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    @JsonProperty(access = Access.WRITE_ONLY)
    private String certPassword;

    @JsonProperty(access = Access.WRITE_ONLY)
    private String configJson;  // H-1: AdminChannelAccountController 暴露此字段
    // ...
}
```

**TypeHandler 设计**:
```java
// 放在 payflow-common 或 admin-server
public class EncryptedStringTypeHandler extends BaseTypeHandler<String> {
    private static AesEncryptor encryptor; // 从 CryptoProperties 获取 masterKey
    
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String param, JdbcType jdbcType) {
        ps.setString(i, encryptor.encrypt(param, masterKey));
    }
    
    @Override
    public String getNullableResult(ResultSet rs, String columnName) {
        String ciphertext = rs.getString(columnName);
        return ciphertext != null ? encryptor.decrypt(ciphertext, masterKey) : null;
    }
    // getNullableResult(ResultSet, int) 和 getNullableResult(CallableStatement, int) 同理
}
```

### 3. Order（cashier-server）——payAmount 修复

无需修改实体定义，仅修改业务逻辑：

- **修复前**: `PayNotifyService.handlePaymentSuccess()` → `orderService.updateOrderStatus(orderId, STATUS_PAID, null)`
- **修复后**: `PayNotifyService.handlePaymentSuccess()` → `orderService.updateOrderStatus(orderId, STATUS_PAID, payment.getAmount())`

`payment.getAmount()` 来自已加载的 `Payment` 实体，值为渠道回调返回的实际支付金额（分）。

### 4. Refund（cashier-server）——乐观锁

```java
public class Refund extends BaseEntity {
    // ... 现有字段 ...
    
    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;  // 新增乐观锁字段
}
```

**DDL 变更**:
```sql
ALTER TABLE cashier_refunds ADD COLUMN version INT NOT NULL DEFAULT 0;
```

**并发保护机制**:
- `RefundServiceImpl.refund()` 方法中的 `@Transactional` 已有
- MyBatis-Plus 的 `OptimisticLockerInnerInterceptor`（已配置）自动在 UPDATE 时使用 `WHERE version = ?` 并递增 `version`
- 如果两个并发退款更新同一 Payment 的相关记录，一个会因版本冲突失败（返回 0 行更新）

### 5. WebhookDeliveryLog & MerchantWebhookEndpoint（cashier-server）——激活已有实体

这两个实体的 DDL 已在 `20260507-plan-extensions.sql` 中定义，实体类存在于 cashier-server 但从未被业务代码使用。需要创建：

```
payflow-cashier-server/src/main/java/com/payflow/cashier/
├── service/
│   ├── WebhookDeliveryService.java          # 新增
│   └── impl/WebhookDeliveryServiceImpl.java # 新增
├── mapper/
│   ├── WebhookDeliveryLogMapper.java        # 新增（如尚不存在）
│   └── MerchantWebhookEndpointMapper.java   # 新增（如尚不存在）
└── task/
    └── WebhookRetryTask.java                # 新增：@Scheduled 扫描待重试记录
```

## 状态机定义

### Merchant 状态转换

```
ACTIVE ──────────────────────────> SUSPENDED
  │        （管理员暂停）               │
  │                                    │
  │  <─────────────────────────────    │
  │        （管理员恢复）               │
  │                                    │
  ├────────────────────────────────> CLOSED
  │        （管理员关闭）               （不可逆）
  │
  └────────────────────────────────> DELETED（新增）
           （管理员删除）               （不可逆，软删除）
```

**规则**:
- ACTIVE ↔ SUSPENDED: 允许双向
- ACTIVE → CLOSED: 允许，不可逆
- SUSPENDED → CLOSED: 允许，不可逆
- ACTIVE/SUSPENDED/CLOSED → DELETED: 允许（软删除，`deleted=1`）

### Refund 过程状态（现有，不变）

```
REFUNDING → REFUNDED（成功）
REFUNDING → FAILED（失败）
```

新增乐观锁不影响状态机，仅在并发更新时提供检测。

## 索引与约束变更

| 表 | 变更 | 说明 |
|----|------|------|
| `cashier_refunds` | 新增 `version` 列 | 乐观锁 |
| `cashier_refunds` | 新增复合索引 `(payment_id, status)` | 优化 `sumRefundedAmount()` 查询 |
| `admin_merchant_payment_methods` | 新增唯一约束 `(merchant_id, payment_method_id)` | 防止重复绑定 |

## 新增实体：EncryptedStringTypeHandler（基础设施，不映射表）

此 TypeHandler 是 MyBatis 持久化层组件，在加密/解密敏感字段时使用。它不是数据库实体，不映射任何表。主密钥来源：`@Value("${payflow.crypto.master-key}")` 从环境变量/配置注入。
