# API 契约：商户隔离与安全审计

**Feature**: 006-merchant-isolation  
**Date**: 2026-05-18

## 1. 错误响应契约（收银台 `payflow-cashier-server`）

所有端点保持既有包装格式 `R<T>`（`code` / `message` / `data`）。

### 1.1 商户身份字段不匹配（FR-002）

**触发**: 请求体或 query 中 `merchantId` 与 JWT/HMAC 上下文不一致。

```http
HTTP/1.1 403 Forbidden
Content-Type: application/json

{
  "code": 5101,
  "message": "商户身份与请求不匹配",
  "data": null
}
```

**副作用**: 异步写入 `cashier_security_audit`，`reason_code=5101`。

### 1.2 资源不存在或无权限（FR-005 / FR-006）

**触发**: 资源 ID 不存在，或资源属于其他商户（读/写均适用）。

```http
HTTP/1.1 404 Not Found
Content-Type: application/json

{
  "code": 5102,
  "message": "请求的资源不存在",
  "data": null
}
```

**注意**: 真实不存在与跨商户越权 **必须使用相同 JSON**（含 `message` 文案一致）。

**副作用**: 审计 `reason_code` 可为 `5102` 或 `5103`，不得返回给客户端。

---

## 2. 受影响的收银台端点（行为变更摘要）

以下端点 **不新增路径**，仅加强校验逻辑。

| 方法 | 路径 | 认证 | 变更要点 |
|------|------|------|----------|
| POST | `/api/v1/orders` | JWT | 禁止覆盖 merchantId；不一致 403 |
| GET | `/api/v1/orders/{orderId}` | JWT | 跨商户 404 |
| POST | `/api/v1/payments` | HMAC | 跨资源 404 |
| GET | `/api/v1/payments/status/{paymentId}` | 无签名（消费者轮询） | **保持现状**，不在本次 merchant 隔离范围 |
| POST | `/api/v1/refunds` | HMAC | paymentId 级联校验 |
| GET | `/api/v1/refunds/{refundId}` | HMAC | 跨商户 404 |
| GET | `/api/v1/merchant/orders/{orderId}` | HMAC | 跨商户 404 |
| GET | `/api/v1/merchant/payments/{paymentId}/status` | HMAC | 跨商户 404 |
| POST/GET | `/api/v1/payment-links` | HMAC | list/create 限定本商户 |

### 2.1 请求体 `merchantId` 字段（弃用说明）

OpenAPI 描述更新为：

> **已弃用**：若携带则必须与认证上下文一致，否则返回 5101。推荐省略该字段。

---

## 3. 管理端：安全审计查询 API（`payflow-admin-server`）

### 3.1 分页查询

```http
GET /api/v1/admin/security/audit
Authorization: Bearer {admin_jwt}
```

**权限**: `RISK` 或 `SUPER_ADMIN`（`@RequireRole`）

**Query 参数**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `merchantId` | string | 否 | 调用方商户号 |
| `outcome` | string | 否 | 默认 `DENIED` |
| `reasonCode` | string | 否 | `5101` / `5102` / `5103` |
| `requestPath` | string | 否 | 路径包含匹配 |
| `startTime` | string | 否 | ISO-8601，如 `2026-05-18T00:00:00` |
| `endTime` | string | 否 | ISO-8601 |
| `page` | int | 否 | 默认 1 |
| `pageSize` | int | 否 | 默认 20，最大 100 |

**成功响应**（admin 统一格式）:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 42,
    "page": 1,
    "pageSize": 20,
    "records": [
      {
        "id": "1001",
        "merchantId": "M100001",
        "targetMerchantId": "M100002",
        "authMode": "JWT",
        "httpMethod": "POST",
        "requestPath": "/api/v1/orders",
        "resourceType": null,
        "resourceId": null,
        "clientIp": "203.0.113.1",
        "userAgent": "curl/8.0",
        "outcome": "DENIED",
        "reasonCode": "5101",
        "createdAt": "2026-05-18T10:00:00"
      }
    ]
  }
}
```

**无权限**:

```json
{
  "code": 1003,
  "message": "无权限访问",
  "data": null
}
```

HTTP 403。

---

## 4. 白名单路径（不经过 merchantId 绑定与资源所有权拦截）

| 模式 | 说明 |
|------|------|
| `/api/v1/auth/**` | 登录/登出 |
| `/api/v1/cashier/**` | 消费者收银台 |
| `/api/v1/public/**` | Payment Link 公开查询 |
| `/api/v1/callbacks/**`、`/notify/**` | 渠道回调 |
| `/api/v1/payments/status/**` | 支付状态轮询（无商户头） |
| `/swagger-ui/**`、`/v3/api-docs/**` | 文档 |
| `/actuator/**` | 健康检查 |

---

## 5. 契约变更记录

| 版本 | 日期 | 变更 |
|------|------|------|
| 1.0 | 2026-05-18 | 初版：5101/5102/5103、admin 审计 API |

实施完成后须同步更新 [`docs/CONTRACT_MATRIX.md`](../../../docs/CONTRACT_MATRIX.md)。
