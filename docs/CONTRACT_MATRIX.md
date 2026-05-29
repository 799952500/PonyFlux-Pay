# 前端 API 与后端路由对照表

约定：`payflow-admin-client` / `payflow-cashier-client` 的 `axios` **baseURL** 均为 `/api/v1`，下列路径为 **相对 baseURL** 的路径；完整 URL 形如 `http://host:port/api/v1` + 路径。

成功响应：后端多为 `{ code: 0, message: "success", data: ... }`，前端拦截器解包后调用方拿到 **data**。

---

## 管理后台（admin-client ↔ payflow-admin-server）

| 前端方法（参考） | HTTP | 路径 | 后端 Controller |
|------------------|------|------|-----------------|
| `getDashboardStats` | GET | `/admin/dashboard` | [`AdminDashboardController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminDashboardController.java) 根路径 |
| `getDashboardMetrics` | GET | `/admin/dashboard/metrics` | 同上 `GET /metrics`（预聚合指标，支持 granularity/dateFrom/dateTo/channelCode） |
| `getMerchantRanking` | GET | `/admin/dashboard/merchant-ranking` | 同上 `GET /merchant-ranking`（商户 Top 10 排行） |
| `getMerchantInsight` | GET | `/admin/dashboard/merchant/{merchantId}/insight` | 同上 `GET /merchant/{merchantId}/insight`（商户钻取详情） |
| `createExportTask` | POST | `/admin/export/report` | [`AdminExportController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminExportController.java) |
| `getExportTasks` | GET | `/admin/export/tasks` | 同上 |
| `getChurnAlerts` | GET | `/admin/churn-alerts` | [`ChurnAlertController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/ChurnAlertController.java) |
| `getChurnAlertDetail` | GET | `/admin/churn-alerts/{id}` | 同上 |
| `updateChurnAlertStatus` | PUT | `/admin/churn-alerts/{id}/status` | 同上 |
| `getFeeRates` | GET | `/admin/fee-rates` | [`FeeRateController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/FeeRateController.java) |
| `createFeeRate` | POST | `/admin/fee-rates` | 同上 |
| `updateFeeRate` | PUT | `/admin/fee-rates/{id}` | 同上 |
| `deleteFeeRate` | DELETE | `/admin/fee-rates/{id}` | 同上 |
| `getFeeRateAuditLog` | GET | `/admin/fee-rates/audit-log` | 同上 |
| `getMerchantFeeProgress` | GET | `/admin/merchant-fee/{merchantId}/progress` | [`MerchantFeeController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/MerchantFeeController.java) |
| `getMerchantFeeHistory` | GET | `/admin/merchant-fee/{merchantId}/history` | 同上 |
| `getRoutingLogs` | GET | `/admin/routing-logs` | [`RoutingLogController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/RoutingLogController.java) |
| `exportRoutingLogs` | GET | `/admin/routing-logs/export` | 同上 |
| `getOrders` | GET | `/admin/orders` | [`AdminOrderController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminOrderController.java) |
| `getOrderDetail` / `getOrderDetailFull` | GET | `/admin/orders/{orderId}` | 返回 `order` + `payments` |
| `createOrderRefundRequest` | POST | `/admin/orders/{orderId}/refund-requests` | 创建待审批退款（body: paymentId, refundAmount, reason） |
| `queryOrderPaymentChannel` | POST | `/admin/orders/{orderId}/payments/{paymentId}/query-channel?sync=` | 向支付机构查单；`sync=true` 时回写本地（当前仅微信） |
| `closeOrder` | POST | `/admin/orders/{orderId}/close` | 同上 |
| `listMerchantNotifies` | GET | `/admin/merchant-notifies` | [`AdminMerchantNotifyController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminMerchantNotifyController.java) |
| `getMerchantNotifyDetail` | GET | `/admin/merchant-notifies/{notifyId}` | 同上 |
| `getMerchantNotifyByOrder` | GET | `/admin/merchant-notifies/by-order/{orderId}` | 同上 |
| `getRefunds` | GET | `/admin/refunds` | [`AdminRefundController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminRefundController.java) |
| `approveRefund` / `rejectRefund` | POST | `/admin/refunds/{refundId}/approve` / `reject` | 同上 |
| `getChannels` 等 | * | `/admin/channels` | [`AdminChannelController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminChannelController.java) |
| 渠道账号池 | * | `/admin/channels/accounts` | [`AdminChannelAccountController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminChannelAccountController.java) |
| 渠道路由 | * | `/admin/channels/routes` | [`AdminChannelRouteController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminChannelRouteController.java) |
| `getMerchants` 等 | * | `/admin/merchants` | [`AdminMerchantController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminMerchantController.java) |
| 兼容路径 `/merchants` | * | `/merchants` | [`MerchantController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/MerchantController.java)（若仍存在则仅兼容旧客户端） |
| 商户支付方式 | * | `/admin/merchant-payment-methods` | [`MerchantPaymentMethodController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/MerchantPaymentMethodController.java) |
| 商户支付路由 | * | `/admin/merchant-payment-routes` | [`MerchantPaymentRouteController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/MerchantPaymentRouteController.java) |
| `getRiskRules` / `createRiskRule` / `updateRiskRule` / `updateRiskRuleStatus` | GET/POST/PUT | `/admin/risk/rules`、`/admin/risk/rules/{ruleId}`、`/admin/risk/rules/{ruleId}/status` | [`AdminRiskController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminRiskController.java) |
| `getRiskRuleScopes` / `updateRiskRuleScopes` | GET/PUT | `/admin/risk/rules/{ruleId}/scopes` | 同上 |
| `getRiskHitRecords` / `getRiskAuditLogs` | GET | `/admin/risk/hits`、`/admin/risk/audits` | 同上 |
| `getMerchantRiskRules` / `createMerchantRiskRule` / `updateMerchantRiskRule` / `updateMerchantRiskRuleStatus` | GET/POST/PUT | `/merchant/risk/rules`、`/merchant/risk/rules/{ruleId}`、`/merchant/risk/rules/{ruleId}/status` | [`MerchantRiskController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/MerchantRiskController.java) |
| `getMerchantRiskHitRecords` | GET | `/merchant/risk/hits` | 同上 |
| 支付方式 | * | `/admin/payment-methods` | [`PaymentMethodController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/PaymentMethodController.java) |
| 删除依赖预检 | GET | `/admin/resource-dependencies` | [`ResourceDependencyController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/ResourceDependencyController.java) |
| 支付账号（旧路径） | * | `/admin/payment-accounts` | [`PaymentAccountController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/PaymentAccountController.java) |
| 角色/菜单/用户 | * | `/admin/roles`、`/admin/menus`、`/admin/users` | [`SysRoleController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/SysRoleController.java)、[`SysMenuController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/SysMenuController.java)、[`SysUserController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/SysUserController.java) |
| 系统配置 | * | `/admin/system-configs` | [`SystemConfigController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/SystemConfigController.java) |
| 登录 | POST | `/admin/auth/login` | [`AuthController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AuthController.java)；`data.permissions` 为扁平 `perm_code` 列表 |
| 刷新按钮权限 | GET | `/admin/auth/permissions` | 同上；`data.permissions` |
| 字典枚举 | GET | `/admin/dicts` | [`AdminDictController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminDictController.java) |
| 订单导出 CSV | GET | `/admin/orders/export` | [`AdminOrderController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminOrderController.java) |
| 全局搜索 | GET | `/admin/search` | [`AdminSearchController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminSearchController.java) |
| 通知摘要 | GET | `/admin/notifications/summary` | [`AdminNotificationController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminNotificationController.java) |
| 通知列表 | GET | `/admin/notifications` | [`AdminNotificationController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminNotificationController.java) |
| 通知未读数 | GET | `/admin/notifications/unread-count` | [`AdminNotificationController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminNotificationController.java) |
| 通知标记已读 | POST | `/admin/notifications/{id}/read` | [`AdminNotificationController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminNotificationController.java) |
| 通知全部已读 | POST | `/admin/notifications/read-all` | [`AdminNotificationController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminNotificationController.java) |
| 通知批量已读 | POST | `/admin/notifications/read-batch` | [`AdminNotificationController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminNotificationController.java) |
| 支付漏斗 | GET | `/admin/insights/funnel` | [`AdminInsightsController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminInsightsController.java) |
| 审计日志 | GET | `/admin/audit-logs` | [`AdminAuditLogController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminAuditLogController.java) |
| 安全审计（越权拒绝） | GET | `/admin/security/audit` | [`AdminSecurityAuditController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminSecurityAuditController.java)（`RISK` / `SUPER_ADMIN`） |
| 验证码 | GET | `/admin/auth/captcha` | [`AuthController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AuthController.java) |
| 是否需要验证码 | GET | `/admin/auth/captcha-required?username=` | 同上；`data.required` 为 true 时前端展示验证码；首次登录（该用户无密码失败记录）为 false |
| 版本信息 | GET | `/admin/meta/version` | [`AdminMetaController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminMetaController.java) |

