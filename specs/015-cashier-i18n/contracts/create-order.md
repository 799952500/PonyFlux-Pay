# Contract: 创建订单（展示语言）

**Service**: `payflow-cashier-server`  
**Path**: `POST /api/v1/orders`  
**Auth**: JWT（商户）+ 现有签名校验链

## Request Body

在现有 `CreateOrderRequest` 基础上新增：

| 字段 | 类型 | 必填 | 约束 | 说明 |
|------|------|------|------|------|
| `language` | string | 否 | `zh-CN` \| `zh-TW` \| `en-US` | 收银台展示语言 |

其余字段不变（`merchantOrderNo`, `amount`, `channel`, `subject`, …）。

### 示例

```json
{
  "merchantOrderNo": "M20260603001",
  "amount": 10000,
  "currency": "CNY",
  "channel": "WECHAT_PAY",
  "notifyUrl": "https://merchant.example/notify",
  "subject": "测试商品",
  "body": "订单说明",
  "language": "zh-TW"
}
```

## Response

`R<CreateOrderResponse>` — 结构不变；`payUrl` 仍指向收银台入口。

## 服务端行为

1. `language` 经 `DisplayLocale.normalize()` → 非法回 `zh-CN`。
2. 持久化至 `cashier_orders.display_language`。
3. 不在 `payUrl` 附加 `lang` 查询参数（语言由订单数据驱动）。

## 错误码

沿用现有下单错误码；`language` 非法**不**单独报错。
