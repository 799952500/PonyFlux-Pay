# Data Model: 银联支付渠道完整接入

**Feature**: 002-unionpay-channel
**Date**: 2026-05-12

## Entity Changes Summary

| Entity | Type | Description |
|--------|------|-------------|
| `PayMethod.UNION_QR` | 新增枚举值 | 银联扫码支付方式编码 |
| `UnionPayAccountConfig` | 新增 POJO | 银联商户配置（merId, 证书, 密钥） |
| `Payment` (cashier_payments) | 复用 | pay_method 字段扩展：支持 "UNION_H5" / "UNION_QR" |
| `Refund` (cashier_refunds) | 复用 | 无 schema 变更 |
| `ReconBillRecord` | 复用 | channel_code = "unionpay" |
| `ReconDiff` | 复用 | channel_code = "unionpay" |

---

## 1. PayMethod 枚举扩展

```java
// 新增枚举值
UNION_QR("UNION_QR", "银联扫码支付");
```

**已有值**: `UNION_H5("UNION_H5", "银联云闪付H5")`（保留，修改注释去掉"SPI 占位"）

---

## 2. UnionPayAccountConfig (配置 POJO)

存储于 `cashier_channel_accounts.channel_config` JSON 字段中。

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `merId` | String | 是 | 银联商户号 |
| `signCertPath` | String | 是 | 签名证书文件路径 (.pfx) |
| `signCertPassword` | String | 是 | 签名证书密码 (AES-256-GCM 加密存储) |
| `encryptCertPath` | String | 否 | 加密证书文件路径 (.cer) |
| `encryptCertPassword` | String | 否 | 加密证书密码 |
| `gatewayUrl` | String | 是 | 银联网关地址 (沙箱或生产) |
| `notifyUrl` | String | 否 | 自定义回调地址 (覆盖默认) |
| `unionPublicKeyPath` | String | 否 | 银联公钥证书路径 (验签用) |

**验证规则**:
- `merId` 不可为空
- `signCertPath` 必须指向存在的文件
- `gatewayUrl` 必须以 `https://` 开头

---

## 3. Payment (cashier_payments) 扩展

复用现有表结构，pay_method 字段新增两个可选值：

| pay_method 值 | 支付方式 |
|--------------|----------|
| `UNION_H5` | 银联 H5 支付 (已有) |
| `UNION_QR` | 银联扫码支付 (新增) |

**关联**: `pay_channel` = `UNION_PAY`

---

## 4. Refund (cashier_refunds)

无 schema 变更。`pay_channel` = `UNION_PAY`，退款状态机沿用现有模型：

```
CREATED → PROCESSING → SUCCESS
                     → FAILED (可重试)
```

---

## 5. ReconBillRecord (对账账单记录)

复用现有表结构。新增 channel_code 值：

| channel_code | bill_type | parser |
|-------------|-----------|--------|
| `unionpay` | `trade` | `unionpayBillParser` |
| `unionpay` | `settle` | `unionpayBillParser` (可选) |

**账单记录字段映射** (银联 CSV 列 → ReconBillRecord 字段):

| 银联字段 | ReconBillRecord 字段 |
|----------|---------------------|
| 交易流水号 (queryId) | `channel_trade_no` |
| 商户订单号 (orderId) | `out_trade_no` |
| 交易金额 (txnAmt) | `amount` (分) |
| 手续费 (fee) | `fee` (可选) |
| 交易时间 (txnTime) | `trade_time` |
| 交易类型 (txnType) | `trade_type` |
| 清算日期 (settleDate) | `settle_date` |

---

## 6. ReconDiff (对账差异)

复用现有表结构。channel_code = `unionpay`。

差异类型（与现有一致）:
- `CHANNEL_ONLY`: 银联有记录，本地无
- `LOCAL_ONLY`: 本地有记录，银联无
- `AMOUNT_MISMATCH`: 金额不一致
- `STATUS_MISMATCH`: 状态不一致

---

## 7. Seed Data (种子数据)

### payment_methods 新增记录

```sql
-- admin 库
INSERT INTO payment_methods (method_code, method_name, channel_id, enabled, created_at, updated_at)
SELECT 'UNION_QR', '银联扫码支付', id, 1, NOW(), NOW()
FROM channels WHERE channel_code = 'UNION_PAY';
```

### full-reseed 更新

更新 `sql/full-reseed-payflow-demo.sql`，新增 `UNION_QR` 的 INSERT 语句。

### channel_config JSON 示例

```json
{
  "merId": "777290058110048",
  "signCertPath": "/etc/payflow/certs/unionpay/sign.pfx",
  "signCertPassword": "encrypted:base64...",
  "gatewayUrl": "https://gateway.test.95516.com/gateway/api",
  "unionPublicKeyPath": "/etc/payflow/certs/unionpay/union_public.cer"
}
```
