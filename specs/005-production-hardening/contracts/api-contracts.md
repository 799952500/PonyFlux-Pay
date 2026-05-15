# API Contracts: 生产环境加固

**Feature**: 005-production-hardening
**Date**: 2026-05-15

本目录包含本次加固涉及的所有 API 契约变更。

---

## 1. 安全错误响应契约

### 1.1 未认证响应（401）

所有需要认证的端点（包括 `/api/v1/merchants/*`），未携带有效 JWT Token 时返回：

```json
{
  "code": 1002,
  "message": "未授权访问：请先登录",
  "data": null
}
```

HTTP Status: 401 Unauthorized

### 1.2 权限不足响应（403）

非 SUPER_ADMIN 角色尝试执行受限操作（创建管理员、修改系统配置、修改角色权限）时返回：

```json
{
  "code": 1003,
  "message": "权限不足：此操作需要超级管理员权限",
  "data": null
}
```

HTTP Status: 403 Forbidden

### 1.3 服务不可用响应（503）

Redis 不可用导致安全拦截器 fail-close 时返回：

```json
{
  "code": 5000,
  "message": "服务暂不可用，请稍后重试",
  "data": null
}
```

HTTP Status: 503 Service Unavailable

---

## 2. 商户管控错误响应

### 2.1 商户已暂停（402/423）

被暂停或关闭的商户尝试创建订单/支付/退款时返回：

```json
{
  "code": 5001,
  "message": "商户已暂停服务，请联系平台",
  "data": null
}
```

HTTP Status: 423 Locked

### 2.2 商户登录锁定

登录失败超过 5 次后，账户被锁定 15 分钟：

```json
{
  "code": 1004,
  "message": "账户已锁定，请 15 分钟后重试",
  "data": {
    "lockedUntil": "2026-05-15T14:30:00"
  }
}
```

HTTP Status: 429 Too Many Requests

---

## 3. Webhook 通知契约

### 3.1 支付成功通知（HTTP POST → 商户 notifyUrl）

**请求**:
```http
POST {merchant_notify_url}
Content-Type: application/json
X-Payflow-Signature: {HMAC-SHA256(payload, merchantKey)}
X-Payflow-Event: payment.success
X-Payflow-Delivery-Id: wdl_xxxxx
X-Trace-Id: {trace_id}

{
  "orderId": "PO1682400000001001",
  "paymentId": "PAY1682400000001002",
  "merchantOrderNo": "M202605150001",
  "amount": 10000,
  "currency": "CNY",
  "status": "PAID",
  "channelTransactionId": "4200001234567890",
  "paidAt": "2026-05-15T10:30:00"
}
```

**期望响应**: HTTP 2xx（任意 2xx 状态码视为成功）

**重试策略**: 失败后按 1min → 5min → 15min → 30min → 1h 间隔重试（最多 5 次）

### 3.2 退款成功通知

```json
{
  "orderId": "PO1682400000001001",
  "paymentId": "PAY1682400000001002",
  "refundId": "REF1682400000002001",
  "merchantRefundNo": "RTN202605150001",
  "refundAmount": 5000,
  "currency": "CNY",
  "status": "REFUNDED",
  "refundedAt": "2026-05-15T11:00:00"
}
```

---

## 4. 敏感字段过滤契约

### 4.1 商户数据响应（脱敏后）

`GET /api/v1/admin/merchants/{merchantId}` 以及 `GET /api/v1/merchants/*` 返回的商户数据中，以下字段被排除：

| 排除字段 | 说明 |
|----------|------|
| `merchantKey` | HMAC 签名密钥 |
| （如果有 password） | 密码哈希 |

**示例响应**（注意缺少 `merchantKey`）:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "merchantId": "M100001",
    "merchantName": "测试商户",
    "merchantType": "ENTERPRISE",
    "status": "ACTIVE",
    "contactEmail": "test@example.com",
    "contactPhone": "138****1234",
    "commissionRate": 0.006,
    "createdAt": "2026-01-01T00:00:00"
  }
}
```

### 4.2 渠道账户数据响应（脱敏后）

`GET /api/v1/admin/channel-accounts/*` 返回的渠道账户数据中：

| 排除字段 | 说明 |
|----------|------|
| `appSecret` | 渠道应用密钥 |
| `mchKey` | 商户密钥 |
| `certPassword` | 证书密码 |
| `configJson` | 包含序列化密钥的扩展配置 |

---

## 5. 健康检查增强契约

### 5.1 增强健康检查响应

`GET /actuator/health`

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "validationQuery": "SELECT 1"
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.0.12"
      }
    },
    "paymentChannels": {
      "status": "UP",
      "details": {
        "alipay": "UP",
        "wechat": "UP",
        "unionpay": "UP"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 100000000000,
        "free": 50000000000
      }
    }
  }
}
```

### 5.2 支付指标端点

`GET /actuator/metrics/payment.success.count`

```json
{
  "name": "payment.success.count",
  "measurements": [
    { "statistic": "COUNT", "value": 15234 }
  ],
  "availableTags": [
    { "tag": "channel", "values": ["alipay", "wechat", "unionpay"] },
    { "tag": "merchantId", "values": ["M100001", "M100002", "..."] }
  ]
}
```

---

## 6. 安全响应头契约

所有 HTTP 响应必须包含以下安全头：

```http
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Trace-Id: {uuid}
```

- `Strict-Transport-Security` 仅在 HTTPS 环境下添加
- `X-Trace-Id` 为每个请求的唯一追踪标识
