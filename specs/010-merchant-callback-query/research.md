# Research: 商户回调记录查询

## 1. 与现有「渠道回调日志」的边界

**Decision**: 新增 `cashier_merchant_notify` / `cashier_merchant_notify_attempt` 两张表，不复用 `cashier_callback_logs`。

**Rationale**:
- `cashier_callback_logs` 记录支付机构 → 平台的入站通知（`channel`、`raw_request`、`sign_verified`），语义与「平台 → 商户」完全不同。
- 运营排障「商户订单状态不一致」时混用两张表易误导；规格 FR-012 要求业务语义分离。
- 商户回调需按 `notify_type`（支付/退款）、`merchant_id`、汇总状态筛选，字段模型与渠道日志差异大。

**Alternatives considered**:
- 扩展现有 `cashier_callback_logs` 增加 `direction` 字段 — 拒绝：历史数据、索引与 UI 筛选均混乱。
- 仅依赖 `cashier_orders.notify_status` — 拒绝：无每次出入参，无法支撑 FR-002/FR-004。

## 2. 汇总与明细的数据粒度

**Decision**: 汇总键为 `(order_id, notify_type)`；每条 HTTP 尝试写入明细表。

**Rationale**:
- 规格 Assumptions：同一订单可有支付通知与退款通知，需分类型汇总。
- `MerchantNotifyWorker` 通过 MQ 投递，支付与退款消息体均走同一 Worker（退款带 `refundId`）。
- `notify_type` 枚举：`PAYMENT`、`REFUND`（`refundId != null` 时判定为 REFUND）。

**Alternatives considered**:
- 严格一对一订单仅一条汇总 — 拒绝：退款通知会覆盖支付汇总语义。

## 3. 写入时机与事务边界

**Decision**: 在 `MerchantNotifyWorker.deliverOrScheduleRetry` 内，于 HTTP 调用前后同步持久化；写入失败记录错误日志但不吞掉投递异常（与现有 Worker 行为一致，优先保证回调继续执行）。

**Rationale**:
- 写入点集中，覆盖首次投递与延迟重试两条 MQ 消费路径。
- 无需新增 MQ 主题；减少分布式一致性问题。
- 未配置 `merchant_notify_url` 时仍创建/更新汇总为 `NOT_CONFIGURED`，满足 FR-011。

**Alternatives considered**:
- 异步落库 MQ — 拒绝：增加复杂度，排障实时性变差。
- AOP 拦截 HttpUtil — 拒绝：隐式、难测，不符合显式异常与清晰职责。

## 4. 读取与模块职责

**Decision**: 
- **写入**：`payflow-cashier-server`（交易域）。
- **查询 API + 菜单**：`payflow-admin-server` + `payflow-admin-client`（沿用 cashier 数据源读模型，模式同 `AdminOrderController`）。

**Rationale**:
- 宪法 I：回调投递在收银域；后台仅查询展示。
- admin-server 已具备 cashier 库双数据源与 `AdminRequestContext.merchantScope` 隔离能力。

**Alternatives considered**:
- cashier-server 暴露内部查询 API 供 admin 调用 — 拒绝：增加服务间认证与网络依赖，现有双库直连模式已成熟。

## 5. 敏感字段脱敏

**Decision**: 存储完整请求/响应 JSON；API 与前端展示时对 `sign` 及疑似密钥字段做掩码（如保留前后各 4 位）；复用 admin 侧 `GlobalResourceKit` 类掩码思路，在商户回调专用 DTO 转换层实现。

**Rationale**:
- 排障需要完整参数结构；展示层脱敏满足 FR-010/SC-005。
- 不在库内预脱敏，避免丢失排障信息（授权角色展开时仍脱敏）。

**Alternatives considered**:
- 库内仅存 hash — 拒绝：无法满足出入参比对需求。

## 6. 大报文与保留策略

**Decision**: 
- 请求/响应 TEXT 字段应用层上限 32KB，超出部分截断并标记 `truncated=true`。
- 保留期与订单数据一致，默认 ≥90 天；暂不实现自动归档任务（首版）。

**Rationale**:
- 防止异常商户响应撑爆行大小；规格 Edge Case 已要求截断/摘要。
- 与 Assumptions 对齐，计划阶段不引入额外运维组件。

## 7. 与订单表 `notify_status` 的关系

**Decision**: 首版保留 `cashier_orders.notify_status` / `notify_retry_count` 现有更新逻辑；汇总表为后台查询的权威展示源，列表同时展示订单状态与回调汇总状态（FR-013）。

**Rationale**:
- 避免破坏依赖订单字段的既有逻辑。
- 逐步迁移：后续可考虑仅写汇总表并由查询 join，不在本特性范围。

## 8. 成功判定规则

**Decision**: 沿用 `MerchantNotifyWorker.sendMerchantNotify` 现有判定（响应体包含 `success`/`SUCCESS`/`ok`/`OK`）；明细如实记录原始响应与判定结果。

**Rationale**: 规格 Assumptions 明确不改变业务规则，仅记录事实。

## 9. 菜单与权限

**Decision**: 在 `sys_menus`「交易与订单」分组下新增菜单 `merchant_notifies`，路径 `/admin/merchant-notifies`；权限码 `merchant_notify:view`，授予 FINANCE、ADMIN 角色；商户管理员按数据范围自动过滤。

**Rationale**: 与 seed 中 orders/refunds 同级；符合 FR-006/FR-009。

## 10. 首版范围裁剪

**Decision**: P3「订单页一键跳转」、手动重发、历史补录、导出 CSV 列入 Phase 2 / 后续迭代，tasks 阶段标注可选。

**Rationale**: 规格 Assumptions 已排除手动重发与历史补录；核心 P1/P2 优先交付。
