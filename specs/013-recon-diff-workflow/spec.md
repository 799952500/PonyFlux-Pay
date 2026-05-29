# Feature Specification: 对账差异处置工作流升级

**Feature Branch**: `013-recon-diff-workflow`
**Created**: 2026-05-28
**Status**: Draft
**Input**: User description: "对现有系统进行业务方面的功能升级与优化（不扩展新模块），由 AI 给出方向 —— 选定：对账差异处置工作流升级（工单化、SLA 升级、自动归因、长尾追踪、报告订阅）"

## Constitution Compliance *(mandatory)*

在编写规范前，确认本功能涉及的宪法原则：

| 宪法原则 | 是否涉及 | 说明 |
|----------|----------|------|
| I. 模块边界纪律 | [x] 是 | 后端落在 `payflow-admin-server`（同库直连 `payflow_admin.recon_*`，不引入 `recon-server` 改动）；前端落在 `payflow-admin-client/src/pages/admin/reconcile/`，并复用 012 通知中心 |
| II. 支付渠道抽象 | [ ] 否 | 不新增支付渠道，不改 `PayStrategy` SPI；仅消费现有 `recon_diff` 数据与 `cashier_payments` 关联结果 |
| III. 数据库分区 | [x] 是 | 所有新表落在 `payflow_admin` 库，前缀沿用 `recon_` 命名空间（`recon_diff_assignment`、`recon_diff_sla_rule`、`recon_report_subscription`、`recon_report_snapshot`），不在 `payflow_cashier` 增表 |
| IV. API 响应规范 | [x] 是 | 新增 API 返回 `{ code, message, data }` 统一结构 |
| V. 密钥与配置安全 | [ ] 否 | 不涉及密钥、JWT、CORS 变更 |
| 编码规范 | [x] 是 | Controller/Service/Mapper/Entity 分层、Lombok、统一异常 |
| 数据库访问规范 | [x] 是 | 新表通过 Flyway 迁移、自增主键、含商户隔离字段、分页统一使用 MyBatis-Plus Page |
| 安全编码规范 | [x] 是 | 差异列表与工单分页强制按 `AdminRequestContext.merchantScope` 过滤；指派 / 升级 / 处置操作写入 `recon_handler_audit`；操作需具体权限（`recon:diff:assign`、`recon:diff:handle`、`recon:diff:escalate`、`recon:report:subscribe`） |
| 测试规范 | [x] 是 | 派单、SLA 升级、自动归因聚合 SQL、报告快照生成需集成测试；Playwright E2E 覆盖"我的工单"列表、超时升级提示、订阅周报全流程 |

> 涉及的原则将在 `plan.md` Constitution Check 中逐项检查。

## User Scenarios & Testing *(mandatory)*

> 当前对账系统现状（与本次升级的基线）：
> - `recon_diff` 已经能识别 4 类差异（`CHANNEL_ONLY` / `LOCAL_ONLY` / `AMOUNT_MISMATCH` / `STATUS_MISMATCH`），含 `handle_status`（PENDING / PROCESSED / IGNORED）
> - 差异通过 `POST /admin/reconcile/diffs/{id}/handle` 可单条标记处理
> - 任务完成发现差异时已通过通知中心（012）发送 `RECON_DIFF` 角色通知
> - **缺失**：差异无指派负责人、无 SLA 截止时间、无超时升级、无聚合归因视图、无长尾账龄追踪、无对账日 / 周报订阅
> 本次升级目标：把"差异列表"升级为"差异工单 + 处置看板"，把"一次性发现差异通知"升级为"主动 SLA 提醒 + 定时报告订阅"。

### User Story 1 - 差异工单化：每条差异有责任人、有状态机、有处置闭环 (Priority: P1)

财务对账员登录管理后台 → 进入"对账 → 我的差异工单"页 → 看到系统自动派给自己的所有 PENDING 差异；可以认领未指派差异、可以改派给同部门同事、可以在工单详情留下处置过程留言（不止最终结论）、可以将状态在 `ASSIGNED / IN_PROGRESS / PROCESSED / IGNORED` 间流转。所有状态变更进入 `recon_handler_audit` 留痕。

**Why this priority**: 这是把"差异 = 一行表格"升级为"差异 = 一张可问责工单"的最核心动作。没有责任人就没有 SLA、没有升级、没有报告。当前差异处置完全靠对账员"自己看一遍"，是合规审计最大的缺口。

