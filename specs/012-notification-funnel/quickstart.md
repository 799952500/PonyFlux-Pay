# Quickstart: 通知中心与支付漏斗真实化

**Feature**: `012-notification-funnel`
**Branch**: `012-notification-funnel`

## 前提条件

- Java 17 / Maven
- Node.js 18+ / npm
- MySQL 8.0（`payflow_admin` + `payflow_cashier` 两个数据库已存在）
- Redis 运行中（`127.0.0.1:6379`）

## 数据库迁移

Flyway 迁移文件 `V9__notification_center.sql` 在 admin-server 启动时自动执行。

如需手动执行：

```sql
source payflow-admin-server/src/main/resources/db/migration/admin/V9__notification_center.sql
```

## 启动后端

```bash
# Admin Server (port 3003)
mvn -B -pl payflow-admin-server spring-boot:run

# Cashier Server (port 3002) — 漏斗依赖 cashier 数据库需运行
mvn -B -pl payflow-cashier-server spring-boot:run
```

## 启动前端

```bash
cd payflow-admin-client && npm run dev
# 浏览器打开 http://localhost:3001
```

## 验证通知中心

1. 登录管理后台，顶栏铃铛应显示未读 badge（基于 demo seed 数据）
2. 点击铃铛 → 弹出面板展示最近 10 条未读
3. 点击"查看全部" → 跳转到 `/admin/notifications` 列表页
4. 切换 Tab（全部/未读/已读）+ 业务类型筛选器

## 验证支付漏斗

1. 导航到 `/admin/insights/funnel`
2. 应看到 ECharts 漏斗图（CREATED → PAYING → PAID）
3. 右侧流失支路柱状图（FAILED / CLOSED / EXPIRED）
4. 筛选器：选择时间范围 / 商户 / 渠道 → 漏斗即时刷新

## API 测试

```bash
# 获取未读数
curl -H "Authorization: Bearer <token>" http://localhost:3003/api/v1/admin/notifications/unread-count

# 通知列表
curl -H "Authorization: Bearer <token>" "http://localhost:3003/api/v1/admin/notifications?read=false&page=1&size=10"

# 支付漏斗
curl -H "Authorization: Bearer <token>" "http://localhost:3003/api/v1/admin/insights/funnel?dateFrom=2026-05-20&dateTo=2026-05-26"
```

## 关键文件

| 文件 | 说明 |
|------|------|
| `AdminNotificationController.java` | 通知 CRUD 端点 |
| `NotificationService.java` | 通知写入/查询/标记已读 |
| `AdminInsightsController.java` | 漏斗聚合端点 |
| `FunnelService.java` | 漏斗统计逻辑 |
| `NotificationPopover.vue` | 铃铛下拉面板组件 |
| `notifications.vue` | 通知列表页 |
| `insights-funnel.vue` | 漏斗图页 |
| `useNotification.ts` | 通知轮询 composable |
