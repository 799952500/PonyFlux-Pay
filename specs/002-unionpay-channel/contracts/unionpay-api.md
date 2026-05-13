# UnionPay API Contracts

**Feature**: 002-unionpay-channel
**Date**: 2026-05-12

## 1. 对外暴露接口 (系统 → 前端/商户)

### 1.1 创建支付 (已有端点，新增 UNION_QR)

```
POST /api/v1/merchant/payment/create
```

**新增支持的参数组合**:

| payChannel | payMethod | 说明 |
|-----------|-----------|------|
| `UNION_PAY` | `UNION_H5` | H5 支付 (占位→正式) |
| `UNION_PAY` | `UNION_QR` | 扫码支付 (新增) |

**UNION_H5 响应** (action=REDIRECT):
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "action": "REDIRECT",
    "h5Url": "https://gateway.test.95516.com/gateway/api/frontTransReq.do?...",
    "channelTradeNo": "1224050100000001"
  }
}
```

**UNION_QR 响应** (action=QR_CODE):
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "action": "QR_CODE",
    "qrCode": "https://qr.95516.com/...",
    "qrExpireSeconds": 300,
    "channelTradeNo": "1224050100000002"
  }
}
```

### 1.2 退款 (已有端点，修复 channelCode 映射)

```
POST /api/v1/merchant/refund
```

**新增支持**:
- `payChannel`: `UNION_PAY`
- `refundAmount`: 全额或部分退款金额 (分)
- 渠道代码 `union_pay` 将被 `normalizeChannelCode()` 正确映射为 `unionpay`

### 1.3 回调通知 (已有路由，新增实现)

```
POST /notify/unionpay
Content-Type: application/x-www-form-urlencoded
```

**银联通知参数** (body):
| 参数 | 说明 |
|------|------|
| `merId` | 商户号 |
| `orderId` | 商户订单号 |
| `queryId` | 银联交易流水号 |
| `txnAmt` | 交易金额 (分) |
| `respCode` | 响应码 ("00" = 成功) |
| `respMsg` | 响应消息 |
| `signature` | RSA-SHA256 签名 |
| `signMethod` | 签名方法 ("01" = RSA-SHA256) |

**系统响应**:
- 验签成功 + respCode="00": 返回 HTTP 200，处理支付成功
- 验签失败: 返回 HTTP 200 (body 为空)，银联将重发通知

---

## 2. 银联网关调用 (系统 → 银联)

### 2.1 H5 支付下单

```
POST {gatewayUrl}/gateway/api/frontTransReq.do
Content-Type: application/x-www-form-urlencoded
```

**请求参数**:
| 参数 | 必填 | 说明 |
|------|------|------|
| `version` | 是 | "5.1.0" |
| `encoding` | 是 | "UTF-8" |
| `signMethod` | 是 | "01" (RSA-SHA256) |
| `txnType` | 是 | "01" (消费) |
| `txnSubType` | 是 | "01" |
| `bizType` | 是 | "000201" (H5) |
| `channelType` | 是 | "08" |
| `accessType` | 是 | "0" |
| `merId` | 是 | 商户号 |
| `orderId` | 是 | 商户订单号 |
| `txnAmt` | 是 | 交易金额 (分) |
| `txnTime` | 是 | 交易时间 (yyyyMMddHHmmss) |
| `frontUrl` | 是 | 支付完成前端跳转地址 |
| `backUrl` | 是 | 异步通知地址 |
| `signature` | 是 | RSA-SHA256 签名 |

**响应**: HTML 重定向页面（用户浏览器自动跳转至银联支付页）

### 2.2 QR 扫码下单

```
POST {gatewayUrl}/gateway/api/backTransReq.do
Content-Type: application/x-www-form-urlencoded
```

**请求参数**: 与 H5 类似，但 `bizType` = "000000" (默认), `txnSubType` = "07" (二维码)

**响应** (同步 JSON/键值对):
| 字段 | 说明 |
|------|------|
| `respCode` | 响应码 ("00" = 成功) |
| `respMsg` | 响应消息 |
| `queryId` | 银联交易流水号 |
| `qrCode` | 二维码内容 (URL) |

### 2.3 退款

```
POST {gatewayUrl}/gateway/api/backTransReq.do
Content-Type: application/x-www-form-urlencoded
```

**关键参数**: `txnType` = "04" (退货), 额外的 `origQryId` (原交易流水号)

### 2.4 账单下载

```
POST {gatewayUrl}/gateway/api/fileTransReq.do
```

**关键参数**: `txnType` = "76" (对账文件下载), `fileType` = "00" (商户账单), `settleDate` = "MMDD"

**响应**: ZIP 压缩的 CSV 文件内容