**Independent Test**: 在 demo 库中跑一次对账，生成 3 条差异；以对账员 A 身份登录后台，验证 ① 进入"我的差异工单"页能看到自动派给自己的差异；② 认领一条 PENDING 未指派差异成功；③ 改派给对账员 B 后 A 列表中该差异消失、B 列表中出现；④ B 将状态从 ASSIGNED 推进到 IN_PROGRESS 并留言"已联系商户核实"；⑤ B 最终标记 PROCESSED 并填写处置说明；⑥ 整条工单的所有状态变更和留言在审计页可追溯。整个流程无需 SLA / 归因 / 报告即可独立验收。

**Acceptance Scenarios**:

1. **Given** 当日生成 5 条 PENDING 差异、系统中有 2 位拥有 `recon:diff:handle` 权限的对账员 A 和 B, **When** 系统执行自动派单, **Then** 差异按轮询规则分配（A 3 条、B 2 条），每条差异有 `assignee_id` 字段，原始 `handle_status` 同步推进到 `ASSIGNED`
2. **Given** 对账员 A 进入"我的差异工单"页, **When** 切换"我负责的 / 我创建的 / 全部"过滤器, **Then** 只看到与当前用户身份匹配且符合 `merchantScope` 数据隔离规则的工单
3. **Given** 一条差异处于 `ASSIGNED` 状态, **When** 当前负责人在工单详情点击"开始处理", **Then** 状态推进到 `IN_PROGRESS` 且 `recon_handler_audit` 写入一条 `START_PROGRESS` 审计
4. **Given** 一条差异处于 `IN_PROGRESS`, **When** 负责人点击"改派"并选择对账员 B, **Then** 工单转移到 B 的列表，A 列表中消失，审计写入 `REASSIGN A→B`，并通过通知中心向 B 发送一条 `RECON_DIFF_ASSIGNED` 通知
5. **Given** 一条差异最终被标记为 `PROCESSED`, **When** 提交处置, **Then** 必填"处置说明"（≥ 10 字符），审计完整记录，且后续不允许再次修改状态（除非 `RISK` / `SUPER_ADMIN` 角色介入回滚）
6. **Given** 商户管理员（非平台管理员）查看工单, **When** 加载列表, **Then** 仅看到自己授权商户范围内的差异工单

---

### User Story 2 - SLA 监控与超时自动升级：让"超时差异"在 D+1 之前必有人响应 (Priority: P1)

平台管理员在"对账 → SLA 规则"页配置不同差异类型的处置时效（默认值：`AMOUNT_MISMATCH` 24 小时、`STATUS_MISMATCH` 12 小时、`CHANNEL_ONLY` / `LOCAL_ONLY` 8 小时）。系统按差异生成时间自动计算每条工单的 `due_at`；当 SLA 剩余 ≤ 20% 时通过通知中心提醒负责人；当 SLA 已超时仍处于 PENDING/ASSIGNED/IN_PROGRESS 时自动升级 —— 工单标记 `ESCALATED`、追加 `escalated_to` 角色（默认 `recon:manage`）、通过通知中心向该角色全员发送 `RECON_DIFF_OVERDUE` 通知。

**Why this priority**: SLA 是合规与资金安全的底线。一笔金额不符差异如果 3 天没人看，财务核账就会失真，节假日可能直接挂账。当前完全没有 SLA 机制，仅靠对账员主动每天进系统查，节假日和值班交接是最大盲区。

**Independent Test**: ① 配置 `AMOUNT_MISMATCH` SLA = 1 分钟（用于演示）；② 生成 1 条该类型差异、指派给对账员 A；③ 等待 50 秒后系统应在通知中心向 A 发送"差异即将超时"提醒；④ 满 1 分钟后工单自动标记 `ESCALATED`，列表中标红，`recon:manage` 角色用户收到 `RECON_DIFF_OVERDUE` 通知；⑤ A 仍可继续处理但状态会附加 `is_overdue=true` 标识在审计日志。

**Acceptance Scenarios**:

