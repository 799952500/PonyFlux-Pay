# Feature Specification: 通知中心与支付漏斗真实化

**Feature Branch**: `012-notification-funnel`
**Created**: 2026-05-26
**Status**: Draft
**Input**: User description: "通知中心和支付漏斗目前还只是一个样子，并没有真正实现他的功能，需要你帮我规划"

## Constitution Compliance *(mandatory)*

在编写规范前，确认本功能涉及的宪法原则：

| 宪法原则 | 是否涉及 | 说明 |
|----------|----------|------|
| I. 模块边界纪律 | [x] 是 | 通知中心后端落在 `payflow-admin-server`；事件采集分布在 admin-server 各业务 Service；前端落在 `payflow-admin-client`。漏斗后端落在 `payflow-admin-server`，直查跨库的 `cashier_orders`（沿用现有 `OrderMapper` 二级数据源），不引入 cashier-server 改动。 |
| II. 支付渠道抽象 | [ ] 否 | 不新增支付渠道，不改 `PayStrategy` SPI；仅消费订单状态机已有事件 |
| III. 数据库分区 | [x] 是 | 通知中心新表落在 `payflow_admin` 库，前缀 `admin_notification_*`；不在 `payflow_cashier` 增表 |
| IV. API 响应规范 | [x] 是 | 新增 API 必须返回 `{ code, message, data }` 统一结构 |
| V. 密钥与配置安全 | [ ] 否 | 不涉及密钥或 JWT/CORS 变更 |
| 编码规范 | [x] 是 | Controller/Service/Mapper/Entity 分层、Lombok、统一异常 |
| 数据库访问规范 | [x] 是 | 新增表需 Flyway 迁移 + 主键自增 + 索引 + 商户隔离字段；分页统一 MyBatis-Plus Page |
| 安全编码规范 | [x] 是 | 通知列表必须按当前管理员的 `merchantScope` 过滤，复用 `AdminRequestContext.merchantScope` 数据隔离机制 |
| 测试规范 | [x] 是 | 通知写入与已读流程需集成测试；漏斗聚合 SQL 需断言；E2E 走 Playwright 覆盖顶栏 Bell + 漏斗页关键交互 |

> 涉及的原则将在 `plan.md` Constitution Check 中逐项检查。

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 顶栏 Bell 显示真实未读消息并可下钻查看 (Priority: P1)

管理员登录后台后，顶栏铃铛图标会显示未读消息总数 badge；点击铃铛弹出最近未读消息预览面板，可逐条点击跳转到对应业务详情页（如退款审批、流失预警、导出任务）。点击"查看全部"进入通知中心列表页。

**Why this priority**: 这是用户能立刻感知到"通知中心从样子变成真功能"的最核心入口。当前顶栏 badge 硬编码 0 且隐藏，是用户判断"只是一个样子"的直接证据。没有这个入口，其他通知能力无法被发现。

**Independent Test**: 在 demo 库中触发一笔退款进入待审批状态，验证 ① 管理员登录后顶栏 badge 显示 ≥ 1；② 点击铃铛弹出面板包含该退款条目；③ 点击条目跳转到退款审批页；④ 点击"标记已读"后 badge 减 1。整个流程不依赖漏斗功能即可独立验收。

**Acceptance Scenarios**:

1. **Given** 管理员当前有 3 条未读通知, **When** 登录后看到顶栏铃铛, **Then** badge 显示 3，铃铛 hover 显示"3 条未读消息"提示
2. **Given** 管理员点击铃铛, **When** 弹出面板, **Then** 显示最近 10 条未读消息（时间倒序），每条包含图标分类、标题、相对时间、链接
3. **Given** 面板中存在某条退款待审通知, **When** 点击该条, **Then** 自动标记为已读 + badge 减 1 + 跳转到 `/admin/refunds/{id}` 详情
4. **Given** 管理员点击面板底部"全部标记已读", **When** 操作完成, **Then** badge 变为 0，面板提示"暂无未读消息"
5. **Given** 一个商户管理员（非平台管理员）登录, **When** 查看未读, **Then** 只看到与其授权商户相关的通知（数据隔离）

---

### User Story 2 - 通知中心列表页支持分类筛选与已读管理 (Priority: P1)

管理员在通知中心列表页可以按"全部 / 未读 / 已读"和按业务类型（退款待审、流失预警、导出完成、对账差异、回调失败、系统公告）切换查看；支持分页浏览、单条已读、批量已读、按时间倒序排序。

