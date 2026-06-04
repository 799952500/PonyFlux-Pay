# Contract: 内部收银台支付方式列表

**Service**: `payflow-admin-server`  
**Path**: `GET /api/v1/internal/cashier/payment-methods`  
**Auth**: `X-Payflow-Internal-Token`

## Query Parameters

| 参数 | 必填 | 说明 |
|------|------|------|
| `merchantId` | 是 | 商户号 |
| `orderChannel` | 是 | 订单渠道，如 `WECHAT_PAY` |
| `locale` | 否 | `zh-CN` \| `zh-TW` \| `en-US`，默认 `zh-CN` |

## Response `data`

`List<Map>`，每项：

| 键 | 类型 | 说明 |
|----|------|------|
| `methodCode` | string | 编码 |
| `methodName` | string | **按 locale 解析后**的单语言名称 |
| `description` | string | **按 locale 解析后**的单语言描述 |
| `priority` | int | 路由优先级 |
| `clientScopes` | string[] | 终端范围 |

## 调用方变更

`payflow-cashier-server` `AdminPaymentConfigClient.fetchPaymentMethods` 增加 `locale` 参数，URL 追加 `&locale=zh-TW`。

`OrderServiceImpl.resolvePaymentMethods` 传入 `order.getDisplayLanguage()`。
