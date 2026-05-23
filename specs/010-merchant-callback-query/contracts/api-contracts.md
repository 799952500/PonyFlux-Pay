# Contracts: 商户回调记录查询

## 统一约定

- Base path: `/api/v1/admin/merchant-notifies`
- 响应格式: `{ "code": 0, "message": "success", "data": { ... } }`
- 鉴权: JWT + `AdminRequestContext.merchantScope`
- 跨商户拒绝: `code` 非 0，message 不暴露目标订单是否存在

## 数据范围

| 角色 | 规则 |
|------|------|
| 商户管理员 | 仅 `authorizedMerchantIds` 内 `merchant_id` |
| 系统管理员 | 可按 `merchantId` 筛选；未传则查授权范围内全部 |

---

## GET /api/v1/admin/merchant-notifies

分页查询商户回调汇总列表。

**Query Parameters**:

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `merchantId` | string | 否 | 商户号（系统管理员可选） |
| `orderId` | string | 否 | 平台订单号 |
| `merchantOrderNo` | string | 否 | 商户订单号 |
| `notifyType` | string | 否 | `PAYMENT` / `REFUND` |
| `summaryStatus` | string | 否 | 汇总状态 |
| `startTime` | datetime | 否 | 最近尝试时间起 |
| `endTime` | datetime | 否 | 最近尝试时间止 |
| `page` | int | 否 | 默认 1 |
| `size` | int | 否 | 默认 20，最大 100 |

**Response `data`**:

```json
{
  "total": 100,
  "page": 1,
  "size": 20,
  "list": [
    {
      "notifyId": "MN202605220001",
      "orderId": "ORD-20260518-0003",
      "merchantId": "M100001",
      "merchantOrderNo": "MO-001",
      "notifyType": "PAYMENT",
      "notifyUrl": "https://merchant.example/notify",
      "summaryStatus": "FAILED",
      "attemptCount": 3,
      "lastAttemptAt": "2026-05-22T10:00:00",
      "lastFailReason": "RESPONSE_NOT_SUCCESS",
      "lastResponsePreview": "fail",
      "orderStatus": "PAID",
      "notifyPayloadStatus": "PAID"
    }
  ]
}
```

**脱敏**: 列表不返回完整 `request_params`；`notifyUrl` 可完整展示（非密钥）。

---

## GET /api/v1/admin/merchant-notifies/{notifyId}

查询单条汇总及全部尝试明细。

**Path**: `notifyId`

**Response `data`**:

```json
{
  "summary": {
    "notifyId": "MN202605220001",
    "orderId": "ORD-20260518-0003",
    "merchantId": "M100001",
    "merchantOrderNo": "MO-001",
    "notifyType": "PAYMENT",
    "notifyUrl": "https://merchant.example/notify",
    "summaryStatus": "FAILED",
    "attemptCount": 3,
    "lastAttemptAt": "2026-05-22T10:00:00",
    "lastFailReason": "商户响应未包含成功标识",
    "orderStatus": "PAID",
    "notifyPayloadStatus": "PAID",
    "createdAt": "2026-05-22T09:58:00",
    "updatedAt": "2026-05-22T10:00:00"
  },
  "attempts": [
    {
      "attemptNo": 1,
      "resultStatus": "FAILED",
      "failReasonType": "RESPONSE_NOT_SUCCESS",
      "failReasonDetail": "body=fail",
      "httpStatus": 200,
      "durationMs": 120,
      "requestParams": { "orderId": "ORD-...", "sign": "abcd****wxyz" },
      "responseBody": "fail",
      "truncated": false,
      "createdAt": "2026-05-22T09:59:00"
    }
  ]
}
```

**脱敏**: `requestParams.sign` 及键名含 `secret`/`key` 的字段掩码处理。

**错误**:
- 汇总不存在或越权: `code=403` 或业务码（与项目 BizException 段对齐），`message` 通用化。

---

## GET /api/v1/admin/merchant-notifies/by-order/{orderId}

按订单查询该订单下所有类型的回调汇总（便捷入口，供订单页跳转 P3）。

**Query**: `notifyType` 可选，过滤单一类型。

**Response `data`**:

```json
{
  "orderId": "ORD-20260518-0003",
  "orderStatus": "PAID",
  "summaries": [ { "...": "同列表项字段" } ]
}
```

无记录时 `summaries: []`，若订单存在但未配置回调，`summaryStatus=NOT_CONFIGURED` 的汇总可由 Worker 预创建后返回。

---

## 前端路由契约

| 路由 | 页面 | 说明 |
|------|------|------|
| `/admin/merchant-notifies` | 列表 + 筛选 | P2 |
| `/admin/merchant-notifies/:notifyId` | 详情（抽屉或子页） | P1 |
| 订单详情「查看商户回调」 | 跳转 `by-order` 或列表带 `orderId` 查询 | P3 可选 |

菜单 seed: `menu_code=merchant_notifies`, `menu_name=商户回调记录`, parent=`grp_trade`, `sort_order=3`

---

## 业务错误码（建议段）

| code | 场景 |
|------|------|
| 403 / 7401 | 越权或商户范围外 |
| 7404 | 回调记录不存在（仅当调用方在授权范围内） |

具体码段在 tasks 实现时与 `payflow-common` 错误码表对齐。