1. **Given** SLA 规则配置 `AMOUNT_MISMATCH = 24h`, **When** 对账任务在 2026-05-28 10:00 产生一条该类型差异, **Then** 工单 `due_at` 自动写入 2026-05-29 10:00
2. **Given** 一条工单的剩余 SLA 时长 ≤ 总 SLA 的 20%（即"临近超时"）, **When** 调度任务每 5 分钟扫描一次, **Then** 仅向当前负责人发送一次 `RECON_DIFF_DUE_SOON` 通知（同一工单不重复发送）
3. **Given** 一条工单 `due_at` 已过、状态仍非 `PROCESSED` / `IGNORED`, **When** 升级调度任务执行, **Then** 工单状态变为 `ESCALATED`，`escalated_at` / `escalated_to` 写入，通知中心向 `escalated_to` 角色全员发送 `RECON_DIFF_OVERDUE` 通知；同一工单仅升级一次
4. **Given** 平台管理员在 SLA 规则页修改 `AMOUNT_MISMATCH` 从 24h 改为 12h, **When** 保存, **Then** 仅对修改时间之后**新生成**的差异生效，已有工单的 `due_at` 不变；规则修改写入 `recon_handler_audit` 全局审计
4a. **Given** SLA 规则被禁用（`enabled=false`）, **When** 该类型新差异生成, **Then** 工单不写入 `due_at`，列表中显示"无 SLA"，不参与超时升级
5. **Given** 一条已升级工单, **When** 负责人或上级最终处置完成, **Then** `handle_status` 进入终态，工单不再被升级调度扫描；本次"超时"事件在最终报告中可统计
6. **Given** 平台管理员关闭"SLA 自动升级"开关, **When** 扫描任务执行, **Then** 仍发送"临近超时"提醒，但不自动升级工单（用于演练 / 灾备场景）

---

### User Story 3 - 差异自动归因看板：把"散落的差异"升级为"可决策的趋势" (Priority: P2)

对账主管进入"对账 → 差异归因看板"页，可以选择时间范围（默认最近 30 天）查看：① 按渠道 × 差异类型的二维聚合矩阵（笔数 / 金额双指标）；② 差异笔数与金额的趋势折线图（按日 / 按周聚合切换）；③ TOP 5 差异源（按渠道账号、按商户、按差异类型分组）；④ 处置时效分布（平均处置时长、SLA 达成率、长尾比例）。看板支持下钻：点击矩阵中任一单元格直接跳到该过滤条件下的差异工单列表。

**Why this priority**: 当前差异处置是"逐条修复"思维，但 80% 的差异往往集中在 20% 的渠道账号 / 商户 / 时段。没有归因看板，对账主管无法判断"是哪条线在持续出问题"，只能被动救火。次于工单化（US1）和 SLA（US2），但比报告订阅（US5）优先级高 —— 因为有了看板，主管才能给出周会汇报数据。

**Independent Test**: ① 在 demo 库准备最近 30 天每天 5-10 条差异、覆盖 alipay / wxpay / unionpay 三个渠道；② 进入归因看板，验证矩阵中各 cell 数字与差异表 GROUP BY 结果一致；③ 切换"按日 / 按周"，趋势折线图正确切换；④ 点击 wxpay × `AMOUNT_MISMATCH` 单元格，跳转到工单列表且过滤器自动应用；⑤ TOP 5 列表与真实统计一致。

**Acceptance Scenarios**:

1. **Given** 最近 30 天共有 120 条差异分布在 3 个渠道、4 种差异类型, **When** 加载看板, **Then** 矩阵显示 3×4 = 12 个 cell，每个 cell 含笔数和金额，零值 cell 显示"-"而非 0
2. **Given** 当前角色受 `merchantScope` 限制（仅看商户 M1）, **When** 加载看板, **Then** 所有聚合数据仅基于 M1 名下差异，TOP 5 商户榜中不出现其他商户
3. **Given** 用户点击趋势图上"2026-05-25"这一天, **When** 触发下钻, **Then** 跳转到工单列表，过滤条件自动应用为"创建日期 = 2026-05-25"，列表仅显示当日新增差异
4. **Given** 用户切换看板时间范围为"最近 7 天", **When** 切换, **Then** 看板所有指标在 ≤ 2 秒内重新加载完成（数据量 ≤ 1 万条）
5. **Given** 看板处置时效分布显示"SLA 达成率 65%", **When** 用户 hover 该指标, **Then** 显示该比例的计算方式（达成数 / 总应处置数）与样本数（如"达成 78 / 总 120"）