**Why this priority**: 顶栏面板只能放最近 10 条，超过这个量必须有完整列表页；同时这是替换当前"暂无系统公告"占位页的核心交付。与 US1 并列 P1 是因为面板"查看全部"必须有目标页面。

**Independent Test**: 在 demo 库写入 30 条不同业务类型的通知，登录后台访问 `/admin/notifications`，验证 ① 默认显示全部，按时间倒序分页；② 切换"未读"Tab 仅剩未读；③ 选中多条勾选后点击"批量标记已读"成功；④ 切换业务类型筛选器后列表只剩匹配类型。

**Acceptance Scenarios**:

1. **Given** 通知中心存在 50 条通知（30 未读、20 已读）, **When** 进入列表页, **Then** 默认每页 20 条、显示"全部"Tab、未读数量徽章显示 30
2. **Given** 列表显示未读消息, **When** 切换到"已读"Tab, **Then** 列表刷新为 20 条已读
3. **Given** 在筛选器选择"流失预警"类型, **When** 列表刷新, **Then** 仅显示该类型通知，并且 URL query 携带 `type=churn_alert` 以便分享
4. **Given** 选中 5 条未读通知后点击"批量标记已读", **When** 提交成功, **Then** 这 5 条变为已读、顶栏 badge 同步减 5
5. **Given** 通知列表存在一条"导出完成"通知附下载链接, **When** 点击该通知, **Then** 自动标为已读，并直接触发下载/跳转

---

### User Story 3 - 业务事件自动写入通知，无需手动维护 (Priority: P1)

后端在以下事件发生时自动产生通知，确保用户能"被动"收到信息而无需轮询页面：

- 退款进入 `REFUNDING` 状态（等待管理员审批）
- 流失预警从待跟进进入"超时未跟进"（>48h）
- 导出任务完成（成功或失败）
- 对账任务发现差异（diff_count > 0）
- 商户回调连续失败达到阈值（如 5 次）

**Why this priority**: 没有事件源写入，前端再漂亮也"没有真消息"。这是把"样子"变成"真功能"的关键管道。与 US1/US2 同 P1 因为三者必须一起交付才能让用户感受到完整闭环。

**Independent Test**: 分别触发每类事件（用脚本或调接口），验证 ① `admin_notifications` 表新增对应记录；② 接收人按业务规则正确（如退款审批通知给有 `refund:approve` 权限的运营管理员）；③ 通知 `link` 字段指向正确业务详情 URL；④ 重复触发同一事件（同一退款）不会产生重复通知（幂等性）。

**Acceptance Scenarios**:

1. **Given** 商户创建一笔退款且金额需审批, **When** `cashier_refunds.status = REFUNDING` 写入完成, **Then** 在 5 秒内 `admin_notifications` 表为每个有审批权限的运营管理员各产生 1 条 `refund_approval` 类型通知
2. **Given** 流失预警 `admin_churn_alert` 创建后超过 48h 仍未处理, **When** `ChurnDetectionTask` 扫描到超时, **Then** 给该商户运营写入 `churn_overdue` 通知；同一 alert 不会因任务多次执行而重复通知
3. **Given** 管理员发起异步导出任务, **When** 导出文件生成完成, **Then** 给发起人写入 `export_completed` 通知，`link` 字段指向下载 URL
4. **Given** 对账任务跑完发现 5 笔差异, **When** 任务状态变为 SUCCESS 且 `diff_count > 0`, **Then** 给对账运营写入 `recon_diff` 通知，正文包含差异数与任务 ID
5. **Given** 某商户 webhook 端点连续 5 次失败, **When** 第 5 次失败完成, **Then** 写入 `webhook_failure` 通知（接收人=该商户对应运营）

---

### User Story 4 - 支付漏斗页展示真实多阶段转化数据 (Priority: P2)

平台管理员进入"支付漏斗"页面（`/admin/insights/funnel`），看到基于真实订单数据的多阶段漏斗图（订单创建 → 进入支付 → 支付成功），以及各阶段绝对数量、相邻阶段转化率、整体转化率。同时展示流失支路（失败 / 关闭 / 超时未支付）的数量分布。

**Why this priority**: 当前页面明确标"占位"，后端硬编码 0，用户进入就会失望。但相比通知中心，漏斗对日常运营的紧迫性更低（已有 Dashboard 单一"转化率"KPI 兜底），因此排 P2。

