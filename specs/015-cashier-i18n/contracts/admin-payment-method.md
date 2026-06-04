# Contract: 支付方式管理（多语言）

**Service**: `payflow-admin-server`  
**Base**: `/api/v1/admin/payment-methods`  
**Auth**: JWT + 权限 `payment_method:*`

## 资源字段（请求/响应 body）

在现有 `PaymentMethod` 上扩展：

| 字段 | 类型 | 必填（写） | 说明 |
|------|------|------------|------|
| `methodCode` | string | 是 | 方式编码 |
| `channelId` / `channelType` | — | 是 | 所属渠道（现有逻辑） |
| `methodNameZhCn` | string | 是 | 展示名-简体 |
| `methodNameZhTw` | string | 是 | 展示名-繁体 |
| `methodNameEn` | string | 是 | 展示名-英文 |
| `descriptionZhCn` | string | 是 | 描述-简体 |
| `descriptionZhTw` | string | 是 | 描述-繁体 |
| `descriptionEn` | string | 是 | 描述-英文 |
| `enabled` | boolean | 否 | 启用状态 |
| `configJson` | string | 否 | 扩展配置 |

JSON 序列化建议使用 **camelCase**（与现有 admin API 一致）；DB 列为 snake_case。

### 创建示例

```json
{
  "methodCode": "WECHAT_NATIVE",
  "channelType": "WECHAT",
  "methodNameZhCn": "微信支付（扫码）",
  "methodNameZhTw": "微信支付（掃碼）",
  "methodNameEn": "WeChat Pay (QR)",
  "descriptionZhCn": "使用微信扫码支付",
  "descriptionZhTw": "使用微信掃碼支付",
  "descriptionEn": "Pay by scanning with WeChat",
  "enabled": true
}
```

## 校验错误

任一多语言字段为空时：

```json
{
  "code": 4001,
  "message": "支付方式展示名与描述须填写简体中文、繁体中文、英文",
  "data": null
}
```

（具体错误码以实现阶段 `BizException` 常量为准。）

## 列表/详情响应

返回完整六语字段；列表表格默认展示 `methodNameZhCn`（或 `methodName` 同步列）。

## 兼容

- 写入时同步 `method_name` = `method_name_zh_cn`，`description` = `description_zh_cn`。
- 旧客户端若仍传 `methodName` 单字段：可映射到 `methodNameZhCn` 并拒绝保存（三语不全）或短期兼容仅更新简体列——**推荐仅支持新字段，强制三语表单**。
