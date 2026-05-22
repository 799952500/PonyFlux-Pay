# 商户隔离入口清单

**Feature**: 008-merchant-data-isolation  
**Purpose**: 建立商户级数据、全局配置、系统审计和待人工确认入口的治理基线，供后续授权范围、隔离检查和验收使用。

> **实现备注（2026-05-21）**：US1/US2/US3 代码已落地；`admin_data_isolation_check` 种子与 `POST /admin/data-isolation/checks/scan` 可刷新检查项；历史 `recon_task`/`recon_diff` 缺 `merchant_id` 的记录仍标记为 `MANUAL_REVIEW`，需人工确认后整改。

## 分类规则

| 分类 | 判定标准 | 商户管理员可见性 | 系统管理员可见性 |
|------|----------|------------------|------------------|
| 商户级 | 数据能归属到唯一商户，或操作会影响单一商户资源 | 仅授权商户范围内可见和可操作 | 可跨商户筛选并展示归属 |
| 全局级 | 平台公共定义、字典、菜单模板、基础渠道等不绑定单一商户 | 只展示允许共享信息或脱敏摘要 | 可维护全局配置 |
| 系统审计 | 用于平台运维、审计、拒绝记录或系统任务追踪 | 仅展示自身授权范围相关审计摘要 | 可按商户、资源、结果定位 |
| 待人工确认 | 缺少 merchant_id 或无法自动推断归属 | 普通商户管理员不可见 | 可治理、补归属、豁免或限制 |

## 后台管理入口

| 入口 | 主要文件/接口 | 数据分类 | 当前归属字段/来源 | 隔离要求 | 初始状态 |
|------|---------------|----------|-------------------|----------|----------|
| 订单列表/详情/关闭 | `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminOrderController.java` | 商户级 | `cashier_orders.merchant_id` | 服务端按授权商户范围过滤；详情和关闭不得泄露授权外订单是否存在 | 待整改 |
| 订单导出 | `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminOrderController.java` | 商户级 | `cashier_orders.merchant_id` | 导出条件只能缩小授权范围；CSV 不包含授权外订单 | 待整改 |
| 支付查询 | `payflow-admin-server/src/main/java/com/payflow/admin/service/PaymentService.java` | 商户级 | 通过 `cashier_payments.order_id -> cashier_orders.merchant_id` 推断 | 查询必须 Join 或子查询限定授权商户 | 待整改 |
| 退款列表/审核 | `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminRefundController.java` | 商户级 | 通过 `cashier_refunds.order_id -> cashier_orders.merchant_id` 推断 | 审核通过/拒绝前校验退款所属商户 | 待整改 |
| 渠道账号池 | `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminChannelAccountController.java` | 商户级敏感配置 | 通过商户路由/绑定关系推断 | 商户管理员仅能查看自身商户可用账号的脱敏摘要；系统管理员可治理 | 待整改 |
| 基础渠道 | `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminChannelController.java` | 全局级 | 不适用 | 平台公共定义可共享，敏感密钥不可向商户管理员明文输出 | 待确认 |
| 支付方式公共定义 | `payflow-admin-server/src/main/java/com/payflow/admin/controller/PaymentMethodController.java` | 全局级 | 不适用 | 公共定义共享；商户可用性由商户支付方式配置决定 | 待确认 |
| 商户支付方式 | `payflow-admin-server/src/main/java/com/payflow/admin/controller/MerchantPaymentMethodController.java` | 商户级 | `admin_merchant_payment_methods.merchant_id` | 查询、保存、批量修改必须限制授权商户 | 待整改 |
| 商户支付路由 | `payflow-admin-server/src/main/java/com/payflow/admin/controller/MerchantPaymentRouteController.java` | 商户级 | `admin_merchant_payment_routes.merchant_id` | 路由查询和维护必须限制授权商户 | 待整改 |
| 渠道路由 | `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminChannelRouteController.java` | 商户级 | `admin_channel_routes.merchant_id` | 查询和维护必须限制授权商户 | 待整改 |
| 对账任务 | `payflow-admin-server/src/main/java/com/payflow/admin/service/AdminReconQueryService.java` | 待人工确认 | `recon_task.merchant_id` 已加列；历史任务可能为空 | 新任务/商户子任务已写归属；空归属任务由扫描标记 `NEEDS_MANUAL_REVIEW` | 部分已整改 |
| 商户对账子任务 | `payflow-admin-server/src/main/java/com/payflow/admin/service/AdminReconQueryService.java` | 商户级 | `recon_merchant_task.merchant_id` | 按授权商户过滤 | 待整改 |
| 对账账单明细 | `recon_bill_record` | 待人工确认 | 当前缺少直接 `merchant_id`，可由 `task_id` 或订单号推断 | 归属明确前不向商户管理员展示 | 待人工确认 |
| 对账差异 | `recon_diff` | 待人工确认 | 当前缺少直接 `merchant_id`，可由 `task_id` 或 `local_order_id` 推断 | 差异处理前必须校验商户归属 | 待人工确认 |
| 安全审计 | `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminSecurityAuditController.java` | 系统审计 | `cashier_security_audit.merchant_id` / `target_merchant_id` | 商户管理员仅看自身相关拒绝摘要；系统管理员可跨商户筛选 | 待整改 |
| 操作日志 | `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminAuditLogController.java` | 系统审计 | 当前 `admin_audit_logs` 缺少结构化商户字段 | 后续补充 `merchant_id`、资源类型、结果、拒绝原因 | 待整改 |
| 风控规则 | `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminRiskController.java` | 商户级/全局级 | `owner_type`、`owner_merchant_id`、`scope_type`、`admin_risk_rule_merchant_scope.merchant_id` | 平台规则按作用范围展示；商户自建规则限定所属商户 | 待整改 |
| 风控命中记录 | `admin_risk_hit_record` | 商户级 | `merchant_id` | 按授权商户过滤 | 待整改 |
| 仪表盘聚合 | `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminDashboardController.java` | 商户级聚合/全局聚合 | 明细需由订单/支付归属推断，预聚合当前缺少单商户维度 | 商户管理员仅统计授权商户；系统管理员可跨商户 | 待整改 |
| 流失预警 | `admin_churn_alert` | 商户级 | `merchant_id` | 按授权商户过滤 | 待整改 |
| 费率配置 | `admin_fee_rate_config` | 全局级/商户级 | `scope_type`、`scope_value` | global/merchant_group 可共享；merchant 范围需按授权商户限制 | 待整改 |
| 费率审计 | `admin_fee_rate_audit_log` | 商户级审计 | `merchant_id` | 按授权商户过滤 | 待整改 |
| 路由决策日志 | `recon_routing_decision_log` | 商户级 | `merchant_id` | 按授权商户过滤 | 待整改 |
| 商户管理 | `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminMerchantController.java` | 商户级主体 | `admin_merchants.merchant_id` | 商户管理员仅看自身商户摘要；系统管理员可全局治理 | 待整改 |
| 全局搜索 | `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminSearchController.java` | 混合 | 由命中的资源决定 | 每类结果必须独立应用授权范围 | 待整改 |
| 系统配置 | `payflow-admin-server/src/main/java/com/payflow/admin/controller/SystemConfigController.java` | 全局级/商户级敏感 | `config_key` 与内容判定 | 平台公共配置共享；商户专属密钥、回调、费率转为商户级 | 待确认 |
| 角色/菜单/用户 | `SysRoleController` / `SysMenuController` / `SysUserController` | 系统审计/全局级 | 不适用或用户授权范围 | 商户管理员不得扩大自身菜单和数据范围 | 待整改 |
| 数据隔离治理 | `DataIsolationCheckController` | 系统治理 | `admin_data_isolation_checks` | 系统管理员查看和触发扫描；商户管理员不可扩大范围 | 待新增 |

