# API Contract: 通知中心

**Base Path**: `/api/v1/admin/notifications`
**Auth**: JWT Bearer Token（`JwtInterceptor`）
**Data Isolation**: 所有端点强制 `merchantScope` 过滤

---

## GET /notifications

通知列表（分页）。

**Query Parameters**:

| 参数 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| `read` | String | 否 | `all` | `true` / `false` / `all` |
| `type` | String | 否 | — | 业务类型枚举（`REFUND_APPROVAL` 等） |
| `page` | Integer | 否 | 1 | 页码（≥1） |
| `size` | Integer | 否 | 20 | 每页条数（≤100） |

**Response** `200`:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "bizType": "REFUND_APPROVAL",
        "title": "退款 REF-20260525-001 等待审批",
        "summary": "商户 M100001 发起退款 ¥158.00",
        "link": "/admin/refunds?status=REFUNDING",
        "readStatus": 0,
        "createdAt": "2026-05-26T10:30:00"
      }
    ],
    "total": 42,
    "page": 1,
    "size": 20
  }
}
```

---

## GET /notifications/unread-count

未读总数（顶栏 badge 用）。

**Response** `200`:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "count": 7
  }
}
```

---

## POST /notifications/{id}/read

标记单条为已读。

**Response** `200`:

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

**Error** `404` — 通知不存在或不属于当前用户。

---

## POST /notifications/read-all

标记当前用户全部未读为已读。

**Response** `200`:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "affected": 12
  }
}
```

---

## POST /notifications/read-batch

批量标记已读。

**Request Body**:

```json
{
  "ids": [1, 2, 5, 8]
}
```

**Response** `200`:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "affected": 4
  }
}
```

---

## GET /notifications/summary（保留兼容）

现有 summary 端点保留，扩展返回 `unreadCount` 字段。

**Response** `200`:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "pendingRefunds": 2,
    "pendingChurnAlerts": 1,
    "overdueChurnAlerts": 0,
    "unreadCount": 7,
    "announcements": []
  }
}
```

---

## 错误码

| 错误码 | 说明 |
|--------|------|
| 8001 | 通知不存在 |
| 8002 | 通知不属于当前用户 |
| 8003 | 无效的通知类型 |
