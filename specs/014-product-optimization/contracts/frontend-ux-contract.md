# UI Contract: 前端体验与错误处理

**Created**: 2026-05-29  
**Feature**: [../spec.md](../spec.md)

适用于 `payflow-admin-client` 与 `payflow-cashier-client`。

## 1) 页面加载状态机

每个数据驱动页面 MUST 实现以下状态之一：

| 状态 | 展示 | 触发条件 |
|------|------|----------|
| `loading` | `v-loading` 或 `el-skeleton` | 请求进行中 |
| `success` | 正常内容 | `code===0` 且有数据 |
| `empty` | `el-empty` + 文案「暂无数据」 | `code===0` 且列表为空 |
| `error` | 错误卡片 + 「重试」按钮 | 网络错误 / `code!==0` / 异常 catch |

**禁止**: `catch` 块仅 `console.error` 或注释「静默」而无用户提示。

## 2) P1 强制覆盖页面

| 客户端 | 页面路径 | FR |
|--------|----------|-----|
| admin | `reconcile/work-item-detail.vue` | FR-010 |
| admin | `reconcile/report-detail.vue` | FR-010 |
| admin | `components/NotificationPopover.vue` | FR-010 |
| admin | `reconcile/sla-rules.vue` | FR-010 |
| cashier | `pc/index.vue`, `h5/index.vue` | FR-010, FR-012 |
| cashier | `composables/useCashierCheckout.ts` | FR-012 |

## 3) HTTP 拦截器行为

### admin `api/request.ts`

| 状态码 | 行为 |
|--------|------|
| 401 | `ElMessage.warning('登录已过期')` → 清 token → `/login` |
| 403 | `ElMessage.error('无权限访问')` |
| 5xx | `ElMessage.error(message)` |

### cashier `api/request.ts`

- 对齐 admin：解析 `code` 前先判断非 JSON 响应。
- 支付轮询连续失败 **≥3 次** → 提示用户并停止轮询。

## 4) API 调用集中化

| 禁止 | 应使用 |
|------|--------|
| 页面内 `request.get/post` 散落 | `api/admin.ts` / `api/auth.ts` |
| 裸 `fetch()` | Axios 实例（blob 下载封装 `downloadBlob()`） |

## 5) i18n（Wave 2）

| 客户端 | 要求 |
|--------|------|
| admin | `useI18n()` 覆盖主要 CRUD + reconcile 模块 |
| cashier | `main.ts` 注册 `createI18n`，`fallbackLocale: 'zh-CN'` |

**缺失 key**: 显示中文默认值，禁止显示 raw key（如 `order.title`）。

## 6) 验收检查表（手工 / E2E）

- [ ] 停 admin-server → 打开工单详情 → 见错误+重试
- [ ] 停 cashier-server → 打开收银台 → 见错误+重试
- [ ] 二维码场景无 paymentResult 点「我已支付」→ loading 复位
- [ ] 空列表页 → `el-empty` 可见
