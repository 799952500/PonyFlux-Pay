# 按钮级权限（BUTTON Permissions）

## 命名规范

权限码采用 `{module}:{resource}:{action}` 或 `{module}:{action}` 两段/三段式，全部小写，段内使用下划线：

- `refund:approve` — 退款审批通过
- `order:export` — 订单导出
- `payment_method:delete` — 删除支付方式

## 数据模型

- 表：`sys_menus`，`menu_type = 'BUTTON'`
- 字段：`perm_code`（必填、唯一）、`api_pattern`（可选，文档化）
- 角色绑定：沿用 `sys_role_menus`

## 后端

- 注解：`@RequirePermission("perm:code")`
- 拦截器：`PermissionInterceptor`（在 `JwtInterceptor`、`RoleBasedInterceptor` 之后）
- `SUPER_ADMIN` 角色 JWT 始终放行
- 配置：`payflow.permission.enforce-button`（默认 `true`，紧急可关）

## 前端

- 登录 / profile 返回 `permissions: string[]`
- 刷新：`GET /api/v1/admin/auth/permissions`
- 指令：`v-permission="'refund:approve'"` 或 `v-permission.any="['a','b']"`
- 组合式：`usePermission().hasPermission(code, 'AND'|'OR')`

## 权限码字典（种子数据）

| perm_code | 说明 | API |
|-----------|------|-----|
| order:export | 导出订单 | GET /api/v1/admin/orders/export |
| order:close | 关单 | POST /api/v1/admin/orders/*/close |
| refund:create | 申请退款 | POST /api/v1/admin/orders/*/refund-requests |
| order:payment:query | 查单并同步 | POST .../query-channel |
| refund:approve | 审批通过 | POST /api/v1/admin/refunds/*/approve |
| refund:reject | 审批拒绝 | POST /api/v1/admin/refunds/*/reject |
| channel:create/edit/delete | 渠道 CRUD | /api/v1/admin/channels |
| payment_method:create/edit/delete | 支付方式 CRUD | /api/v1/admin/payment-methods |
| payment_account:create/edit/delete | 支付账号 CRUD | /api/v1/admin/payment-accounts |
| merchant:edit/delete | 商户编辑/删除 | /api/v1/admin/merchants/* |
| onboarding:approve/reject | 进件审批 | /api/v1/admin/onboarding/applications/* |
| risk:rule:write | 风控规则维护 | /api/v1/admin/risk/rules |
| recon:manual_run | 手动对账 | POST .../reconcile/tasks/manual-run |
| recon:diff:handle | 差异处理 | POST .../reconcile/diffs/*/handle |
| fee_rate:write | 费率配置 | /api/v1/admin/fee-rates |
| system_config:write | 系统配置 | /api/v1/admin/system-configs |
| role:create/edit/delete/assign_menu | 角色管理 | /api/v1/admin/roles |
| menu:create/edit/delete | 菜单管理 | /api/v1/admin/menus |
| user:create/edit/reset_password | 用户管理 | /api/v1/admin/users |
| data_isolation:remediate | 隔离治理修复 | PUT .../data-isolation/checks/*/remediation |

## 角色默认授权（演示种子）

| 角色 | 按钮权限范围 |
|------|----------------|
| SUPER_ADMIN | 全部 |
| ADMIN | 除 role:delete、menu:delete、system_config:write 外全部 |
| FINANCE | 订单/退款相关 |
| RISK | 风控、对账处理 |

## 新增敏感接口检查清单

1. 在 `sys_menus` 增加 BUTTON 节点（`perm_code` + `api_pattern`）
2. Controller 方法加 `@RequirePermission`
3. 前端按钮加 `v-permission`
4. 更新本文件与 `docs/CONTRACT_MATRIX.md`