## 收银台与支付链路入口

| 入口 | 主要文件/接口 | 数据分类 | 当前归属字段/来源 | 隔离要求 | 初始状态 |
|------|---------------|----------|-------------------|----------|----------|
| 商户订单创建/查询 | `payflow-cashier-server/src/main/java/com/payflow/cashier/service/ResourceOwnershipService.java` | 商户级 | `cashier_orders.merchant_id` | 继续由认证上下文绑定 `merchant_id`，详情必须校验资源归属 | 已有基线 |
| 支付创建 | `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/PaymentServiceImpl.java` | 商户级 | `cashier_orders.merchant_id` | 支付链路必须使用订单所属商户，不信任请求方传入其他商户 | 已有基线 |
| 退款创建/查询 | `payflow-cashier-server/src/main/java/com/payflow/cashier/service/impl/RefundServiceImpl.java` | 商户级 | `cashier_refunds.order_id -> cashier_orders.merchant_id` | 创建和查询必须校验支付/订单属于当前商户 | 已有基线 |
| 支付回调/补单 | `payflow-cashier-server/src/main/java/com/payflow/cashier/service/PayNotifyService.java` | 系统任务 + 商户级结果 | 由订单/支付记录推断 | 系统模式处理，但写入结果必须保持原订单商户归属 | 待复核 |
| Webhook 投递 | `merchant_webhook_endpoint` / `webhook_delivery_log` | 商户级 | `merchant_id` | 仅投递到同商户端点，日志按商户隔离 | 待复核 |
| 安全审计 | `cashier_security_audit` | 系统审计 | `merchant_id` / `target_merchant_id` | 跨商户拒绝记录内部原因，对外不泄露目标资源 | 已有基线 |
| 收银台页/支付状态轮询 | `/cashier/{orderId}` / `/payments/status/{paymentId}` | 白名单入口 | 由公开收银链路访问 | 不用于后台商户运维；需避免输出其他商户敏感信息 | 已豁免候选 |

## 对账服务入口

