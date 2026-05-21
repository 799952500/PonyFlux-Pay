# Data Model: 商户级风控配置

## Entity: RiskRule（风控规则）

**Purpose**: 表示一条可评估支付请求风险的规则，是配置管理和支付执行的核心实体。

**Fields**:
- `id`: 规则唯一标识，Long。
- `ruleCode`: 规则编码，平台内唯一，用于审计和排查。
- `ruleName`: 规则名称，必填，展示给管理员或商户。
- `ruleType`: 规则类型，枚举：`AMOUNT_SINGLE`、`AMOUNT_DAILY`、`IP_LIMIT`、`MOBILE_LIMIT`、`CUSTOM`。
- `riskExpr`: 自定义表达式，仅 `CUSTOM` 规则使用，需限制长度和可用变量。
- `thresholdFen`: 阈值，Long，单位为分或次数，按 `unit` 解释；金额类必须为分。
- `unit`: 单位，枚举：`CNY_FEN`、`TIMES_PER_HOUR`、`TIMES_PER_DAY`、`COUNT`。
- `action`: 命中动作，枚举：`REJECT`、`REVIEW`（如已有复核能力）、`WARN`（如已有告警能力）。本阶段核心为 `REJECT`。
- `enabled`: 是否启用，Boolean。
- `priority`: 优先级，Integer，数值越小越先评估。
- `ownerType`: 归属来源，枚举：`PLATFORM`、`MERCHANT`。
- `ownerMerchantId`: 归属商户，仅商户自建规则必填；平台规则为空。
- `scopeType`: 作用范围，枚举：`ALL_MERCHANTS`、`SELECTED_MERCHANTS`、`OWNER_MERCHANT_ONLY`。
- `description`: 规则描述，展示用途和业务说明。
- `createdBy`: 创建人标识。
- `updatedBy`: 最近更新人标识。
- `createdAt`: 创建时间。
- `updatedAt`: 更新时间。

**Relationships**:
- `RiskRule.ownerMerchantId` 关联一个商户，仅商户自建规则使用。
- `RiskRule` 为平台定向规则时，通过 `RiskRuleMerchantScope` 关联多个商户。
- `RiskRule` 可被多个 `RiskHitRecord` 引用。
- `RiskRule` 可被多个 `RiskRuleAuditLog` 引用。

**Validation Rules**:
- `ruleCode` 必须唯一，且新增后不建议修改。
- `ownerType=MERCHANT` 时，`ownerMerchantId` 必填，`scopeType` 必须为 `OWNER_MERCHANT_ONLY`。
- `ownerType=PLATFORM` 时，`ownerMerchantId` 必须为空，`scopeType` 只能为 `ALL_MERCHANTS` 或 `SELECTED_MERCHANTS`。
- `scopeType=SELECTED_MERCHANTS` 且启用时，至少存在一个有效商户范围。
- 金额类规则的 `thresholdFen` 必须大于 0，且不得使用浮点单位。
- `CUSTOM` 规则启用前必须有合法 `riskExpr` 或合法阈值条件。
- 商户用户创建或修改规则时，服务端必须忽略请求体中的 `ownerMerchantId` 和 `scopeType`，改用认证上下文派生。

**State Transitions**:
- `DRAFT`（可选）→ `ENABLED`: 校验完整性和权限后启用。
- `ENABLED` → `DISABLED`: 停用后不再参与后续支付评估。
- `DISABLED` → `ENABLED`: 重新校验条件和范围后启用。
- 删除建议采用逻辑删除或禁用；历史命中和审计不得级联删除。

## Entity: RiskRuleMerchantScope（规则商户作用范围）

**Purpose**: 表示平台定向规则适用的商户集合。

**Fields**:
- `id`: 范围记录唯一标识，Long。
- `ruleId`: 风控规则 ID，必填。
- `merchantId`: 适用商户 ID，必填。
- `enabled`: 范围记录是否有效，Boolean。
- `createdAt`: 创建时间。

**Relationships**:
- 多条 `RiskRuleMerchantScope` 归属于一条 `RiskRule`。
- 每条范围记录关联一个商户。

