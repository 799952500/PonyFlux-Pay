# 前端 API 与后端路由对照表

约定：`payflow-admin-client` / `payflow-cashier-client` 的 `axios` **baseURL** 均为 `/api/v1`，下列路径为 **相对 baseURL** 的路径；完整 URL 形如 `http://host:port/api/v1` + 路径。

成功响应：后端多为 `{ code: 0, message: "success", data: ... }`，前端拦截器解包后调用方拿到 **data**。

---

## 管理后台（admin-client ↔ payflow-admin-server）

| 前端方法（参考） | HTTP | 路径 | 后端 Controller |
|------------------|------|------|-----------------|
| `getDashboardStats` | GET | `/admin/dashboard` | [`AdminDashboardController`](payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminDashboardController.java) 根路径 |
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