---

### User Story 4 - 长尾差异追踪：把"跨日未处置"从盲区变成可控议题 (Priority: P2)

对账主管进入"对账 → 长尾差异追踪"页，按账龄 bucket 查看待处置差异（≤ 1 天 / 1-3 天 / 3-7 天 / 7-30 天 / 30 天以上）。系统自动识别"账龄 ≥ 7 天且仍非终态"的工单为"长尾差异"，在通知中心向 `recon:manage` 角色每日推送一次摘要（避免噪音）。长尾工单允许批量"挂账确认"（标记为 `ACCEPTED_LOSS`，作为终态之一，需要"挂账原因"留痕）。

**Why this priority**: 长尾差异是合规审计的最大风险点（监管要求 T+7 内处置完成）。当前系统对长尾完全没有追踪，多天前的差异在通知列表里很快被覆盖。次于 US1/US2 因为没有工单化和 SLA，长尾本身也不存在；但比 US5（报告订阅）优先 —— 报告里需要展示长尾指标。

**Independent Test**: ① demo 库准备账龄 0 / 2 / 5 / 10 / 35 天的差异各 1 条；② 进入长尾追踪页，验证 5 个 bucket 分别显示对应笔数；③ 选中账龄 10 天的工单，执行"挂账确认"并填写原因；④ 工单状态变为 `ACCEPTED_LOSS`、从长尾列表中消失、在工单详情可见挂账记录；⑤ `recon:manage` 角色用户登录后看到通知中心一条"今日长尾差异 4 条（含 3 条 ≥ 7 天）"摘要。

**Acceptance Scenarios**:

1. **Given** 系统中存在账龄 0/2/5/10/35 天的差异, **When** 加载长尾追踪页, **Then** 5 个 bucket 笔数为 1/1/1/1/1，总笔数 5
2. **Given** 一条账龄 10 天的差异, **When** 主管执行"批量挂账确认"且填写原因（≥ 20 字符）, **Then** 工单状态变为 `ACCEPTED_LOSS`，挂账原因写入审计，工单不再被升级扫描
3. **Given** `recon:manage` 角色每日 09:00 接收一次长尾摘要, **When** 当日有 ≥ 1 条 ≥ 7 天差异, **Then** 通知中心收到 `RECON_DIFF_LONG_TAIL` 摘要通知，包含"长尾笔数 / 长尾金额 / 最长账龄"
4. **Given** 全天无 ≥ 7 天差异, **When** 09:00 摘要任务执行, **Then** 不发送通知（避免空摘要）
5. **Given** 一条工单已 `ACCEPTED_LOSS`, **When** 重新加载差异归因看板（US3）, **Then** 该工单计入"挂账"统计 cell，不计入"待处置"

---

### User Story 5 - 对账日报 / 周报订阅：把"看板"升级为"定时推送" (Priority: P3)

管理员在"个人中心 → 报告订阅"或在归因看板顶部点击"订阅日报 / 周报"，选择频次（每日 09:00 / 每周一 09:00）与范围（仅自己负责的商户 / 全部授权范围）。订阅成功后，系统在指定时间生成"对账差异快照"并通过通知中心推送一条 `RECON_REPORT` 通知，附带可点击查看的报告详情页。报告内容包含：当期新增差异、当期处置数、当期 SLA 达成率、当期长尾笔数、TOP 3 差异源（与 US3 看板口径一致）。

**Why this priority**: 报告订阅是把"主动查看看板"升级为"被动接收洞察"的最后一公里。但优先级低于工单化 / SLA / 看板 / 长尾，因为没有那些基础能力，报告内容本身也无意义；本期可与上述能力并行设计但延后实现。

**Independent Test**: ① 对账主管订阅"每日 09:00 报告"，范围"全部授权商户"；② 在调度时点（或手动触发一次报告生成）后，验证通知中心收到一条 `RECON_REPORT` 通知；③ 点击通知跳转到当日报告详情页，显示完整六块数据（新增 / 处置 / 达成率 / 长尾 / TOP 3 渠道 / TOP 3 商户）；④ 取消订阅后下次调度时点不再收到通知。

**Acceptance Scenarios**:

