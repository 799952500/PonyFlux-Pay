# API Contracts: 商户级风控配置

所有响应保持统一结构：

```json
{ "code": 0, "message": "success", "data": {} }
```

错误响应同样使用统一结构，`code` 为非 0，`message` 为面向用户的中文说明，`data` 可为空或包含安全的错误上下文。

## Common Types

### RiskRuleVO

```json
{
  "id": "1001",
  "ruleCode": "RISK_AMT_SINGLE",
  "ruleName": "单笔限额",
  "ruleType": "AMOUNT_SINGLE",
  "thresholdFen": 500000,
  "unit": "CNY_FEN",
  "action": "REJECT",
  "enabled": true,
  "priority": 10,
  "ownerType": "PLATFORM",
  "ownerMerchantId": null,
  "ownerMerchantName": null,
  "scopeType": "ALL_MERCHANTS",
  "scopeMerchantCount": 0,
  "description": "单笔超过 5000 元拒绝",
  "createdAt": "2026-05-20T10:00:00",
  "updatedAt": "2026-05-20T10:00:00"
}
```

### RiskRuleUpsertRequest

```json
{
  "ruleCode": "RISK_AMT_SINGLE_M001",
  "ruleName": "高风险商户单笔限额",
  "ruleType": "AMOUNT_SINGLE",
  "riskExpr": null,
  "thresholdFen": 500000,
  "unit": "CNY_FEN",
  "action": "REJECT",
  "enabled": true,
  "priority": 10,
  "ownerType": "PLATFORM",
  "scopeType": "SELECTED_MERCHANTS",
  "scopeMerchantIds": ["M001", "M002"],
  "description": "指定商户单笔超过 5000 元拒绝"
}
```

### RiskHitRecordVO

```json
{
  "id": "9001",
  "traceId": "trace-abc",
  "merchantId": "M001",
  "merchantName": "示例商户",
  "orderId": null,
  "merchantOrderNo": "MO202605200001",
  "ruleId": "1001",
  "ruleCode": "RISK_AMT_SINGLE",
  "ruleName": "单笔限额",
  "ownerType": "PLATFORM",
  "scopeType": "ALL_MERCHANTS",
  "action": "REJECT",
  "decision": "REJECTED",
  "hitReason": "单笔金额超过阈值",
  "requestSummary": "amountFen=600000, channel=WECHAT_PAY, clientIp=10.0.*.*",
  "createdAt": "2026-05-20T10:05:00"
}
```

## Admin Risk APIs

### GET /api/v1/admin/risk/rules

管理员查询全部风控规则，支持分页和筛选。

**Query Parameters**:
- `page`: 页码，默认 1。
- `pageSize`: 每页数量，默认 20，最大 100。
- `merchantId`: 按归属商户或适用商户筛选。
- `ownerType`: `PLATFORM` / `MERCHANT`。
- `scopeType`: `ALL_MERCHANTS` / `SELECTED_MERCHANTS` / `OWNER_MERCHANT_ONLY`。
- `ruleType`: 规则类型。
- `enabled`: true / false。
- `keyword`: 规则编码或名称关键字。

**Response data**:

```json
{
  "list": [RiskRuleVO],
  "total": 1,
  "page": 1,
  "pageSize": 20
}
```

### POST /api/v1/admin/risk/rules

管理员创建平台规则。

**Rules**:
- `ownerType` 必须为 `PLATFORM` 或由服务端固定为 `PLATFORM`。
- `scopeType=SELECTED_MERCHANTS` 时 `scopeMerchantIds` 必须非空。
- 启用前必须通过规则条件校验。

**Request body**: `RiskRuleUpsertRequest`

**Response data**: `RiskRuleVO`

### PUT /api/v1/admin/risk/rules/{ruleId}

管理员编辑平台规则，或在明确审计下干预商户自建规则。

**Request body**: `RiskRuleUpsertRequest`

**Response data**: `RiskRuleVO`

### PUT /api/v1/admin/risk/rules/{ruleId}/status

管理员启用或停用规则。

**Request body**:

```json
{ "enabled": true }
```