**Independent Test**: 在 demo 库（`cashier_orders` 已有覆盖 CREATED/PAYING/PAID/FAILED/CLOSED/EXPIRED 的 seed 数据）进入漏斗页，验证 ① 不再出现"占位"字样；② 显示漏斗图而非 `<pre>` JSON；③ 各阶段数字与手工 `SELECT status, COUNT(*) FROM cashier_orders WHERE created_at BETWEEN ... GROUP BY status` 一致；④ 整体转化率与 Dashboard 当日"转化率"KPI 在同口径下一致。

**Acceptance Scenarios**:

1. **Given** 最近 7 天有订单数据, **When** 默认打开漏斗页, **Then** 看到 3 段漏斗图（创建/进入支付/支付成功），每段含数量和上一阶段转化率
2. **Given** 漏斗图右侧显示流失支路, **When** 渲染完成, **Then** 流失支路展示 FAILED / CLOSED / EXPIRED 三类的数量与占总创建数的比例
3. **Given** 不存在任何订单的时间段, **When** 加载漏斗, **Then** 显示空态提示而非报错
4. **Given** 整体转化率为 73.5%, **When** 与 Dashboard 同日 KPI 对比, **Then** 在同时间口径与同商户口径下两者数值一致（允许四舍五入差异 ≤ 0.1pp）

---

### User Story 5 - 支付漏斗支持时间范围、商户、渠道筛选 (Priority: P3)

漏斗页顶部提供筛选器：时间范围（含快捷选项：今日/昨日/最近 7 天/最近 30 天/自定义）、商户（平台管理员可见，受授权商户范围限制）、渠道（微信/支付宝/银联）。筛选变更后漏斗图即时重新计算。

**Why this priority**: 没有筛选器漏斗也能用，但要从"能看"升级到"能分析"必须支持维度切片；属于完整体验的最后一公里。

**Independent Test**: 在筛选器选择"最近 30 天 + 商户 M100001 + 渠道 ALIPAY"，验证返回数据与同等条件的手工 SQL 聚合一致；切换商户后数据相应变化；非平台管理员看不到无授权的商户选项。

**Acceptance Scenarios**:

1. **Given** 默认时间范围"最近 7 天", **When** 切换为"最近 30 天", **Then** 漏斗各段数字相应增大（除非该商户/渠道近期无新订单）
2. **Given** 平台管理员在商户下拉看到 3 个商户, **When** 商户管理员（仅授权 M100001）登录, **Then** 商户下拉只能选自身授权商户或被禁用
3. **Given** 选择渠道 = "微信", **When** 漏斗刷新, **Then** 各阶段只统计 `channel=WECHAT_PAY` 的订单
4. **Given** 筛选条件全部清空, **When** 漏斗刷新, **Then** 恢复为默认（最近 7 天 + 全商户 + 全渠道）的视图

---

### Edge Cases

- 通知中心
  - 同一事件短时间内重复触发（如对账任务被人为重跑）→ 系统以业务键去重，同一业务对象 + 同一类型 24 小时内不重复写
  - 接收人不存在或被禁用 → 通知不写入，记录告警日志，不影响事件主流程
  - 用户标记已读后该通知再次被业务触发（如再次审批） → 视为新通知，写新记录
  - 单用户未读数过大（> 1000） → 顶栏 badge 显示 "999+"；列表仍可分页浏览
  - 删除/归档过期通知 → 90 天前已读通知由定时任务清理，未读通知不清理
- 支付漏斗
  - 选择的时间段跨大量数据（如最近 1 年）→ 后端必须能在合理时间内返回，超时返回错误提示而非空白
  - 用户切换筛选器期间快速多次操作 → 请求竞态由前端取消上一请求
  - 商户隔离冲突（非平台管理员尝试通过 URL 参数访问无授权商户漏斗）→ 返回 403，写审计日志
  - 阶段计数中存在跨阶段订单（如直接从 CREATED 跳到 PAID 跳过 PAYING） → 漏斗阶段统计按"曾经达到过该状态"而非"当前状态"，确保不漏算

## Requirements *(mandatory)*

### Functional Requirements

#### 通知中心 — 数据模型