1. **Given** 用户订阅"每日 09:00、范围=授权全部"且当日有 12 条新差异、处置 8 条、SLA 达成率 75%、长尾 2 条, **When** 09:00 调度执行, **Then** 通知中心生成一条 `RECON_REPORT` 通知，标题"昨日对账差异日报"，正文摘要包含核心指标
2. **Given** 同一用户同时订阅日报和周报, **When** 周一 09:00 同时触发, **Then** 收到两条独立通知（一条日报、一条周报），互不替代；快照分别落库
3. **Given** 用户取消订阅周报但保留日报, **When** 周一 09:00, **Then** 仅收到日报通知，不收到周报
4. **Given** 当日完全无新增差异、无处置, **When** 09:00 调度, **Then** 仍发送一条日报通知，标题"昨日对账无差异"（保留"系统在运行且无异常"的确定性信号）
5. **Given** 用户点击通知进入报告详情页, **When** 加载, **Then** 报告内容与归因看板（US3）口径完全一致（同一时间范围结果相同），不出现指标漂移

---

### Edge Cases

- **空对账日**：当某日无任何 `recon_diff` 生成时，长尾追踪页 5 个 bucket 全为 0，归因看板矩阵全为"-"，日报标题为"昨日对账无差异"
- **指派给已禁用账号**：负责人账号被禁用 / 离职时，系统自动将其名下未终态工单回收到"未指派池"，并向 `recon:manage` 角色发送"工单回收"通知
- **SLA 规则缺失**：差异类型未配置 SLA 时，工单 `due_at` 为空，不参与超时升级，但仍可被正常工单化与处置
- **并发指派**：同一条 PENDING 差异在两个浏览器 tab 中被同时认领时，第二个请求收到明确错误"该差异已被 {assignee} 认领，请刷新"，不允许覆盖
- **改派权限**：仅当前负责人或 `recon:manage` 角色可执行改派；其他对账员尝试改派他人工单时收到 403
- **挂账确认回滚**：`ACCEPTED_LOSS` 是终态之一，仅 `RISK` / `SUPER_ADMIN` 角色可回滚到 `PENDING`，且必须留下回滚原因
- **历史差异升级**：上线本特性时，存量 PENDING 差异在迁移脚本中按"创建时间 + 当前 SLA 规则"重算 `due_at`；若计算结果已过期则一次性升级到 `ESCALATED`，避免上线后立刻产生大规模升级噪音的方案：上线后第一周内禁用自动升级仅推送提醒，第二周再启用
- **报告订阅时区**：调度时间 `09:00` 按服务器配置时区（默认 `Asia/Shanghai`）触发；多时区不在本期范围内
- **大额差异预警**：当单条差异金额 ≥ 平台配置阈值（默认 10000 元）时，无论 SLA 是否到期，工单生成即触发一次 `RECON_DIFF_HIGH_VALUE` 通知给 `recon:manage` 角色
- **审计权限隔离**：负责人 A 改派给 B 后，A 仍可在"我创建的"过滤器下查看该工单状态变迁，但不能再操作

## Requirements *(mandatory)*

### Functional Requirements

#### 工单化（US1）

- **FR-001**: 系统必须为每条 `recon_diff` 维护扩展工单信息：`assignee_id`、`assigned_at`、`workflow_status`（`UNASSIGNED` / `ASSIGNED` / `IN_PROGRESS` / `PROCESSED` / `IGNORED` / `ESCALATED` / `ACCEPTED_LOSS`），存放在新表 `recon_diff_assignment`（与 `recon_diff` 一对一）
- **FR-002**: 系统必须支持差异自动派单：每次对账任务完成后，将该任务新增的 PENDING 差异按"轮询 + 商户范围匹配"规则分配给具备 `recon:diff:handle` 权限且当前活跃（未禁用、未离职）的对账员
- **FR-003**: 用户必须能够认领"未指派"差异（仅 `UNASSIGNED` 状态可认领，认领后状态进入 `ASSIGNED`）
- **FR-004**: 用户必须能够改派工单：当前负责人或 `recon:manage` 角色可改派给其他具备权限的活跃账号；改派后通过通知中心向新负责人发送 `RECON_DIFF_ASSIGNED` 通知
- **FR-005**: 工单状态机的合法迁移规则：`UNASSIGNED → ASSIGNED → IN_PROGRESS → {PROCESSED, IGNORED, ACCEPTED_LOSS}` 及任何状态 → `ESCALATED`（由系统自动触发）；非法迁移返回明确错误
- **FR-006**: 提交 `PROCESSED` / `IGNORED` / `ACCEPTED_LOSS` 必须填写处置说明（≥ 10 字符，挂账原因 ≥ 20 字符）
- **FR-007**: 所有工单状态变更、指派、改派、留言必须写入现有 `recon_handler_audit` 表，并扩展 `action` 字段支持新动作类型（`ASSIGN` / `REASSIGN` / `START_PROGRESS` / `ESCALATE` / `ACCEPT_LOSS` / `ROLLBACK` / `COMMENT`）
- **FR-008**: 工单列表 / 详情 / 操作必须按当前管理员的 `merchantScope` 数据隔离过滤，复用 `AdminRequestContext.merchantScope` 机制