**Validation Rules**:
- 同一 `ruleId + merchantId` 只能存在一条有效记录。
- 仅 `ownerType=PLATFORM` 且 `scopeType=SELECTED_MERCHANTS` 的规则允许维护范围记录。
- 商户必须存在且状态允许配置。

## Entity: RiskHitRecord（风控命中记录）

**Purpose**: 表示一次支付请求被风控规则命中或拦截的可追踪记录。

**Fields**:
- `id`: 命中记录唯一标识，Long。
- `traceId`: 链路追踪标识。
- `merchantId`: 请求所属商户。
- `orderId`: 订单号，若拦截发生在订单创建前可为空。
- `merchantOrderNo`: 商户订单号。
- `ruleId`: 命中的风控规则 ID。
- `ruleCode`: 命中的规则编码快照。
- `ruleName`: 命中的规则名称快照。
- `ownerType`: 命中规则归属来源快照。
- `scopeType`: 命中规则作用范围快照。
- `action`: 命中动作。
- `decision`: 处理结果，枚举：`REJECTED`、`REVIEW_REQUIRED`、`WARN_ONLY`。
- `requestSummary`: 请求摘要，脱敏保存金额、渠道、IP 摘要、设备或手机号摘要等。
- `hitReason`: 命中原因摘要，不包含完整敏感规则表达式。
- `createdAt`: 命中时间。

**Relationships**:
- 引用 `RiskRule`，但保留规则快照字段以支持规则变更后的历史排查。
- 关联商户和支付请求。

**Validation Rules**:
- `merchantId`、`ruleId`、`decision`、`createdAt` 必填。
- `requestSummary` 必须脱敏，不保存密钥、完整手机号、完整银行卡号等敏感信息。
- 管理员可查询全部；商户用户查询时必须按认证上下文中的商户过滤。

## Entity: RiskRuleAuditLog（风控规则变更审计）

**Purpose**: 表示风控规则配置生命周期中的关键变更。

**Fields**:
- `id`: 审计记录唯一标识，Long。
- `ruleId`: 目标规则 ID。
- `operatorId`: 操作者 ID。
- `operatorName`: 操作者名称快照。
- `operatorType`: 操作者类型，枚举：`ADMIN`、`MERCHANT`、`SYSTEM`。
- `merchantId`: 操作者所属商户；管理员为空或记录代操作目标商户。
- `operationType`: 操作类型，枚举：`CREATE`、`UPDATE`、`ENABLE`、`DISABLE`、`SCOPE_CHANGE`、`DELETE`。
- `beforeSummary`: 变更前摘要。
- `afterSummary`: 变更后摘要。
- `clientIp`: 操作 IP，需按日志规范处理。
- `createdAt`: 操作时间。

**Relationships**:
- 多条审计记录归属于一条 `RiskRule`。

**Validation Rules**:
- 所有创建、编辑、启停、范围变更必须写审计。
- 商户用户的审计记录必须包含所属 `merchantId`。
- 管理员修改商户自建规则时必须标识为平台干预或代操作。

## Entity: Merchant（商户）

**Purpose**: 风控配置归属和支付请求归属主体。

**Fields used by this feature**:
- `merchantId`: 商户标识。
- `merchantName`: 商户名称。
- `status`: 商户状态。

**Validation Rules**:
- 平台定向范围只能选择存在且状态有效的商户。
- 商户状态异常时，其自建规则不得影响其他商户；是否继续影响自身由业务状态规则决定，默认禁用其自建规则评估。

## Entity: PaymentRiskContext（支付风控上下文）

**Purpose**: 支付请求进入风控评估时使用的运行时上下文，不直接暴露给前端。

**Fields**:
- `merchantId`: 请求商户。
- `merchantOrderNo`: 商户订单号。
- `amountFen`: 请求金额，Long 分。
- `currency`: 币种。
- `channel`: 支付渠道。
- `clientIp`: 请求 IP。
- `mobileHash`: 手机号摘要，如有。
- `deviceFingerprint`: 设备摘要，如有。
- `requestTime`: 请求时间。
- `traceId`: 链路追踪标识。

**Validation Rules**:
- `merchantId` 和 `amountFen` 必填。
- 金额必须为正整数分。
- 写入命中记录前必须生成脱敏摘要。
