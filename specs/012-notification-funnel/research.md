# Research: 通知中心与支付漏斗真实化

**Feature**: `012-notification-funnel`
**Date**: 2026-05-26

## 决策记录

### R-001: 通知存储方案——单表 vs 收件箱模式

**Decision**: 采用**单表 `admin_notifications`**，每条通知 = 一个接收人一行。

**Rationale**:
- 管理员用户数有限（<50 人），即使每次事件按角色扇出也不会产生大量数据
- 单表查询简单，无需 JOIN 消息表 + 收件箱表
- 90 天清理策略确保数据量可控
- MyBatis-Plus 单表分页天然支持

**Alternatives considered**:
- 消息表 + 收件箱表（message + inbox）：适用于高并发 IM 场景，本需求无此必要
- Redis Stream + MySQL 归档：增加复杂性，通知量小无需异构存储

---

### R-002: 通知幂等去重方案

**Decision**: 在 `admin_notifications` 表上对 `(biz_type, biz_key)` 建唯一索引，写入前查询 24 小时内是否已存在。

**Rationale**:
- `biz_key` 为业务唯一键（如退款 ID `REF-xxx`、流失预警 ID `CHURN-xxx`），`biz_type` 为通知类型枚举
- 唯一索引确保并发场景下的最终一致性（INSERT IGNORE 或 ON DUPLICATE KEY）
- 24 小时窗口通过查询 `created_at > now() - 24h` 实现软去重，不依赖唯一索引时间范围

**Alternatives considered**:
- 全局唯一索引（不限时间窗口）：会导致同一退款被重新审批时无法再次通知
- Redis 布隆过滤器：内存占用与复杂性不匹配问题规模

---

### R-003: 事件源接入方式——同步 vs 异步

**Decision**: 使用 Spring `@Async` + 已有的 `taskExecutor` 线程池，在事件后异步写入通知。

**Rationale**:
- 项目已配置 `@EnableAsync` 和 `ThreadPoolTaskExecutor`（admin-server 内）
- 通知写入不得阻塞主事务（退款审批、导出任务等），异步解耦
- 不引入 RocketMQ（当前 admin-server 未启用 MQ），保持依赖最小
- 写入失败仅记 ERROR 日志，不影响主流程

**Alternatives considered**:
- Spring `@TransactionalEventListener(phase = AFTER_COMMIT)`：更优雅但需要每个事件都发布 ApplicationEvent，改造面较大
- RocketMQ：admin-server 目前未接入，引入成本高

---

### R-004: 漏斗统计口径——"当前状态" vs "曾经达到过"

**Decision**: 采用**"当前状态"聚合**，按 `cashier_orders.status` 做 `GROUP BY`。

**Rationale**:
- 调查发现 `cashier_orders` 表只有一个 `status` 字段（无状态历史表），无法回溯"曾经达到过"
- spec 中的"曾经达到过"假设需要修正：在缺少状态变更日志的情况下，最务实的方案是——CREATED 统计全部创建订单；PAYING 统计 status IN (PAYING, PAID, SUCCESS)；PAID 统计 status IN (PAID, SUCCESS)。这利用了状态单向递进的事实（PAYING 一定由 CREATED 转来，PAID 一定由 PAYING 转来），等效于"曾经达到过"
- FAILED / CLOSED / EXPIRED 可直接按当前 status 统计

**Alternatives considered**:
- 新建 `cashier_order_status_log` 状态变更日志表：侵入 cashier-server 模块边界，违反 spec "不修改 cashier-server" 约束
- 预聚合表 `admin_funnel_metrics`：增加复杂性，demo 数据量不需要

---

### R-005: 漏斗与 Dashboard "转化率" KPI 的口径对齐

**Decision**: 漏斗和 Dashboard 在同时间范围 + 同商户范围下，使用相同基础 SQL，确保 `(PAID / CREATED) * 100` 一致。

**Rationale**:
- Dashboard `conversionRate = todayPaid * 100.0 / todayOrders`，其中 `todayOrders = countCreatedOnDay(today)`，`todayPaid = countPaidOnDay(today)`
- 漏斗在"今日"范围下也使用相同的 `countCreatedOnDay` 和 `countPaidOnDay`，一致性通过共用 SQL 保证
- 漏斗多了 PAYING 中间阶段和流失支路，是 Dashboard 转化率的超集

**Alternatives considered**: 无替代方案需要，直接复用保证一致

---

### R-006: 前端顶栏 Badge 刷新策略

**Decision**: 使用 `setInterval` 60 秒轮询 `GET /notifications/unread-count`，配合 Pinia store 管理状态。

**Rationale**:
- 项目当前无 WebSocket/SSE 基础设施
- 通知非实时 IM 场景，60 秒延迟可接受
- 标记已读后本地 store 即时 -1 并同步更新 badge，无需等轮询
- 组件卸载时 `clearInterval` 避免泄漏

**Alternatives considered**:
- WebSocket/SSE：需在 admin-server 新增端点和前端连接管理，引入新基础设施复杂度
- 30 秒轮询：增加服务端压力，收益不大

---

### R-007: 通知接收人确定策略

**Decision**: `NotificationService.send()` 接受两种接收人模式：① 指定 userId 列表；② 指定角色/权限码，由 Service 查询 `admin_sys_users` + `admin_sys_user_roles` + `admin_sys_role_menus` 解析具体用户。

**Rationale**:
- "退款审批" → 给有 `refund:approve` 权限的用户
- "导出完成" → 给发起人（指定 userId）
- "流失预警超时" → 给该商户对应运营角色
- 需要两种模式覆盖不同场景

**Alternatives considered**:
- 全部走权限码：导出完成只需通知发起人一个人，按权限码扇出不合适
- 全部走 userId：退款审批需要通知所有有审批权限的人，硬编码用户列表不可维护
