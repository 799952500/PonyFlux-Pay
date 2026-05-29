# API Contract: 支付漏斗

**Base Path**: `/api/v1/admin/insights`
**Auth**: JWT Bearer Token（`JwtInterceptor`）
**Data Isolation**: 强制 `merchantScope` 过滤

---

## GET /insights/funnel

支付漏斗多阶段聚合统计。

**Query Parameters**:

| 参数 | 类型 | 必填 | 默认 | 说明 |
|------|------|------|------|------|
| `dateFrom` | LocalDate | 否 | 7 天前 | 开始日期（含），ISO 格式 `yyyy-MM-dd` |
| `dateTo` | LocalDate | 否 | 今天 | 结束日期（含） |
| `merchantId` | String | 否 | — | 商户号筛选（受 merchantScope 限制） |
| `channel` | String | 否 | — | 渠道筛选（`WECHAT_PAY` / `ALIPAY` / `UNION_PAY`） |

**Response** `200`:

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "dateFrom": "2026-05-20",
    "dateTo": "2026-05-26",
    "stages": [
      {
        "name": "CREATED",
        "count": 1200,
        "rate": null
      },
      {
        "name": "PAYING",
        "count": 980,
        "rate": 81.7
      },
      {
        "name": "PAID",
        "count": 876,
        "rate": 89.4
      }
    ],
    "overallConversionRate": 73.0,
    "lossBreakdown": [
      {
        "name": "FAILED",
        "count": 45,
        "percentage": 3.8
      },
      {
        "name": "CLOSED",
        "count": 210,
        "percentage": 17.5
      },
      {
        "name": "EXPIRED",
        "count": 69,
        "percentage": 5.8
      }
    ]
  }
}
```

**字段说明**:

| 字段 | 说明 |
|------|------|
| `stages[].rate` | 相邻阶段转化率 = `当前阶段 / 上一阶段 * 100`，第一阶段为 null |
| `overallConversionRate` | `PAID / CREATED * 100`，保留 1 位小数 |
| `lossBreakdown[].percentage` | `该流失类型 / CREATED * 100`，保留 1 位小数 |

**统计口径**:

| 阶段 | SQL WHERE 条件 |
|------|----------------|
| CREATED | `created_at BETWEEN dateFrom 00:00 AND dateTo 23:59:59` |
| PAYING | 上述 + `status IN ('PAYING','PAID','SUCCESS')` |
| PAID | 上述 + `status IN ('PAID','SUCCESS')` |
| FAILED | 上述 + `status = 'FAILED'` |
| CLOSED | 上述 + `status = 'CLOSED'` |
| EXPIRED | 上述 + `status = 'EXPIRED'` |

所有条件额外叠加 `merchantId` 和 `channel` 筛选（如指定）以及 `merchantScope` 隔离。

---

## 错误码

| 错误码 | 说明 |
|--------|------|
| 8010 | 无效的日期范围（dateFrom > dateTo） |
| 8011 | 日期范围过大（超过 366 天） |
| 8012 | 无权访问指定商户数据 |