#### SLA 与升级（US2）

- **FR-010**: 系统必须维护差异类型 SLA 规则表 `recon_diff_sla_rule`（字段：`diff_type`、`sla_hours`、`enabled`、`updated_by`、`updated_at`），平台管理员可在 SLA 规则页 CRUD
- **FR-011**: 工单创建时（含派单时）必须根据当前 SLA 规则计算并写入 `due_at`（`diff_create_time + sla_hours`）；规则禁用或不存在时 `due_at` 为空
- **FR-012**: 系统必须以不超过 5 分钟的调度周期扫描"剩余 SLA ≤ 20%"的工单，向负责人发送一次 `RECON_DIFF_DUE_SOON` 通知（同一工单去重，使用通知中心已有的 `biz_key` 幂等机制）
- **FR-013**: 系统必须以不超过 5 分钟的调度周期扫描"已超时且未终态"的工单：① 状态置 `ESCALATED`；② 写入 `escalated_at` / `escalated_to`（默认 `recon:manage`，规则可配）；③ 向 `escalated_to` 角色全员发送 `RECON_DIFF_OVERDUE` 通知；④ 同一工单仅升级一次
- **FR-014**: SLA 规则修改不溯及既往：已有工单 `due_at` 保持不变；规则修改本身写入审计
- **FR-015**: 平台管理员必须能够通过开关临时禁用"自动升级"（保留"临近超时提醒"），用于灾备演练或节假日特殊安排

#### 自动归因看板（US3）

- **FR-020**: 系统必须提供差异聚合查询接口，输入参数：时间范围、商户范围、渠道（可选）、差异类型（可选），输出二维矩阵（渠道 × 差异类型，含笔数 + 金额）、趋势序列（按日 / 按周）、TOP N 分组（渠道账号 / 商户 / 差异类型）、处置时效分布（avg / SLA 达成率 / 长尾比例）
- **FR-021**: 聚合查询必须强制应用 `merchantScope` 过滤；商户管理员仅能看到自己授权商户范围内的聚合数据，TOP N 列表中不泄露其他商户
- **FR-022**: 用户必须能够从看板下钻到工单列表：点击矩阵 cell 或趋势点跳转到差异工单列表页，过滤器自动应用（渠道 / 差异类型 / 时间范围）
- **FR-023**: 聚合查询对最近 30 天、数据量 ≤ 1 万条差异的场景必须在 ≤ 2 秒内返回；超过则使用预聚合快照表 `recon_aggregation_snapshot`（每日 02:00 重建一次）

#### 长尾差异追踪（US4）

- **FR-030**: 系统必须按账龄 bucket 划分待处置差异（≤ 1 天 / 1-3 天 / 3-7 天 / 7-30 天 / 30 天以上），账龄 = 当前时间 - 差异创建时间
- **FR-031**: 系统必须支持工单批量挂账（`ACCEPTED_LOSS`），单批 ≤ 50 条，挂账原因 ≥ 20 字符，挂账记录写入 `recon_handler_audit`
- **FR-032**: 系统必须以每日 09:00 调度向 `recon:manage` 角色推送一条 `RECON_DIFF_LONG_TAIL` 摘要通知，仅当存在账龄 ≥ 7 天且非终态的差异时发送；摘要内容含"长尾笔数 / 长尾金额 / 最长账龄"
- **FR-033**: 单条差异金额 ≥ 平台配置阈值（默认 10000 元，可在系统配置中调整）时，工单生成即触发一次 `RECON_DIFF_HIGH_VALUE` 通知给 `recon:manage` 角色（与 SLA 通知互不取代）

