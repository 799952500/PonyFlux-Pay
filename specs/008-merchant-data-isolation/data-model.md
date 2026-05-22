# Data Model: 商户数据隔离治理

## 商户

**Purpose**: 平台业务主体，是商户级数据归属和授权范围的核心边界。

**Key Fields**:
- `merchantId`: 商户唯一标识
- `merchantName`: 商户名称
- `status`: 商户状态
- `merchantGroup`: 商户分组或业务分层
- `createdAt` / `updatedAt`: 创建和更新时间

**Relationships**:
- 一个商户可拥有多个商户管理员授权。
- 一个商户可拥有多条订单、支付、退款、渠道路由、渠道账号配置、风控规则、对账任务和审计记录。

**Validation Rules**:
- 商户级数据必须能追溯到唯一商户，或进入待人工确认状态。
- 停用或冻结商户的数据仍需保持归属，不得因状态变化丢失隔离边界。

## 授权主体

**Purpose**: 表示后台用户、商户管理员或系统管理员的访问身份与商户授权范围。

**Key Fields**:
- `userId`: 用户标识
- `username`: 登录名
- `role`: 角色类型
- `authorizedMerchantIds`: 可访问商户集合
- `platformPermission`: 是否具备平台级治理能力
- `status`: 账号状态

**Relationships**:
- 一个授权主体可被授予一个或多个商户范围。
- 系统管理员可查看全局配置和跨商户治理结果。

**Validation Rules**:
- 普通商户管理员不能通过请求参数扩大 `authorizedMerchantIds`。
- 禁用账号不得访问商户级或全局治理数据。

## 商户级数据资源

**Purpose**: 需要按商户隔离的数据抽象，覆盖订单、支付、退款、渠道账号、路由、商户配置、对账任务、差异处理、导出任务和审核任务等。

**Key Fields**:
- `resourceType`: 资源类别
- `resourceId`: 资源标识
- `merchantId`: 所属商户
- `businessKey`: 订单号、支付单号、退款单号等业务键
- `status`: 业务状态
- `sensitiveLevel`: 敏感级别
- `createdAt` / `updatedAt`: 时间信息

**Relationships**:
- 每条商户级资源必须关联一个商户。
- 可关联多个审计记录和隔离检查项。

**Validation Rules**:
- 创建时必须确定商户归属。
- 查询、详情、导出、统计、更新、删除、审核、批量和异步处理必须使用授权范围限制。
- 跨商户访问拒绝时不得泄露目标资源是否存在。

## 全局配置数据

**Purpose**: 对平台或多商户统一适用、不应强制绑定单一商户的数据。

**Key Fields**:
- `configType`: 配置类型
- `configKey`: 配置键
- `configValueSummary`: 脱敏后的配置摘要
- `scope`: 适用范围
- `status`: 配置状态
- `updatedBy`: 最近更新人

**Relationships**:
- 可被多个商户共同使用。
- 若配置包含商户专属密钥、账号、费率或回调信息，应转为商户级数据资源。

**Validation Rules**:
- 商户管理员只能查看权限允许的全局配置摘要或可用选项。
- 全局配置不得包含未脱敏的其他商户敏感信息。

## 数据隔离检查项

**Purpose**: 记录某类数据、功能入口或操作是否满足商户隔离要求。

**Key Fields**:
- `checkId`: 检查项标识
- `targetType`: 数据表、页面、接口、异步任务或导出任务
- `targetName`: 检查目标名称
- `classification`: 商户级 / 全局级 / 系统审计 / 待人工确认
- `merchantFieldStatus`: 已具备 / 缺失 / 不适用 / 待确认
- `riskLevel`: 高 / 中 / 低
- `affectedEntries`: 影响入口
- `remediationStatus`: 待处理 / 处理中 / 已完成 / 已豁免 / 需人工确认
- `decisionReason`: 分类或豁免理由

**Relationships**:
- 可关联商户级数据资源或全局配置数据。
- 可关联验收记录和操作审计记录。

**Validation Rules**:
- 所有纳入治理范围的目标都必须有检查项。
- `classification=待人工确认` 的目标在确认前不得向普通商户管理员开放。
- `remediationStatus=已豁免` 必须说明为什么不需要商户隔离。

## 操作审计记录

**Purpose**: 支持定位商户管理员和系统管理员对关键数据的访问、修改、审核、导出和拒绝结果。

**Key Fields**:
- `auditId`: 审计标识
- `operatorId`: 操作者
- `operatorType`: 系统管理员 / 商户管理员 / 系统任务
- `merchantId`: 操作涉及的商户；全局操作可为空或标记为平台级
- `resourceType`: 操作资源类别
- `resourceId`: 操作资源标识
- `action`: 查看 / 创建 / 更新 / 删除 / 审核 / 导出 / 拒绝访问
- `result`: 成功 / 失败 / 拒绝
- `occurredAt`: 操作时间
- `clientInfo`: 脱敏后的来源信息

**Relationships**:
- 可关联授权主体和商户级数据资源。
- 可作为数据隔离检查和验收的证据。

**Validation Rules**:
- 关键商户级操作必须记录商户归属和结果。
- 审计记录不得包含未脱敏密钥、密码、证书或完整敏感信息。

## 状态流转

### 数据隔离检查项

```text
待处理 -> 处理中 -> 已完成
待处理 -> 需人工确认 -> 处理中 -> 已完成
待处理 -> 已豁免
处理中 -> 需人工确认
```

**Rules**:
- 只有系统管理员或平台治理人员可将检查项标记为已豁免。
- 已豁免必须保留理由和适用范围。
- 需人工确认的检查项在确认前必须保持限制访问。

### 商户级数据资源可见性

```text
归属明确 -> 授权范围内可见
归属明确 -> 授权范围外拒绝
归属缺失 -> 普通商户管理员不可见
归属待确认 -> 普通商户管理员不可见
```
