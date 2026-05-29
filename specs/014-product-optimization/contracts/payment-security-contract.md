# Internal Contract: 支付回调安全与幂等

**Created**: 2026-05-29  
**Feature**: [../spec.md](../spec.md)

适用于 `payflow-cashier-server` 回调入口与 `payflow-payment-wechat` / `alipay` / `union` 模块。

## 1) 微信 v3 回调验签

**入口**: `POST /notify/wechat`（及等价路径）

**处理顺序**（强制）:

```text
1. 读取 Headers: Wechatpay-Signature, Wechatpay-Timestamp, Wechatpay-Nonce, Wechatpay-Serial
2. 校验 Timestamp 在 ±300s
3. RSA 验签（平台证书公钥，按 Serial 匹配 channelConfig）
4. AES-GCM 解密 resource
5. 业务处理（幂等）
```

| 步骤失败 | HTTP | 日志 |
|----------|------|------|
| 验签/时间戳 | 4xx/5xx（按渠道规范） | `warn` + metric `pay.notify.verify.fail` |
| 解密 | 5xx | `error`（body 截断） |
| 幂等命中 | 200 success（幂等成功语义） | `info` |

**缺平台证书**: 拒绝处理 + 告警 `pay.notify.cert.missing`。

## 2) 支付宝 / 银联（保持并作为基准）

- 支付宝：RSA2 验签（已实现，回归测试覆盖）。
- 银联：RSA-SHA256 验签（已实现）。

## 3) 支付成功幂等

**DB 条件更新**:

```sql
UPDATE cashier_payments
SET status = 'SUCCESS', ...
WHERE payment_id = ? AND status = 'PROCESSING'
```

**Redis 去重**（通知副作用）:

| Key | TTL |
|-----|-----|
| `notify:dedup:{paymentId}:PAYMENT_SUCCESS` | 86400s |

**规则**:
- `affectedRows==0` 且 payment 已为 SUCCESS → 跳过 Webhook/MQ，返回成功。
- `paidImmediately` 路径在发通知前必须先写 dedup key。

## 4) channelConfig 加密（cashier）

| 项 | 值 |
|----|-----|
| 算法 | AES-256-GCM（`AesEncryptor`） |
| 主密钥 | `payflow.crypto.master-key` 环境变量 |
| TypeHandler | `EncryptedStringTypeHandler` |

**禁止**: 日志打印完整 `channelConfig` 解密内容。

## 5) 主动查单（统一 SPI）

**接口**: `PayChannelPaymentOpenService.queryOrder(QueryContext)`

| 渠道 | 实现类 |
|------|--------|
| WECHAT_* | `WxPay*OpenService` |
| ALIPAY_* | `AliPay*OpenService` |
| UNION_* | `UnionPay*OpenService` |

**禁止**: Service 层直接注入 `WxPayNativeHandler` 等 Handler。

## 6) 超时关单前置查单

```text
OrderTimeoutTask / OrderMqConsumer:
  IF 本地状态 == PAYING/PROCESSING
    THEN channel.queryOrder()
    IF 渠道已支付 → 走 PayNotifyService 补单
    ELSE IF 确认未支付 → 关单
```

**禁止**: `checkPaymentWithChannel` 恒返回 `false`。