#### 报告订阅（US5）

- **FR-040**: 系统必须支持用户订阅日报 / 周报：新表 `recon_report_subscription`（`subscriber_id` / `report_type`（DAILY / WEEKLY）/ `scope`（OWNED / ALL_AUTHORIZED）/ `enabled` / `last_sent_at`）
- **FR-041**: 系统必须以每日 09:00（日报） / 每周一 09:00（周报）调度生成报告快照，落库到新表 `recon_report_snapshot`（`snapshot_id` / `period_start` / `period_end` / `subscriber_id` / `payload_json`），并通过通知中心向订阅者发送 `RECON_REPORT` 通知
- **FR-042**: 报告内容必须含六个标准块：当期新增差异（笔数 + 金额）、当期处置数（按 PROCESSED / IGNORED / ACCEPTED_LOSS 分组）、当期 SLA 达成率、当期长尾笔数、TOP 3 渠道差异源、TOP 3 商户差异源
- **FR-043**: 报告详情页与归因看板（US3）数据口径完全一致，使用相同的聚合服务，避免双口径漂移
- **FR-044**: 订阅 / 取消订阅必须实时生效（不需要重启调度）；订阅记录变更写入审计

#### 通用横切

- **FR-050**: 本特性新增的所有 API 路径前缀统一为 `/api/v1/admin/reconcile/diffs/*`、`/api/v1/admin/reconcile/sla-rules/*`、`/api/v1/admin/reconcile/aggregation/*`、`/api/v1/admin/reconcile/long-tail/*`、`/api/v1/admin/reconcile/reports/*`、`/api/v1/admin/reconcile/subscriptions/*`，必须返回 `{ code, message, data }` 统一结构
- **FR-051**: 所有新接口必须配置细粒度权限：`recon:diff:assign`（指派 / 改派 / 认领）、`recon:diff:handle`（状态推进 / 处置）、`recon:diff:escalate`（升级 / 回滚）、`recon:report:subscribe`（报告订阅）；现有 `recon:manage` 自动包含全部新权限
- **FR-052**: 业务错误码统一使用对账段 `7500-7599`，本特性新增码段建议 `7560-7589`（具体码值在 plan/contract 阶段细化）
- **FR-053**: 前端工单页 / 看板页 / 长尾页 / 订阅页必须接入双主题（参考 009-dual-theme-ui），不强制单独适配
- **FR-054**: 所有派单、SLA 升级、长尾摘要、报告推送通知必须复用 012 通知中心 `NotificationService`，**不引入独立的消息队列或邮件通道**

### Key Entities *(include if feature involves data)*

