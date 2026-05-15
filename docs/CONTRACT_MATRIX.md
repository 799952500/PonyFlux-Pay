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
| `getOrderDetail` | GET | `/admin/orders/{orderId}` | 同上 |
| `closeOrder` | POST | `/admin/orders/{orderId}/close` | 同上 |
| `getRefunds` | GET | `/admin/refunds` | [`AdminRefundController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminRefundController.java) |
| `approveRefund` / `rejectRefund` | POST | `/admin/refunds/{refundId}/approve` / `reject` | 同上 |
| `getChannels` 等 | * | `/admin/channels` | [`AdminChannelController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminChannelController.java) |
| 渠道账号池 | * | `/admin/channels/accounts` | [`AdminChannelAccountController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminChannelAccountController.java) |
| 渠道路由 | * | `/admin/channels/routes` | [`AdminChannelRouteController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminChannelRouteController.java) |
| `getMerchants` 等 | * | `/admin/merchants` | [`AdminMerchantController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminMerchantController.java) |
| 兼容路径 `/merchants` | * | `/merchants` | [`MerchantController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/MerchantController.java)（若仍存在则仅兼容旧客户端） |
| 商户支付方式 | * | `/admin/merchant-payment-methods` | [`MerchantPaymentMethodController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/MerchantPaymentMethodController.java) |
| 商户支付路由 | * | `/admin/merchant-payment-routes` | [`MerchantPaymentRouteController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/MerchantPaymentRouteController.java) |
| 风控 | GET/PUT | `/admin/risk/rules` | [`AdminRiskController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminRiskController.java) |
| 支付方式 | * | `/admin/payment-methods` | [`PaymentMethodController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/PaymentMethodController.java) |
| 支付账号（旧路径） | * | `/admin/payment-accounts` | [`PaymentAccountController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/PaymentAccountController.java) |
| 角色/菜单/用户 | * | `/admin/roles`、`/admin/menus`、`/admin/users` | [`SysRoleController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/SysRoleController.java)、[`SysMenuController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/SysMenuController.java)、[`SysUserController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/SysUserController.java) |
| 系统配置 | * | `/admin/system-configs` | [`SystemConfigController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/SystemConfigController.java) |
| 登录 | POST | `/admin/auth/login` | [`AuthController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AuthController.java) |
| 字典枚举 | GET | `/admin/dicts` | [`AdminDictController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminDictController.java) |
| 订单导出 CSV | GET | `/admin/orders/export` | [`AdminOrderController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminOrderController.java) |
| 全局搜索 | GET | `/admin/search` | [`AdminSearchController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminSearchController.java) |
| 通知摘要 | GET | `/admin/notifications/summary` | [`AdminNotificationController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminNotificationController.java) |
| 审计日志 | GET | `/admin/audit-logs` | [`AdminAuditLogController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminAuditLogController.java) |
| 验证码 | GET | `/admin/auth/captcha` | [`AuthController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AuthController.java) |
| 版本信息 | GET | `/admin/meta/version` | [`AdminMetaController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminMetaController.java) |

JWT：除 `/admin/auth/login`、`/admin/auth/captcha` 外，`/api/v1/admin/**` 需带 `Authorization: Bearer <token>`（见 [`JwtInterceptor`](payflow-admin-server/src/main/java/com/payflow/admin/interceptor/JwtInterceptor.java)）。

---

## 收银台客户端（cashier-client ↔ payflow-cashier-server）

| 前端方法 | HTTP | 路径 | 后端 |
|----------|------|------|------|
| `merchantLogin` | POST | `/auth/login` | [`MerchantAuthController`](payflow-cashier-server/src/main/java/com/payflow/cashier/controller/MerchantAuthController.java) |
| `getCashierInfo` | GET | `/cashier/{orderId}` | [`CashierController`](payflow-cashier-server/src/main/java/com/payflow/cashier/controller/CashierController.java) |
| `createPayment` | POST | `/payments` | [`PaymentController`](payflow-cashier-server/src/main/java/com/payflow/cashier/controller/PaymentController.java) |
| `pollPaymentStatus` | GET | `/payments/status/{paymentId}` | 同上（无需商户签名） |

退款、商户查询等走 `/api/v1/refunds`、`/api/v1/merchant/**`，需商户签名或 JWT，不在默认 cashier SPA 最小路径内。

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

- [ ] POST `/admin/auth/login`（可选：图形验证码流程）
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
- [ ] GET `/admin/orders/export` 下载 CSV（权限：登录即可，生产建议加角色）

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
| sql.init.mode | cashier-server | `always` → `never`，防止 dev 环境误重置数据 |