- **FR-001**: 系统必须提供站内通知存储能力，每条通知至少包含：唯一 ID、接收人（管理员用户 ID）、商户隔离字段、业务类型枚举、标题、正文摘要、跳转链接、关联业务键（用于幂等）、已读状态、已读时间、创建时间
- **FR-002**: 系统必须支持按"业务对象 + 业务类型"在 24 小时内的幂等去重，避免同一事件多次触发产生重复通知
- **FR-003**: 系统必须支持给"角色范围"或"指定用户列表"投递通知（如所有有 `refund:approve` 权限的用户）

#### 通知中心 — 后端 API

- **FR-004**: 系统必须提供 `GET /api/v1/admin/notifications` 接口，返回当前登录管理员的通知分页列表，支持按 `read=true/false/all`、`type=<业务类型>`、`page/size` 过滤
- **FR-005**: 系统必须提供 `GET /api/v1/admin/notifications/unread-count` 接口，返回当前用户未读总数，用于顶栏 badge
- **FR-006**: 系统必须提供 `POST /api/v1/admin/notifications/{id}/read`、`POST /api/v1/admin/notifications/read-all`、`POST /api/v1/admin/notifications/read-batch` 三个已读接口
- **FR-007**: 所有通知接口必须强制按当前用户的 `merchantScope` 隔离数据（非平台管理员只能看其授权商户范围内通知）

#### 通知中心 — 事件源接入

- **FR-008**: 退款进入 `REFUNDING` 状态时，必须自动触发 `refund_approval` 类型通知写入
- **FR-009**: `ChurnDetectionTask` 检测到流失预警 >48h 未跟进时，必须自动写入 `churn_overdue` 通知（替换当前仅打 log.warn 的行为）
- **FR-010**: 异步导出任务完成（成功或失败）时，必须给发起人写入 `export_completed` / `export_failed` 通知，包含下载链接
- **FR-011**: 对账任务完成且 `diff_count > 0` 时，必须给对账运营角色写入 `recon_diff` 通知
- **FR-012**: 商户 webhook 端点连续失败次数达到阈值时，必须写入 `webhook_failure` 通知

#### 通知中心 — 前端

- **FR-013**: 管理后台顶栏铃铛必须实时显示当前用户未读总数（badge），未读为 0 时隐藏；轮询间隔 ≤ 60 秒
- **FR-014**: 点击铃铛必须弹出最近 10 条未读通知预览面板，含分类图标、标题、相对时间、跳转链接
- **FR-015**: 通知预览面板必须提供"全部标记已读"和"查看全部"两个动作按钮
- **FR-016**: 通知中心列表页必须替换当前占位实现，支持分类 Tab、业务类型筛选、单条已读、批量已读、分页

#### 支付漏斗 — 后端

- **FR-017**: 系统必须改造 `GET /api/v1/admin/insights/funnel` 接口，返回基于 `cashier_orders` 真实统计的多阶段数据（替换硬编码 0 和 `note` 字段）
- **FR-018**: 漏斗接口必须支持参数：`dateFrom`、`dateTo`（默认最近 7 天）、`merchantId`（可选）、`channel`（可选）
- **FR-019**: 漏斗每个阶段的统计口径必须明确文档化：CREATED 阶段统计所有在时间范围内创建的订单；PAYING 阶段统计曾达到过 PAYING/PAID/SUCCESS 状态的订单；PAID 阶段统计达到过 PAID/SUCCESS 的订单
- **FR-020**: 漏斗接口必须同时返回流失支路的统计（FAILED/CLOSED/EXPIRED 数量）
- **FR-021**: 漏斗接口必须强制按当前用户的 `merchantScope` 隔离

#### 支付漏斗 — 前端

- **FR-022**: 漏斗页必须移除"占位"标题与 `<pre>` JSON 输出，改用真实漏斗图（首选 ECharts funnel 图）
- **FR-023**: 漏斗页必须提供筛选器：时间范围（含快捷选项与自定义）、商户下拉、渠道下拉；筛选变更触发即时刷新
- **FR-024**: 漏斗页必须独立展示流失支路（侧栏柱状图或饼图），不与主漏斗混淆

#### 通用

- **FR-025**: 本特性新增的所有表必须通过 Flyway 迁移交付，迁移文件必须同时被 `sql/schema/` 全量 DDL 包含，避免再次出现"Flyway 历史已记录但表未创建"问题
- **FR-026**: 本特性新增 API 必须遵循统一响应规范 `{ code, message, data }`

### Key Entities *(include if feature involves data)*