- **ReconDiffAssignment（差异工单扩展）**：与 `recon_diff` 一对一关联，承载工单语义。关键属性：`diff_id`、`assignee_id`、`workflow_status`、`assigned_at`、`due_at`、`escalated_at`、`escalated_to`、`processed_at`、`merchant_id`（用于隔离）
- **ReconDiffSlaRule（SLA 规则）**：按差异类型配置处置时效。关键属性：`diff_type`（唯一）、`sla_hours`、`enabled`、`escalate_to_role`、`updated_by`、`updated_at`
- **ReconReportSubscription（报告订阅）**：用户订阅日 / 周报。关键属性：`subscriber_id`、`report_type`（DAILY/WEEKLY）、`scope`（OWNED/ALL_AUTHORIZED）、`enabled`、`last_sent_at`
- **ReconReportSnapshot（报告快照）**：每次推送的报告内容快照。关键属性：`snapshot_id`、`subscriber_id`、`report_type`、`period_start`、`period_end`、`payload_json`、`generated_at`
- **ReconAggregationSnapshot（归因预聚合）**：按日 × 渠道 × 差异类型预聚合，用于看板加速。关键属性：`bill_date`、`channel`、`diff_type`、`merchant_id`、`diff_count`、`diff_amount`
- **ReconHandlerAudit（处理审计，已存在表扩展）**：扩展 `action` 字段支持新动作（`ASSIGN` / `REASSIGN` / `START_PROGRESS` / `ESCALATE` / `ACCEPT_LOSS` / `ROLLBACK` / `COMMENT`）
- **Notification（通知中心实体，已存在）**：复用 012 通知中心，新增 7 种通知类型：`RECON_DIFF_ASSIGNED` / `RECON_DIFF_DUE_SOON` / `RECON_DIFF_OVERDUE` / `RECON_DIFF_LONG_TAIL` / `RECON_DIFF_HIGH_VALUE` / `RECON_REPORT` / `RECON_DIFF_RECYCLED`

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 上线后 30 天内，所有 PENDING 差异**100%** 在生成后 5 分钟内被自动指派至活跃对账员（基准：当前 0%）
- **SC-002**: 上线后 30 天内，AMOUNT_MISMATCH 类差异在 24 小时内进入终态（PROCESSED / IGNORED / ACCEPTED_LOSS）的比例 ≥ **80%**（基准：当前未度量，估算 ≤ 30%）
- **SC-003**: 上线后 30 天内，账龄 ≥ 7 天的长尾差异数量较上线前同口径下降 ≥ **70%**
- **SC-004**: 任一具备 `recon:manage` 权限的用户从登录到看到"当日 SLA 达成率"完整数字的耗时 ≤ **30 秒**（含登录 + 进入看板 + 看板加载）
- **SC-005**: 差异归因看板对最近 30 天、≤ 1 万条差异的聚合查询响应时间 ≤ **2 秒**（P95）
- **SC-006**: 上线 14 天后，至少 **80%** 的活跃对账员（在最近 30 天内至少处置过 1 条差异的账号）订阅了对账日报或周报
- **SC-007**: 上线 30 天后，"差异处理审计完整率"（即每条进入终态的工单在 `recon_handler_audit` 中可追溯至少 2 条审计动作）≥ **99%**
- **SC-008**: 上线后 90 天内，因"对账差异未及时处置"产生的合规 / 客诉工单数量较前 90 天下降 ≥ **50%**（由运营 / 客服团队按既有事件分类统计）

## Assumptions

- **基线假设**：现有 `recon_diff` / `recon_handler_audit` / `recon_task` 表结构与字段保持不变，本特性通过新增表 `recon_diff_assignment` / `recon_diff_sla_rule` / `recon_report_subscription` / `recon_report_snapshot` / `recon_aggregation_snapshot` 实现扩展；不删除任何现有字段
- **数据隔离**：复用 008 / 006 已经实现的商户隔离机制（`AdminRequestContext.merchantScope`），不引入新的隔离模型
- **通知通道**：仅使用 012 通知中心的站内消息；本期不引入邮件 / 短信 / 飞书机器人外推通道（这些视作扩展，留待后续特性）
- **调度依赖**：派单、SLA 扫描、长尾摘要、报告生成统一使用 admin-server 内置 `@Scheduled`（依赖 Spring 调度），不强制要求 XXL-Job；如未来切换 XXL-Job 仅需替换调度入口
- **派单规则范围**：本期采用"轮询 + 商户范围匹配"，不支持按工作量自动均衡、按差异类型专长匹配等高级策略；列为后续扩展
- **报告频次**：本期仅支持每日 09:00 / 每周一 09:00 两种固定频次；自定义频次列为后续扩展
- **报告投递时区**：默认 `Asia/Shanghai`，本期不支持多时区订阅
- **大额阈值**：金额阈值默认 10000 元，存放在系统配置 `recon.diff.high-value-threshold-cents`，运维可调
- **历史数据迁移**：上线时存量 PENDING 差异（如有）通过迁移脚本初始化为"未指派"状态；上线后第一周仅推送"临近超时"提醒不自动升级，第二周再启用自动升级
- **演示库准备**：`sql/seed/payflow_admin_seed.sql` 需补充演示用 SLA 规则与示例订阅，保证安装演示库后立刻可演示
- **测试范围**：单元测试覆盖派单 / 状态机 / SLA 计算的纯函数；集成测试覆盖派单 → 升级 → 报告快照三条链路；Playwright E2E 覆盖"我的工单"、"长尾追踪"、"订阅周报"三个核心场景
- **不在本期**：差异自动修复（自动写回 `cashier_payments`、自动补单 / 关单）、跨期对账（隔月对账）、AI 辅助归因 —— 这些都是扩展而非升级，明确不在本特性范围