**Response data**: `RiskRuleVO`

### GET /api/v1/admin/risk/rules/{ruleId}/scopes

查询平台定向规则的商户作用范围。

**Response data**:

```json
{
  "ruleId": "1001",
  "scopeType": "SELECTED_MERCHANTS",
  "merchants": [
    { "merchantId": "M001", "merchantName": "示例商户", "enabled": true }
  ]
}
```

### PUT /api/v1/admin/risk/rules/{ruleId}/scopes

替换平台定向规则的商户作用范围。

**Request body**:

```json
{ "scopeMerchantIds": ["M001", "M002"] }
```

**Response data**: 同 GET scopes。

### GET /api/v1/admin/risk/hits

管理员查询全部风控命中记录。

**Query Parameters**:
- `page`, `pageSize`
- `merchantId`
- `ruleId`
- `ownerType`
- `decision`
- `startTime`, `endTime`

**Response data**:

```json
{
  "list": [RiskHitRecordVO],
  "total": 1,
  "page": 1,
  "pageSize": 20
}
```

### GET /api/v1/admin/risk/audits

管理员查询风控规则变更审计。

**Query Parameters**:
- `page`, `pageSize`
- `ruleId`
- `operatorType`
- `merchantId`
- `operationType`
- `startTime`, `endTime`

## Merchant Risk APIs

商户端接口必须从认证上下文获取当前商户，不接受请求体或查询参数覆盖当前商户身份。

### GET /api/v1/merchant/risk/rules

商户查询自己的自建规则，以及可选展示影响自己的平台规则。

**Query Parameters**:
- `includePlatform`: 是否包含对当前商户生效的平台规则，默认 true。
- `page`, `pageSize`
- `ruleType`
- `enabled`

**Response data**:

```json
{
  "list": [RiskRuleVO],
  "total": 1,
  "page": 1,
  "pageSize": 20
}
```

### POST /api/v1/merchant/risk/rules

商户创建自建规则。

**Rules**:
- 服务端固定 `ownerType=MERCHANT`。
- 服务端固定 `ownerMerchantId=当前商户`。
- 服务端固定 `scopeType=OWNER_MERCHANT_ONLY`。
- 请求体中的 `ownerType`、`ownerMerchantId`、`scopeType`、`scopeMerchantIds` 如存在必须忽略或拒绝。

**Request body**:

```json
{
  "ruleCode": "MERCHANT_SINGLE_LIMIT",
  "ruleName": "单笔限额",
  "ruleType": "AMOUNT_SINGLE",
  "thresholdFen": 100000,
  "unit": "CNY_FEN",
  "action": "REJECT",
  "enabled": true,
  "priority": 100,
  "description": "单笔超过 1000 元拒绝"
}
```

**Response data**: `RiskRuleVO`

### PUT /api/v1/merchant/risk/rules/{ruleId}

商户编辑自己的自建规则。

**Rules**:
- 如果 `ruleId` 不属于当前商户，返回无权限或不存在。
- 商户不能修改平台规则。

**Response data**: `RiskRuleVO`

### PUT /api/v1/merchant/risk/rules/{ruleId}/status

商户启用或停用自己的自建规则。

**Request body**:

```json
{ "enabled": false }
```

**Response data**: `RiskRuleVO`

### GET /api/v1/merchant/risk/hits

商户查询自己支付请求产生的风控命中记录。

**Query Parameters**:
- `page`, `pageSize`
- `ruleId`
- `decision`
- `startTime`, `endTime`

**Response data**:

```json
{
  "list": [RiskHitRecordVO],
  "total": 1,
  "page": 1,
  "pageSize": 20
}
```

## Payment Flow Contract

### 风控拦截错误

当支付请求被风控拒绝时，订单创建接口返回统一错误结构：

```json
{
  "code": 6101,
  "message": "支付请求未通过风控校验，请调整后重试或联系平台客服",
  "data": {
    "traceId": "trace-abc",
    "decision": "REJECTED"
  }
}
```

**Security Requirement**: 返回信息不得包含完整规则表达式、完整阈值策略组合、手机号、银行卡、密钥等敏感信息。