- **Notification（站内通知）**: 表示一条投递给某个管理员用户的可读消息。关键属性：接收用户 ID、商户隔离 ID、业务类型枚举、标题、正文、跳转链接、关联业务键（用于幂等去重）、是否已读、已读时间、创建时间。与"商户回调记录"、"流失预警"、"导出任务"是**消费关系**——通知中心是这些业务事件的统一展示出口，不替代它们本身
- **NotificationType（通知业务类型）**: 枚举：`refund_approval`、`churn_overdue`、`export_completed`、`export_failed`、`recon_diff`、`webhook_failure`、`system_announcement`（预留）。每种类型决定图标、默认接收人范围、跳转目标
- **FunnelStageResult（漏斗阶段结果）**: 表示一次漏斗查询返回的某个阶段聚合结果。关键属性：阶段名（CREATED/PAYING/PAID）、订单数、上一阶段转化率、累计转化率。这不是持久化实体，是 API 返回的 DTO

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 触发任意一类业务事件（退款、流失、导出、对账、回调失败）后，对应通知必须在 **5 秒内**出现在接收人的顶栏 badge 计数中
- **SC-002**: 90% 的管理员从顶栏铃铛进入到目标业务详情页可在 **2 次点击**内完成（铃铛→条目）
- **SC-003**: 通知中心列表页加载（含分页查询）在 demo 数据量（≤ 1 万条/用户）下首屏渲染 **≤ 1.5 秒**
- **SC-004**: 支付漏斗页打开默认视图（最近 7 天 + 全商户 + 全渠道）首屏渲染 **≤ 2 秒**，漏斗图各阶段数字与对应 SQL 手工查询结果完全一致
- **SC-005**: 顶栏 badge 显示数字与后端 `unread-count` 接口返回值在 **任意时刻误差为 0**（轮询周期内的滞后不算误差，强制刷新后必须一致）
- **SC-006**: 同一业务事件（同一退款、同一流失预警）在 24 小时窗口内的重复触发 **不会产生重复通知**（幂等率 100%）
- **SC-007**: 数据隔离测试：非平台管理员通过任何前端入口或直接调用 API **均无法**看到/操作其授权范围外的通知或漏斗数据（违规请求返回 403 + 审计日志）
- **SC-008**: 漏斗"整体转化率"与 Dashboard 同时间口径"转化率"KPI 数值差 **≤ 0.1 个百分点**

## Assumptions

- **管理员用户体系沿用现有 `admin_sys_users`**：通知接收人为管理员用户（不是商户用户/收银台用户），借助 `admin_sys_user_roles` 角色映射决定"按角色投递"的接收范围
- **首版只做站内通知**：不实现邮件/短信/企业微信/飞书等外部渠道推送；外部推送作为后续特性独立规划
- **首版没有系统公告 CMS**：`system_announcement` 类型保留枚举位但不提供创建公告的管理界面；后端写入接口可手动调用（用于初期运营广播），UI 在后续迭代再加
- **通知数据驻留 90 天**：定时任务清理 90 天前已读通知；未读通知不自动清理
- **漏斗首版直查 `cashier_orders`**：不引入预聚合表（`admin_funnel_metrics`），靠现有 `idx_orders_created_at`/`idx_orders_status` 索引即可在 demo 数据量下达成 SC-004；如生产环境数据量增长导致超时，后续特性再引入预聚合
- **漏斗阶段定义为"曾经达到过"**：例如订单从 PAYING 跳到 PAID，PAYING 阶段计数仍包含它；防止因为状态前进而漏算中间阶段
- **漏斗不引入收银台前端埋点**：不在 `payflow-cashier-client` 增加曝光/点击事件；首版只用订单状态机数据
- **未读 badge 用轮询 60 秒**：不引入 WebSocket/SSE；用 HTTP 长轮询或定时拉取，避免引入新基础设施
- **不影响支付主链路**：所有事件源接入采用"事后写入"非阻塞模式（异步线程池或事务回调），任何通知写入失败不得影响业务主事务（退款审批、导出任务等）成功落库
- **依赖现有 `merchantScope` 数据隔离机制**：复用 `AdminRequestContext.merchantScope`（特性 008 已交付），不重复造轮子
- **复用现有 ECharts**：admin-client 已引入 ECharts 5.5，漏斗图直接使用 `funnel` 系列，不引入新可视化库
- **不修改 cashier-server 代码**：本特性后端改动全部落在 admin-server；漏斗对 cashier 数据采用既有的二级数据源跨库读
