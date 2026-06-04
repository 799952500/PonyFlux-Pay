# Contract: 收银台订单信息

**Service**: `payflow-cashier-server`  
**Path**: `GET /api/v1/cashier/{orderId}`  
**Auth**: 签名参数 `sig`（公开收银台）

## Query Parameters

| 参数 | 必填 | 说明 |
|------|------|------|
| `sig` | 是 | 订单签名校验 |
| `client` | 否 | `PC` \| `H5` \| `APP`，过滤支付方式终端范围 |

## Response `data`（CashierResponse）

### 新增字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `displayLanguage` | string | `zh-CN` \| `zh-TW` \| `en-US` |

### 支付方式项（PaymentMethodDTO）

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | string | 方式编码 |
| `name` | string | 已按 `displayLanguage` 解析的展示名 |
| `description` | string | 已按 `displayLanguage` 解析的描述 |
| `icon` | string | 图标 |
| `recommended` | boolean | 是否推荐 |

### 示例片段

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "orderId": "PF2026060312345678",
    "merchantName": "商户-M001",
    "subject": "测试商品",
    "amount": 10000,
    "currency": "CNY",
    "displayLanguage": "zh-TW",
    "paymentMethods": [
      {
        "code": "WECHAT_NATIVE",
        "name": "微信支付（掃碼）",
        "description": "使用微信掃碼完成付款",
        "icon": "/icons/wechat.svg",
        "recommended": true
      }
    ]
  }
}
```

## 服务端行为

1. 从订单读取 `display_language` 填入 `displayLanguage`。
2. 调用管理端内部接口时附带 `locale={displayLanguage}`。
3. 内置 fallback 支付方式列表仍使用 i18n 键（仅 admin 不可用时）。

## 前端行为

- 收到 `displayLanguage` 后调用 `applyLocale(displayLanguage)`。
- 订单页不展示语言切换器。