JWT：除 `/admin/auth/login`、`/admin/auth/captcha`、`/admin/auth/captcha-required` 外，`/api/v1/admin/**` 需带 `Authorization: Bearer <token>`（见 [`JwtInterceptor`](payflow-admin-server/src/main/java/com/payflow/admin/interceptor/JwtInterceptor.java)）。

---

## 收银台客户端（cashier-client ↔ payflow-cashier-server）

| 前端方法 | HTTP | 路径 | 后端 |
|----------|------|------|------|
| `merchantLogin` | POST | `/auth/login` | [`MerchantAuthController`](payflow-cashier-server/src/main/java/com/payflow/cashier/controller/MerchantAuthController.java) |
| `getCashierInfo` | GET | `/cashier/{orderId}?sig=&client=` | 返回订单 + `paymentMethods`；`client=PC\|H5\|APP` 按商户路由 `client_scopes` 过滤；收银台经内部接口拉取管理端配置 |
| （内部）收银台支付方式 | GET | `/internal/cashier/payment-methods?merchantId=&orderChannel=` | [`InternalCashierPaymentController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/InternalCashierPaymentController.java) → 商户支付路由 + 平台支付方式 |
| `createPayment` | POST | `/payments` | [`PaymentController`](payflow-cashier-server/src/main/java/com/payflow/cashier/controller/PaymentController.java) |
| `pollPaymentStatus` | GET | `/payments/status/{paymentId}` | 同上（无需商户签名） |

退款、商户查询等走 `/api/v1/refunds`、`/api/v1/merchant/**`，需商户签名或 JWT，不在默认 cashier SPA 最小路径内。

**商户隔离（006）**：JWT/HMAC 认证后的 `/api/v1/orders/**`、`/api/v1/payments/**`（除 status 轮询）、`/api/v1/refunds/**`、`/api/v1/merchant/**`、`/api/v1/payment-links/**` 均经过 `merchantId` 绑定与资源所有权校验；跨商户访问统一 **HTTP 404 + code 5102**（不泄漏资源字段），`merchantId` 与认证不一致为 **HTTP 403 + code 5101**。

### 银联/云闪付回调

| 方法 | HTTP | 路径 | 后端 |
|------|------|------|------|
| `handleNotifyByChannel` | POST | `/notify/unionpay` | [`PayNotifyController`](payflow-cashier-server/src/main/java/com/payflow/cashier/controller/PayNotifyController.java) → `UnionPayOpenService` |
| 支付下单 `UNION_H5` | POST | `/payments` (body: `payMethod=UNION_H5`) | [`PaymentController`](payflow-cashier-server/src/main/java/com/payflow/cashier/controller/PaymentController.java) → `UnionPayPaymentOpenService` → `UnionH5Strategy` |
| 支付下单 `UNION_QR` | POST | `/payments` (body: `payMethod=UNION_QR`) | 同上 → `UnionQrStrategy` |
| 退款 | POST | `/refunds` | `RefundServiceImpl` → `UnionPayPaymentOpenService.refund()` |

---

## 冒烟清单（本地）

前置：MySQL（`payflow_admin`、`payflow_cashier`）、Redis（收银台缓存若启用）、Admin **3003**、Cashier **3002**；前端 dev proxy 指向对应端口。

### 管理端（需登录 Token）

- [ ] POST `/admin/auth/login`（首次免验证码；密码错误后再走验证码）
- [ ] GET `/admin/dashboard` — KPI 与图表数据非硬编码错误
- [ ] GET `/admin/dashboard/metrics` — 预聚合指标查询（按粒度/日期/渠道筛选）
- [ ] GET `/admin/dashboard/merchant-ranking` — 商户交易额 Top 10
- [ ] GET `/admin/dashboard/merchant/{merchantId}/insight` — 商户洞察详情
- [ ] GET `/admin/churn-alerts` — 流失预警列表
- [ ] PUT `/admin/churn-alerts/{id}/status` — 更新预警状态
- [ ] GET `/admin/fee-rates` — 阶梯费率规则列表
- [ ] POST `/admin/fee-rates` — 创建费率规则
- [ ] GET `/admin/fee-rates/audit-log` — 费率变更审计日志
- [ ] GET `/admin/merchant-fee/{merchantId}/progress` — 商户费率进度
- [ ] GET `/admin/routing-logs` — 路由决策日志列表
- [ ] POST `/admin/export/report` — 创建数据导出任务
- [ ] GET `/admin/notifications/summary` — 含流失预警超时统计
- [ ] GET `/admin/orders` 分页、`/admin/orders/{id}` 详情
- [ ] GET `/admin/refunds`、审批通过/拒绝（与收银台内部退款衔接）
- [ ] GET `/admin/merchants`、`/admin/channels`、`/admin/channels/accounts`
- [ ] GET `/admin/dicts`、`/admin/meta/version`、`/admin/search?q=`
- [ ] GET `/admin/orders/export` 下载 CSV（权限：`order:export`，见 `@RequirePermission`）
- [ ] GET `/admin/security/audit` — 安全审计（`RISK`/`SUPER_ADMIN`），筛选 merchantId/reasonCode

### 收银台客户端

- [ ] POST `/auth/login`
- [ ] GET `/cashier/{orderId}?sig=`、`POST /payments`、`GET /payments/status/{id}`

### 回归注意

- 响应 **`code === 0`** 表示业务成功；401 时前端应跳转登录页。

---

## Production Hardening (005) 契约变更

本节记录 `005-production-hardening` 分支中所有影响前后端契约的变更。

### 新增 API

| 前端方法 | HTTP | 路径 | 后端 Controller | 说明 |
|----------|------|------|-----------------|------|
| Webhook 端点管理 | CRUD | `/admin/webhook-endpoints` | （新增实体+Mapper，Controller 待定） | 管理商户 Webhook 回调 URL 及签名密钥 |
| 频道健康检查 | GET | `/actuator/health` | [`ChannelHealthIndicator`](payflow-cashier-server/src/main/java/com/payflow/cashier/metrics/ChannelHealthIndicator.java) | 支付渠道可用性健康指标 |
| 支付/退款计数 | GET | `/actuator/metrics` | [`PaymentMetrics`](payflow-cashier-server/src/main/java/com/payflow/cashier/metrics/PaymentMetrics.java) | Micrometer 自定义指标（payments/refunds success/failure count + duration） |

### 请求体 DTO 变更（前端需同步）

| Controller | 方法 | 变更前 | 变更后 |
|------------|------|--------|--------|
| `AdminMerchantController` | `PUT /{merchantId}` | `Map<String, Object>`（无校验） | `UpdateMerchantRequest` DTO（含 `@Size`、`@Pattern` 校验） |
| `AdminRiskController` | `PUT /rules/{ruleId}` | `Map<String, Object>`（无校验） | `UpdateRiskRuleRequest` DTO |
| `MerchantPaymentRouteController` | `POST /item`、`PUT /{id}` | `Map<String, Object>` | `PaymentRouteRequest` DTO（含 `@NotBlank`、`@NotNull`） |
| `MerchantPaymentMethodController` | `POST` (saveBatch) | `Map<String, Object>`（`paymentMethodIds` 为 `List<Number>`） | `SavePaymentMethodRequest` DTO（`paymentMethodIds` 为 `List<Long>`） |

> **前端适配提示**：DTO 校验失败时后端返回 400 + `{ "code": 1, "message": "校验失败详情..." }`，前端需处理新的错误格式。

### 按钮级权限（perm_code）

- 数据：`sys_menus.menu_type=BUTTON`，字段 `perm_code`、`api_pattern`；角色绑定仍用 `sys_role_menus`
- 后端：`@RequirePermission("perm:code")` + [`PermissionInterceptor`](payflow-admin-server/src/main/java/com/payflow/admin/interceptor/PermissionInterceptor.java)；`SUPER_ADMIN` JWT 角色放行
- 前端：`v-permission` / `usePermission()`；登录响应 `permissions[]`
- 字典与接入说明：[`docs/BUTTON_PERMISSIONS.md`](BUTTON_PERMISSIONS.md)
- 配置：`payflow.permission.enforce-button`（默认 true）

### RBAC 角色管控收紧

以下 Controller 方法新增 `@RequireRole(SUPER_ADMIN)`，普通 ADMIN 角色将返回 403：

| Controller | 受限方法 |
|------------|----------|
| `SysUserController` | `POST /admin/users`（创建用户）、`PUT /admin/users/{id}/reset-password` |
| `SystemConfigController` | `POST /admin/system-configs`、`PUT`、`DELETE`、`POST /refresh/*` |
| `SysRoleController` | `POST /admin/roles`、`DELETE /admin/roles/{id}`、`PUT /admin/roles/{id}/menus` |

### 安全加固

| 变更项 | 模块 | 说明 |
|--------|------|------|
| 敏感字段加密存储 | admin-server | `Channel.apiKey`、`PaymentAccount.appSecret/mchKey/certPassword`、`Merchant.merchantKey` 使用 AES-256-GCM 加密落库，API 响应自动过滤（`@JsonProperty(WRITE_ONLY)`） |
| JWT 密钥无硬编码默认值 | admin-server + cashier-server | `jwt.secret`、`payflow.jwt.secret`、`payflow.signature.secret` 不再有默认值，未配置时启动报错 |
| 商户密钥不落配置文件 | cashier-server | `payflow.merchants: []` — 商户签名密钥仅从数据库加载 |
| 回调签名校验强化 | cashier-server | Alipay/UnionPay 回调新增 RSA-SHA256 签名验证（通过 Order→Payment→Account 链查找公钥） |
| SQL 注入加固 | admin-server | `AdminRefundService.applyMerchantScope()` 商户 ID 正则过滤 + 子查询；`FeeRateService.getAuditLogs()` 改用 MyBatis-Plus Page 分页 |
| CSV 注入防护 | admin-server | `AdminOrderController.csvEscape()` 对 `=`/`+`/`-`/`@` 开头单元格加单引号前缀 |
| 登录爆破防护 | admin-server + cashier-server | Redis 计数 + 锁 Key：5 次失败锁 15min |
| Redis 故障关闭 | cashier-server | 拦截器中 Redis 不可用时抛 `BizException(5000)` 而非放行 |
| 商户软删除 | admin-server | `DELETE /admin/merchants/{merchantId}` 改为设置 `status='DELETED'`，不再物理删除 |
| 删除前关联校验 | admin-server | 删除渠道/支付方式/支付账号/商户绑定/菜单/角色等前检查引用；存在关联返回 `code=6006`（HTTP 409），`data.refs` 为引用清单 |

### 删除依赖预检与阻断响应

**预检**：`GET /api/v1/admin/resource-dependencies?resourceType=PAYMENT_METHOD&resourceId=12`

| 字段 | 说明 |
|------|------|
| `resourceType` | `CHANNEL` / `PAYMENT_METHOD` / `PAYMENT_ACCOUNT` / `MERCHANT_PAYMENT_METHOD` / `MERCHANT` / `SYS_MENU` / `SYS_ROLE` |
| `resourceId` | 主键；`MERCHANT` 时为商户号字符串 |

**成功预检**（`code=0`）：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "blocked": false,
    "summary": "可以安全删除",
    "refs": []
  }
}
```

**删除被阻断**（`code=6006`，HTTP 409）：

```json
{
  "code": 6006,
  "message": "支付方式「微信扫码」仍被 2 处配置引用，请先解除关联",
  "data": {
    "blocked": true,
    "summary": "支付方式「微信扫码」仍被 2 处配置引用，请先解除关联",
    "refs": [
      {
        "refType": "MERCHANT_PAYMENT_ROUTE",
        "refId": "1",
        "merchantId": "M100001",
        "label": "商户支付路由 #1（商户 M100001）",
        "resolveHint": "/admin/merchants"
      }
    ]
  }
}
```

支付账号删除额外检查：`admin_channel_routes`、`cashier_channel_merchant_routes`（按 `account_code` 映射）、`recon_task` 非终态任务（`status` 非 `SUCCESS`/`FAIL`）。
| sql.init.mode | cashier-server | `always` → `never`，防止 dev 环境误重置数据 |

---

## 商户数据隔离 (006-merchant-isolation) 契约变更

### 收银台错误码（HTTP 状态）

| code | HTTP | 对外 message | 说明 |
|------|------|--------------|------|
| `5101` | 403 | 商户身份与请求不匹配 | 请求体/query 中 `merchantId` 与 JWT/HMAC 上下文不一致 |
| `5102` | 404 | 请求的资源不存在 | 资源不存在或跨商户访问（对外统一文案） |
| `5103` | — | （不对外返回） | 仅写入 `cashier_security_audit.reason_code`，表示真实越权 |

### 新增管理端 API

| 前端方法 | HTTP | 路径 | 后端 | 权限 |
|----------|------|------|------|------|
| `getSecurityAuditList` | GET | `/admin/security/audit` | [`AdminSecurityAuditController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminSecurityAuditController.java) | `RISK`、`SUPER_ADMIN` |

查询参数：`page`、`pageSize`（≤100）、`merchantId`、`outcome`、`reasonCode`、`requestPath`、`startDate`、`endDate`。

前端页面：[`security-audit.vue`](payflow-admin-client/src/pages/admin/security-audit.vue)，路由 `/admin/security-audit`，菜单「系统管理 → 安全审计」。

### 收银台受保护端点行为摘要

| 路径模式 | 认证 | 隔离行为 |
|----------|------|----------|
| `POST /api/v1/orders` | JWT | `merchantId` 由服务端注入；请求体传其他商户 → 5101 |
| `GET /api/v1/orders/{orderId}` | JWT | 非本商户订单 → 5102/404 |
| `POST /api/v1/refunds` | HMAC | `paymentId` 须属当前商户；否则 5102/404 |
| `GET /api/v1/refunds/{refundId}` | HMAC | 非本商户退款 → 5102/404 |
| `GET /api/v1/merchant/orders/{orderId}` 等 | HMAC | 同上 |
| `GET /api/v1/payments/status/{paymentId}` | 无 | **白名单**：消费者轮询，不做商户隔离 |
| `GET /api/v1/cashier/{orderId}` | 无 | **白名单**：收银台页 |
| `POST /notify/**` | 渠道签名校验 | **系统模式**：`MerchantScopeHolder.runInSystemMode` |

持久层：`cashier_orders`、`cashier_payment_link` 在商户上下文中自动追加 `merchant_id` 条件（MyBatis TenantLine）。

---

## 商户数据隔离治理 (008-merchant-data-isolation) 契约变更

### 后台授权范围

| Actor | 服务端范围规则 | 前端输入规则 | 输出规则 |
|-------|----------------|--------------|----------|
| 商户管理员 | 以后端登录态解析的 `authorizedMerchantIds` 为唯一可信边界 | `merchantId`、资源 ID、批量 ID、导出条件只能缩小范围，不能扩大范围 | 列表、详情、统计、导出、批量结果不得包含授权外资源 |
| 系统管理员 | `platformAdmin=true` 时可跨商户筛选和治理 | 可传 `merchantId` 查看指定商户或全局治理视图 | 商户级数据必须展示 `merchantId` 或商户归属摘要 |
| 系统任务 | 仅用于回调、补单、对账、Webhook 等后台流程 | 不接收前端扩大范围参数 | 写入结果必须保留原始资源商户归属 |

跨商户访问被拒绝时，后台接口必须返回通用拒绝或空结果，不得暴露目标数据是否存在、目标商户名称、金额、状态、内部查询条件或敏感字段。

### 新增/调整管理端 API

| 前端方法 | HTTP | 路径 | 后端 Controller | 权限/范围 |
|----------|------|------|-----------------|-----------|
| `getDataIsolationChecks` | GET | `/admin/data-isolation/checks` | `DataIsolationCheckController` | 系统管理员可全局查询；商户管理员仅限授权范围内检查摘要 |
| `scanDataIsolation` | POST | `/admin/data-isolation/checks/scan` | `DataIsolationCheckController` | 系统管理员触发扫描 |
| `updateDataIsolationRemediation` | PUT | `/admin/data-isolation/checks/{checkId}/remediation` | `DataIsolationCheckController` | 系统管理员更新整改状态或豁免理由 |
| `getAdminScope` | 登录响应字段 | `/admin/auth/login` | `AuthController` / `AdminAuthServiceImpl` | 返回 `authorizedMerchantIds`、`platformAdmin`、`scopeMode` |
| `getAuditLogs` | GET | `/admin/audit-logs` | `AdminAuditLogController` | 增加 `merchantId`、`resourceType`、`result` 筛选与展示 |
| `getSystemConfigs` | GET | `/admin/system-configs` | `SystemConfigController` | 增加全局配置标识和敏感摘要，商户专属配置按商户级处理 |

### 隔离检查查询契约

请求参数：`classification`、`riskLevel`、`remediationStatus`、`targetType`、`merchantId`、`page`、`size`。

响应数据：

| 字段 | 说明 |
|------|------|
| `checkId` | 检查项标识 |
| `targetType` | `DATA_TABLE` / `PAGE` / `API` / `ASYNC_TASK` / `EXPORT_TASK` |
| `targetName` | 表名、页面、接口或任务名 |
| `classification` | `MERCHANT` / `GLOBAL` / `SYSTEM_AUDIT` / `MANUAL_REVIEW` |
| `merchantFieldStatus` | `PRESENT` / `MISSING` / `NOT_APPLICABLE` / `PENDING_CONFIRM` |
| `riskLevel` | `HIGH` / `MEDIUM` / `LOW` |
| `affectedEntries` | 受影响入口摘要 |
| `remediationStatus` | `PENDING` / `IN_PROGRESS` / `DONE` / `EXEMPTED` / `NEEDS_MANUAL_REVIEW` |
| `decisionReason` | 分类、整改或豁免理由 |
| `merchantId` | 关联商户；全局或待确认项可为空 |

### 商户级入口范围要求

| 入口类别 | 受影响接口 | 商户管理员期望 | 系统管理员期望 |
|----------|------------|----------------|----------------|
| 订单 | `/admin/orders`、`/admin/orders/{orderId}`、`/admin/orders/export` | 仅授权商户订单；授权外详情通用拒绝 | 可按商户筛选并展示归属 |
| 支付 | 后台支付查询服务 | 通过订单归属限制授权范围 | 可跨商户排障 |
| 退款 | `/admin/refunds`、`/admin/refunds/{refundId}/approve|reject` | 授权外退款不可见且不可审核 | 可按商户筛选和审核 |
| 渠道账号/路由 | `/admin/channels/accounts`、`/admin/channels/routes`、`/admin/merchant-payment-*` | 仅授权商户配置，敏感字段脱敏 | 可维护全局账号池和商户绑定 |
| 对账 | `/admin/reconcile/**` | 仅归属明确且授权内的任务/差异可见 | 可治理待人工确认项 |
| 风控 | `/admin/risk/**`、`/merchant/risk/**` | 商户规则限本商户，平台规则只读或摘要展示 | 可维护平台规则和作用商户范围 |
| 仪表盘/费率/流失预警 | `/admin/dashboard/**`、`/admin/fee-rates/**`、`/admin/churn-alerts` | 统计和列表限定授权商户 | 可跨商户或按商户查看 |
| 导出/批量/异步 | `/admin/export/**` 及批量操作 | 授权外资源不处理、不导出 | 按平台权限治理 |
| 审计 | `/admin/security/audit`、`/admin/audit-logs` | 仅自身授权范围相关摘要 | 可定位商户归属、资源类别和拒绝原因 |

### 对账差异工单（工作流升级）契约补充

| 前端方法 | HTTP | 路径 | 后端 Controller | 权限/范围 |
|----------|------|------|-----------------|-----------|
| `getReconWorkItems` | GET | `/admin/reconcile/diffs/work-items` | `AdminReconController` | 仅授权商户范围内工单可见；支持 onlyMine/onlyUnassigned 过滤 |
| `getReconWorkItemDetail` | GET | `/admin/reconcile/diffs/{diffId}` | `AdminReconController` | 仅授权商户范围内可见，返回 diff+assignment+audits |
| `claimReconWorkItem` | POST | `/admin/reconcile/diffs/{diffId}/claim` | `AdminReconController` | `recon:diff:assign` |
| `assignReconWorkItem` | POST | `/admin/reconcile/diffs/{diffId}/assign` | `AdminReconController` | `recon:diff:assign` |
| `startReconWorkItem` | POST | `/admin/reconcile/diffs/{diffId}/start` | `AdminReconController` | `recon:diff:handle` |
| `completeReconWorkItem` | POST | `/admin/reconcile/diffs/{diffId}/complete` | `AdminReconController` | `recon:diff:handle` |
| `commentReconWorkItem` | POST | `/admin/reconcile/diffs/{diffId}/comment` | `AdminReconController` | `recon:diff:handle` |

### 对账 SLA 规则契约补充

| 前端方法 | HTTP | 路径 | 后端 Controller | 权限/范围 |
|----------|------|------|-----------------|-----------|
| `getReconSlaRules` | GET | `/admin/reconcile/sla-rules` | `AdminReconController` | `recon:manage` |
| `saveReconSlaRule` | PUT | `/admin/reconcile/sla-rules/{diffType}` | `AdminReconController` | `recon:manage` |

### 对账归因看板 / 长尾 / 报告订阅

| 前端方法 | HTTP | 路径 | 后端 Controller | 权限/范围 |
|----------|------|------|-----------------|-----------|
| `getReconAggregationDashboard` | GET | `/admin/reconcile/aggregation/dashboard` | `AdminReconController` | merchantScope |
| `getReconLongTailSummary` | GET | `/admin/reconcile/long-tail/summary` | `AdminReconController` | merchantScope |
| `batchReconAcceptLoss` | POST | `/admin/reconcile/long-tail/accept-loss` | `AdminReconController` | `recon:manage` |
| `getReconSubscriptions` | GET | `/admin/reconcile/subscriptions` | `AdminReconController` | `recon:report:subscribe` |
| `createReconSubscription` | POST | `/admin/reconcile/subscriptions` | `AdminReconController` | `recon:report:subscribe` |
| `deleteReconSubscription` | DELETE | `/admin/reconcile/subscriptions/{id}` | `AdminReconController` | `recon:report:subscribe` |
| `getReconReportDetail` | GET | `/admin/reconcile/reports/{snapshotId}` | `AdminReconController` | `recon:report:subscribe` |

### 全局配置白名单

以下目标为全局级或豁免候选，不强制绑定单一商户：`admin_channels` 基础渠道定义、`admin_payment_methods` 支付方式公共定义、`sys_menus` 菜单模板、`sys_roles` 角色定义、`/admin/dicts` 公共字典、平台级系统配置。若配置包含商户专属密钥、账号、费率、回调地址或证书，则必须转为商户级敏感数据并按授权范围脱敏输出。
