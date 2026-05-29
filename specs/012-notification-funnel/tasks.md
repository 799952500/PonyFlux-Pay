# Tasks: 通知中心与支付漏斗真实化

**Input**: Design documents from `/specs/012-notification-funnel/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Spec 中要求 Playwright E2E 验证和集成测试。测试任务包含在 Polish 阶段。

**Organization**: 任务按用户故事分组，每个故事可独立实施和测试。任务分组采用 PonyFlux-Pay 项目的 Maven 模块边界。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行执行（不同文件，无依赖）
- **[Story]**: 归属的用户故事（US1、US2、US3、US4、US5）
- 任务描述包含精确的文件路径

---

## Phase 1: Setup（项目结构准备）

**Purpose**: 数据库迁移和全量 DDL 同步，确保 Flyway 迁移与 schema 文件一致

- [x] T001 创建 Flyway 迁移文件 `payflow-admin-server/src/main/resources/db/migration/admin/V9__notification_center.sql`，包含 `admin_notifications` 表 DDL（字段、索引定义见 `data-model.md`）
- [x] T002 同步更新全量 DDL `sql/schema/payflow_admin.sql`，追加 `DROP TABLE IF EXISTS admin_notifications` 和 `CREATE TABLE admin_notifications` 语句
- [x] T003 [P] 追加通知 demo 种子数据到 `sql/seed/payflow_admin_seed.sql`，插入 20+ 条覆盖全部 `biz_type` 的示例通知，接收人为已有 seed 用户

---

## Phase 2: Foundational（阻断性前置条件）

**Purpose**: 通知中心的核心后端基础设施（Entity/Enum/Mapper/Service），是 US1-US3 的共享前提

**⚠️ CRITICAL**: 在本阶段完成之前，不得开始任何用户故事工作。

- [x] T004 [P] 创建 `NotificationTypeEnum` 枚举 `payflow-admin-server/src/main/java/com/payflow/admin/enums/NotificationTypeEnum.java`，枚举值：`REFUND_APPROVAL`、`CHURN_OVERDUE`、`EXPORT_COMPLETED`、`EXPORT_FAILED`、`RECON_DIFF`、`WEBHOOK_FAILURE`、`SYSTEM_ANNOUNCEMENT`，每个值含 `icon` 和 `defaultLink` 属性
- [x] T005 [P] 创建 `Notification` 实体类 `payflow-admin-server/src/main/java/com/payflow/admin/entity/Notification.java`，使用 `@TableName("admin_notifications")` + Lombok `@Data`，字段按 `data-model.md` 定义，`id` 用 `@TableId(type = IdType.AUTO)` + `@JsonSerialize(using = ToStringSerializer.class)`
- [x] T006 [P] 创建 `NotificationDTO` 类 `payflow-admin-server/src/main/java/com/payflow/admin/dto/NotificationDTO.java`，字段：`id`、`bizType`、`title`、`summary`、`link`、`readStatus`、`createdAt`
- [x] T007 创建 `NotificationMapper` 接口 `payflow-admin-server/src/main/java/com/payflow/admin/mapper/NotificationMapper.java`，继承 `BaseMapper<Notification>`；在 `AdminDataSourceConfig` 中手动注册为 `MapperFactoryBean`（遵循主数据源不用 `@MapperScan` 的宪法规则）
- [x] T008 创建 `NotificationService` 类 `payflow-admin-server/src/main/java/com/payflow/admin/service/NotificationService.java`，实现核心方法：① `send(NotificationTypeEnum type, String bizKey, String title, String summary, String link, String merchantId, List<Long> recipientUserIds)` — 异步写入通知（幂等去重：同 biz_type+biz_key+recipient 不重复）；② `sendToRole(... String permissionCode)` — 按权限码查询接收人再调 send；③ `listByUser(Long userId, List<String> merchantScope, String readFilter, String typeFilter, int page, int size)` — 分页查询；④ `countUnread(Long userId, List<String> merchantScope)` — 未读计数；⑤ `markRead(Long id, Long userId)` — 单条标记已读；⑥ `markAllRead(Long userId, List<String> merchantScope)` — 全部标记已读；⑦ `markBatchRead(List<Long> ids, Long userId)` — 批量标记已读。`send` 方法加 `@Async` 注解
- [x] T009 [P] 创建 `FunnelStageDTO` 和 `FunnelResult` DTO `payflow-admin-server/src/main/java/com/payflow/admin/dto/FunnelStageDTO.java` 和 `payflow-admin-server/src/main/java/com/payflow/admin/dto/FunnelResult.java`，字段按 `contracts/funnel-api.md` 定义
- [x] T010 [P] 在前端 `payflow-admin-client/src/api/admin.ts` 中追加通知和漏斗 API 函数：`getNotifications(params)`、`getUnreadCount()`、`markNotificationRead(id)`、`markAllNotificationsRead()`、`markBatchNotificationsRead(ids)`、`getInsightsFunnel(params)` 签名升级（新增 dateFrom/dateTo/merchantId/channel 参数）

**Checkpoint**: 基础设施就绪 — 可以开始并行实现用户故事

---

## Phase 3: User Story 1 - 顶栏 Bell 显示真实未读消息并可下钻查看 (Priority: P1) 🎯 MVP

**Goal**: 管理员登录后，顶栏铃铛显示真实未读 badge，点击弹出最近 10 条未读预览面板

**Independent Test**: 在 demo 库中有 seed 通知数据，登录后 ① badge 显示 ≥1；② 点击铃铛弹出面板包含通知条目；③ 点击条目跳转到业务详情；④ 点击"标记已读"后 badge 减 1

### Implementation for User Story 1

- [x] T011 [US1] 改造 `AdminNotificationController` `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminNotificationController.java`：注入 `NotificationService`，新增 `GET /unread-count` 端点（返回 `{ code, message, data: { count } }`），扩展现有 `GET /summary` 返回 `unreadCount` 字段
- [x] T012 [US1] 在 `AdminNotificationController` 新增 `GET /` 端点（通知列表分页），支持 query 参数 `read`、`type`、`page`、`size`，强制 `merchantScope` 过滤，返回统一响应格式
- [x] T013 [US1] 在 `AdminNotificationController` 新增 `POST /{id}/read` 端点（单条标记已读），校验通知归属当前用户
- [x] T014 [P] [US1] 创建通知轮询 composable `payflow-admin-client/src/composables/useNotification.ts`：使用 Pinia store 管理 `unreadCount` 状态，`setInterval` 60 秒轮询 `getUnreadCount()`，提供 `decrementUnread(n)` 方法用于本地即时更新，组件卸载时 `clearInterval`
- [x] T015 [P] [US1] 创建通知预览面板组件 `payflow-admin-client/src/components/NotificationPopover.vue`：使用 `el-popover` 包裹铃铛，弹出面板展示最近 10 条未读（调 `getNotifications({ read: 'false', size: 10 })`），每条含 `bizType` 对应图标、`title`、相对时间（`dayjs` relative）、点击跳转 `link` 并标记已读，面板底部"全部标记已读"和"查看全部"按钮
- [x] T016 [US1] 改造 `payflow-admin-client/src/pages/admin/layout.vue` 顶栏 Bell 部分：替换静态 `<el-badge :value="0" :hidden="true">` 为动态绑定 `useNotification` store 的 `unreadCount`，用 `NotificationPopover` 组件包裹铃铛

**Checkpoint**: User Story 1 完成后，Bell badge + 预览面板独立可测

---

## Phase 4: User Story 2 - 通知中心列表页支持分类筛选与已读管理 (Priority: P1)

**Goal**: 通知中心列表页替换当前占位，支持 全部/未读/已读 Tab、业务类型筛选、批量已读、分页

**Independent Test**: 在 demo 库有 30+ 条通知，访问 `/admin/notifications` ① 默认按时间倒序分页；② 切换 Tab 筛选已读/未读；③ 勾选多条后"批量标记已读"；④ 切换业务类型筛选器

### Implementation for User Story 2

- [x] T017 [US2] 在 `AdminNotificationController` 新增 `POST /read-all` 和 `POST /read-batch` 端点，`read-batch` 接收 `{ ids: [Long] }` JSON body，两者均强制 `merchantScope` 过滤，返回 `{ affected }` 计数
- [x] T018 [US2] 改造 `payflow-admin-client/src/pages/admin/notifications.vue`：替换当前占位实现，改为完整列表页——① 顶部 3 个 Tab（全部/未读/已读）；② 业务类型下拉筛选（El-Select）；③ 通知卡片列表（左侧图标 + 标题/摘要/时间 + 右侧已读/未读标识）；④ 表格选择模式 + "批量标记已读"按钮；⑤ 底部 `el-pagination` 分页；⑥ Tab 切换和筛选变更 watch 后重新请求 + URL query 同步
- [x] T019 [US2] 在 `payflow-admin-client/src/i18n/` 中中英文语言包追加通知中心相关翻译键（`notifications.tabs.all`、`notifications.tabs.unread`、`notifications.tabs.read`、`notifications.batchRead`、`notifications.markAllRead`、各 `bizType` 显示名称）

**Checkpoint**: User Story 2 完成后，通知列表页独立可测

---

## Phase 5: User Story 3 - 业务事件自动写入通知 (Priority: P1)

**Goal**: 5 类业务事件触发时自动写入通知表，无需手动维护

**Independent Test**: 分别触发每类事件 ① 检查 `admin_notifications` 表新增记录；② 接收人正确；③ 重复触发不产生重复通知

### Implementation for User Story 3

- [x] T020 [US3] 退款审批通知接入：在退款创建/状态变更逻辑中（定位 admin-server 中消费 cashier 退款数据的 Service 或定时任务），当检测到 `status=REFUNDING` 时调用 `notificationService.sendToRole(REFUND_APPROVAL, refundId, title, summary, link, merchantId, "refund:approve")`。定位文件 `payflow-admin-server/src/main/java/com/payflow/admin/service/` 中相关退款 Service
- [x] T021 [P] [US3] 流失预警超时通知接入：修改 `payflow-admin-server/src/main/java/com/payflow/admin/task/ChurnDetectionTask.java` 的 `checkOverdueAlerts()` 方法，将 `log.warn` 替换为（或追加）调用 `notificationService.send(CHURN_OVERDUE, alertId, ...)`，给该商户对应运营角色写入通知
- [x] T022 [P] [US3] 导出完成通知接入：修改 `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminExportController.java` 的 `asyncGenerateReport()` 方法，在 `status="completed"` 和 `status="failed"` 时分别调用 `notificationService.send(EXPORT_COMPLETED/EXPORT_FAILED, taskId, ..., userId)`，`userId` 从导出任务记录中获取
- [x] T023 [P] [US3] 对账差异通知接入：定位 admin-server 中对账任务完成的逻辑（`AdminReconController` 或相关 Service），当任务状态变为 SUCCESS 且 `diffCount > 0` 时调用 `notificationService.sendToRole(RECON_DIFF, taskId, ...)`，接收人为对账运营角色
- [x] T024 [P] [US3] Webhook 回调失败通知接入：定位 admin-server 中记录商户回调失败的逻辑，当连续失败次数达到阈值（5 次）时调用 `notificationService.send(WEBHOOK_FAILURE, endpointId, ...)`，接收人为该商户对应运营
- [x] T025 [US3] 创建通知清理定时任务 `payflow-admin-server/src/main/java/com/payflow/admin/task/NotificationCleanupTask.java`：`@Scheduled(cron = "0 0 3 * * ?")` 每日凌晨 3 点删除 90 天前的已读通知（`read_status=1 AND created_at < now()-90d`）

**Checkpoint**: US3 完成后，触发任意业务事件 → `admin_notifications` 表可查到对应记录

---

## Phase 6: User Story 4 - 支付漏斗页展示真实多阶段转化数据 (Priority: P2)

**Goal**: 漏斗页从硬编码 0 升级为真实 ECharts 漏斗图，展示 CREATED→PAYING→PAID 和流失支路

**Independent Test**: 在 demo 库有 seed 订单数据，进入漏斗页 ① 不再出现"占位"字样；② 显示漏斗图；③ 各阶段数字与手工 SQL 一致

### Implementation for User Story 4

- [x] T026 [US4] 在 `OrderMapper` `payflow-admin-server/src/main/java/com/payflow/admin/mapper/cashier/OrderMapper.java` 新增漏斗聚合 SQL 方法 `funnelAggregate(LocalDateTime start, LocalDateTime end, String merchantId, String channel, List<String> merchantScopeIds)`，返回 `Map<String, Long>` 包含各 status 的 COUNT，使用 `@Select` 注解 + 条件拼接（`<script>` 动态 SQL）
- [x] T027 [US4] 创建 `FunnelService` `payflow-admin-server/src/main/java/com/payflow/admin/service/FunnelService.java`：注入 `OrderMapper`，实现 `queryFunnel(LocalDate dateFrom, LocalDate dateTo, String merchantId, String channel, List<String> merchantScopeIds)` 方法——① 调 `funnelAggregate` 获取原始计数；② 按 R-004 逻辑映射三阶段（CREATED=全部，PAYING=PAYING+PAID+SUCCESS，PAID=PAID+SUCCESS）；③ 计算相邻转化率和整体转化率（BigDecimal 保留 1 位）；④ 构建流失支路（FAILED/CLOSED/EXPIRED）；⑤ 返回 `FunnelResult` DTO
- [x] T028 [US4] 改造 `AdminInsightsController` `payflow-admin-server/src/main/java/com/payflow/admin/controller/AdminInsightsController.java`：注入 `FunnelService`，将 `funnel()` 方法改为接收 `dateFrom`、`dateTo`（默认最近 7 天）、`merchantId`、`channel` 参数，强制 `merchantScope` 过滤，返回 `{ code, message, data: FunnelResult }`；添加日期范围校验（dateFrom≤dateTo，跨度≤366 天）
- [x] T029 [US4] 改造 `payflow-admin-client/src/pages/admin/insights-funnel.vue`：替换 `<pre>` JSON 输出为 ECharts funnel 图——① 使用 `echarts/charts` 的 `FunnelChart` 系列渲染三阶段漏斗（CREATED→PAYING→PAID），每段显示数量和转化率标签；② 右侧或下方用 ECharts `BarChart` 展示流失支路（FAILED/CLOSED/EXPIRED）及占比；③ 空态时显示 `el-empty` 提示；④ 顶部显示整体转化率大字

**Checkpoint**: US4 完成后，漏斗页独立可测（默认最近 7 天全数据）

---

## Phase 7: User Story 5 - 支付漏斗支持时间范围、商户、渠道筛选 (Priority: P3)

**Goal**: 漏斗页增加筛选器（时间/商户/渠道），筛选变更后漏斗即时刷新

**Independent Test**: 选择"最近 30 天 + 商户 M100001 + 渠道 ALIPAY"后数据与手工 SQL 一致；非平台管理员商户下拉受限

### Implementation for User Story 5

- [x] T030 [US5] 在 `payflow-admin-client/src/pages/admin/insights-funnel.vue` 顶部添加筛选器区域：① 时间范围 `el-date-picker` + 快捷选项（今日/昨日/最近 7 天/最近 30 天/自定义）；② 商户下拉 `el-select`（复用已有 `getMerchantList` API，平台管理员可见全部，商户管理员受 `merchantScope` 限制隐藏或禁用无权商户）；③ 渠道下拉 `el-select`（WECHAT_PAY/ALIPAY/UNION_PAY/全部）；④ 任一筛选变更时以新参数重新请求 `getInsightsFunnel`，前端用 `AbortController` 取消上一次请求防止竞态
- [x] T031 [US5] 更新 `payflow-admin-client/src/api/admin.ts` 中 `getInsightsFunnel` 的参数类型定义，确保 `dateFrom`、`dateTo`、`merchantId`、`channel` 参数正确传递

**Checkpoint**: US5 完成后，筛选器联动漏斗图独立可测

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: 文档更新、安全加固、E2E 验证

- [x] T032 [P] 更新 `docs/CONTRACT_MATRIX.md`，追加通知中心 6 个端点和漏斗端点的前后端映射
- [x] T033 [P] 更新 `sql/schema/payflow_admin.sql` 确保全量 DDL 包含 `admin_notifications` 表（与 T002 对应的最终验证，确保 Flyway 迁移文件和全量 DDL 完全一致）
- [x] T034 [P] 在 `payflow-admin-client/src/i18n/` 中补全漏斗页相关翻译键（`funnel.title`、`funnel.stages.*`、`funnel.loss.*`、`funnel.filters.*`）
- [x] T035 安全加固审查：① 确认所有通知 Controller 端点均通过 `AdminRequestContext.merchantScope()` 过滤；② 确认漏斗端点的 `merchantId` 参数受 `assertMerchantAllowed` 校验；③ 通知 `link` 字段不含敏感信息；④ 分页 `size` 参数加 `@Max(100)` 校验
- [x] T036 按需使用 Playwright 验证关键前端交互：① 管理员登录 → 顶栏 Bell badge 显示 → 点击弹出面板 → 点击通知条目跳转；② 通知列表页 Tab 切换 + 筛选 + 批量已读；③ 漏斗页显示漏斗图 + 筛选器联动
- [x] T037 监控相关后台服务日志（admin-server），根据异常日志修复后重复验证至无阻断错误
- [x] T038 宪法合规检查 — 逐项验证 Constitution Check 9 项强制规则通过

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 无依赖 — 可立即开始
- **Foundational (Phase 2)**: 依赖 Phase 1（T001 迁移文件必须先创建） — **阻断所有用户故事**
- **US1 (Phase 3)**: 依赖 Foundational 完成（T008 NotificationService）
- **US2 (Phase 4)**: 依赖 Foundational + US1（T012 列表端点在 US1 中实现，US2 扩展 read-all/read-batch）
- **US3 (Phase 5)**: 依赖 Foundational 完成（T008 NotificationService）— 可与 US1/US2 并行
- **US4 (Phase 6)**: 依赖 Foundational 完成（T009 FunnelResult DTO） — 可与 US1/US2/US3 并行
- **US5 (Phase 7)**: 依赖 US4 完成（T029 漏斗页基础需先存在）
- **Polish (Phase 8)**: 依赖所有用户故事完成

### User Story Dependencies

- **US1 (P1)**: Foundational → 可开始
- **US2 (P1)**: Foundational + US1(T012) → 可开始（US2 的列表页依赖 US1 中已创建的列表端点）
- **US3 (P1)**: Foundational → 可开始 — **可与 US1/US2 并行**
- **US4 (P2)**: Foundational → 可开始 — **可与 US1/US2/US3 并行**
- **US5 (P3)**: US4 → 可开始

### Within Each User Story

- 后端 Entity/Mapper → Service → Controller
- 前端 API 封装 → 组件/页面
- 后端优先于前端（前端依赖 API）

### Module Boundary Order

```
payflow-admin-server（Entity → Mapper → Service → Controller）
  → payflow-admin-client（API → composables → components → pages）