| 入口 | 主要文件/接口 | 数据分类 | 当前归属字段/来源 | 隔离要求 | 初始状态 |
|------|---------------|----------|-------------------|----------|----------|
| 对账任务生成 | `payflow-recon-server/src/main/java/com/payflow/recon/service/ReconTaskSeedService.java` | 待人工确认 | 当前任务按渠道账户生成，缺少直接商户字段 | 后续生成任务时写入可追溯商户归属或生成商户子任务 | 待整改 |
| 对账比对 | `payflow-recon-server/src/main/java/com/payflow/recon/service/ReconCompareService.java` | 商户级/待人工确认 | 可由本地支付订单推断 | 比对记录和差异需保持支付记录商户归属一致 | 待整改 |
| 差异处理审计 | `payflow-recon-server/src/main/java/com/payflow/recon/service/ReconDiffHealService.java` | 商户级审计 | 当前 `recon_handler_audit` 缺少直接商户字段 | 差异处理记录必须写入商户归属 | 待整改 |

## 前端页面入口

| 页面 | 文件 | 数据分类 | 隔离要求 | 初始状态 |
|------|------|----------|----------|----------|
| 管理后台 Store | `payflow-admin-client/src/stores/admin.ts` | 授权上下文 | 登录后保存 `authorizedMerchantIds` 和 `platformAdmin` | 待整改 |
| 订单列表 | `payflow-admin-client/src/pages/admin/orders/index.vue` | 商户级 | 商户管理员自动限定范围，不能手工扩大 merchantId | 待整改 |
| 订单详情 | `payflow-admin-client/src/pages/admin/orders/detail.vue` | 商户级 | 授权外详情隐藏并显示通用拒绝 | 待整改 |
| 退款管理 | `payflow-admin-client/src/pages/admin/refunds.vue` | 商户级 | 列表和审核操作受授权范围限制 | 待整改 |
| 支付账号 | `payflow-admin-client/src/pages/admin/payment-accounts.vue` | 商户级敏感配置 | 商户管理员仅看脱敏摘要 | 待整改 |
| 渠道路由 | `payflow-admin-client/src/pages/admin/channel-routes.vue` | 商户级 | 自动限定授权商户 | 待整改 |
| 对账页面 | `payflow-admin-client/src/pages/admin/reconcile/index.vue` | 商户级/待人工确认 | 待确认记录不向商户管理员展示 | 待整改 |
| 风控页面 | `payflow-admin-client/src/pages/admin/risk.vue` | 商户级/全局级 | 商户规则和平台规则分层展示 | 待整改 |
| 仪表盘 | `payflow-admin-client/src/pages/admin/dashboard.vue` | 商户级聚合/全局聚合 | 商户管理员统计授权商户，系统管理员可筛选 | 待整改 |
| 数据隔离治理 | `payflow-admin-client/src/pages/admin/data-isolation.vue` | 系统治理 | 系统管理员查看检查项、扫描、整改状态 | 待新增 |
| 系统配置 | `payflow-admin-client/src/pages/admin/settings.vue` | 全局级/敏感配置 | 显示全局配置标识和脱敏摘要 | 待整改 |
| 审计日志 | `payflow-admin-client/src/pages/admin/audit-logs.vue` | 系统审计 | 增加商户归属筛选与展示 | 待整改 |

## 初始豁免候选

| 目标 | 分类 | 豁免理由 | 复核要求 |
|------|------|----------|----------|
| 基础渠道定义 `admin_channels` | 全局级 | 平台统一渠道类型和展示信息，不属于单一商户 | 不输出未脱敏 API Key |
| 支付方式公共定义 `admin_payment_methods` | 全局级 | 作为平台可用支付方式字典，商户可用性由绑定表控制 | 商户专属配置不得放在公共定义中 |
| 系统菜单 `sys_menus` | 全局级 | 平台统一菜单模板 | 菜单授权不能替代数据授权 |
| 系统角色 `sys_roles` | 全局级 | 平台角色定义 | 用户授权范围需另行限制 |
| 公共字典 `/admin/dicts` | 全局级 | 平台枚举和展示字典 | 不包含商户专属敏感值 |
| 收银台页 `/api/v1/cashier/{orderId}` | 白名单 | 面向消费者完成支付，不属于后台商户运维入口 | 只展示支付所需信息，避免商户敏感配置 |
| 支付状态轮询 `/api/v1/payments/status/{paymentId}` | 白名单 | 消费者支付结果轮询 | 仅返回支付状态，不返回其他商户敏感字段 |
| 渠道回调 `/notify/**` | 系统任务 | 由渠道签名校验驱动，不由商户管理员直接访问 | 回写必须保持原订单商户归属 |

## 风险优先级

1. **高风险**：订单、支付、退款、渠道账号、商户路由、导出、批量操作、对账差异处理。
2. **中风险**：风控、费率、仪表盘、流失预警、路由日志、审计日志。
3. **低风险**：公共字典、基础渠道、菜单、角色、系统版本信息。
4. **待人工确认**：缺少直接 `merchant_id` 的对账任务、账单明细、差异、部分预聚合指标。