```

---

## Parallel Example: User Story 1 + US3 + US4

```bash
# Foundational 完成后，以下三条线可并行：

# 线路 A: US1 — Bell badge + 预览面板
Task T011: "AdminNotificationController unread-count 端点"
Task T014: "useNotification.ts composable"       # [P] 可与 T015 并行
Task T015: "NotificationPopover.vue"              # [P] 可与 T014 并行
Task T016: "layout.vue Bell 改造"

# 线路 B: US3 — 事件源接入（5 个接入点全部 [P] 可并行）
Task T020: "退款审批通知"
Task T021: "流失预警超时通知"        # [P]
Task T022: "导出完成通知"            # [P]
Task T023: "对账差异通知"            # [P]
Task T024: "Webhook 失败通知"        # [P]

# 线路 C: US4 — 漏斗真实化
Task T026: "OrderMapper 聚合 SQL"
Task T027: "FunnelService"
Task T028: "AdminInsightsController 改造"
Task T029: "ECharts 漏斗图"
```

---

## Implementation Strategy

### MVP First (User Story 1 + 2 + 3)

1. 完成 Phase 1: Setup（T001-T003）
2. 完成 Phase 2: Foundational（T004-T010）— CRITICAL
3. 完成 Phase 3: US1（T011-T016）— Bell badge 上线
4. 完成 Phase 4: US2（T017-T019）— 通知列表页上线
5. 完成 Phase 5: US3（T020-T025）— 事件自动写入通知
6. **STOP and VALIDATE**: 独立测试通知中心完整闭环，按需 Playwright + 后台日志验证

### Incremental Delivery

1. Setup + Foundational → 基础就绪
2. + US1 + US2 + US3 → 通知中心完整可用（MVP）
3. + US4 → 漏斗图上线（默认 7 天全量数据）
4. + US5 → 漏斗筛选器上线（完整功能）
5. + Polish → 文档/安全/E2E → 交付

### Parallel Team Strategy

多开发者时：

1. 团队共同完成 Setup + Foundational
2. Foundational 完成后：
   - 开发者 A: US1 + US2（通知前端线）
   - 开发者 B: US3（事件源后端线）
   - 开发者 C: US4 + US5（漏斗线）
3. 各线独立完成和集成

---
