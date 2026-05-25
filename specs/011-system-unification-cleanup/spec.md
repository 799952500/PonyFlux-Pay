# Feature Specification: 系统统一性升级与代码瘦身

**Feature Branch**: `011-system-unification-cleanup`
**Created**: 2026-05-23
**Status**: Draft
**Input**: User description: "本次主要是对系统进行统一性升级，升级包含数据库表名称的统一管理，所有收银台的表都要以cashier_ +  具体业务命名，例如： cashier_channel_accounts 后台管理系统则是admin_  +  具体业务命名，例如admin_users，另外是去除废弃的代码，不管是前端还是后端，如果扫描到没有使用或者因为迭代被替代的方法，都要删除掉，另外初始化脚本也要整理到一个脚本里面，还有演示数据也是。除此之外，还有哪些需要优化，你也可以提出你的看法"

## Constitution Compliance *(mandatory)*

在编写规范前，确认本功能涉及的宪法原则：

| 宪法原则 | 是否涉及 | 说明 |
|----------|----------|------|
| I. 模块边界纪律 | 是 | 跨所有 Maven 模块的代码瘦身与文件归集，需确保各模块职责不被破坏。 |
| II. 支付渠道抽象 | 是 | 渠道相关表与代码涉及重命名，必须保持 `PayStrategy` / Locator 抽象不被反推为硬编码。 |
| III. 数据库分区 | 是 | 本规范的核心目标即统一表前缀，强制对齐宪法 III 的 `admin_` / `cashier_` / `recon_` 约定。 |
| IV. API 响应规范 | 是 | 删除被替代代码可能涉及多套响应包装类的合并与统一。 |
| V. 密钥与配置安全 | 否 | 不新增密钥/配置项；表重命名不改变加密字段语义。 |
| 编码规范 | 是 | 死代码清理涉及类、方法、命名一致性。 |
| 数据库访问规范 | 是 | 表重命名涉及实体 `@TableName`、Mapper、XML、SQL 全链路同步。 |
| 安全编码规范 | 是 | 重命名期间必须避免遗漏脱敏字段或破坏既有审计链路。 |
| 测试规范 | 是 | 所有受影响模块需在重构后回归通过，含按需 Playwright 端到端验证与后台日志闭环。 |

> 涉及的原则将在 `plan.md` Constitution Check 中逐项检查。

## Clarifications

### Session 2026-05-23

- Q: `recon_*` 前缀是否保留为第三合法前缀？ → A: **保留**。`payflow_admin` 内对账域表继续使用 `recon_*`，不强制收敛为 `admin_recon_*`。
- Q: 同名表在 admin/cashier 两库重复时的权威归属？ → A: **默认策略**——写入方所属业务域优先：事务性写入归 `payflow_cashier`（`cashier_*`），配置性写入归 `payflow_admin`（`admin_*`）；确定归属后删除另一库副本。
- Q: 生产环境表重命名策略？ → A: **当前无生产系统**，不实施 `RENAME TABLE` 类在线迁移；表前缀统一通过直接修订 `sql/schema/` 理想终态 DDL、同步应用代码与 Flyway baseline/终态迁移完成；新环境一律从正确表名建表。
- Q: `sys_*` 表合并到 `admin_*` 的目标命名？ → A: **默认策略**——使用 `admin_sys_*` 语义前缀（如 `admin_sys_users`、`admin_sys_roles`、`admin_sys_menus`），避免与既有 `admin_users`（运营用户）等业务表名冲突。

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 数据库表前缀全面对齐宪法约定 (Priority: P1)

作为系统架构守护者与后续开发者，我需要 `payflow_admin` 与 `payflow_cashier` 两库内每一张业务表的名称都严格遵循「数据库 ↔ 业务前缀」约定：所有运营/管理域表使用 `admin_` 前缀，所有收银交易域表使用 `cashier_` 前缀，对账域表**保留**既有 `recon_` 前缀。当前已存在 `sys_users`、`sys_roles`、`sys_menus`、`risk_rules`、`merchant_application`、`merchant_open_app`、`webhook_delivery_log`、`payment_link`、以及位于 admin 库却被命名为 `cashier_risk_blacklist` 等违反约定的表，必须在 **schema 终态与代码中直接改为正确表名**（非在线 `RENAME TABLE`）；`sys_*` 统一为 `admin_sys_*`（如 `admin_sys_users`）；位于 cashier 库的 `merchant_webhook_endpoint`、`webhook_delivery_log` 等按归属业务域纠正为 `cashier_*` 或迁回 admin 域。

**Why this priority**: 数据库分区是宪法 III 的强制原则，违反此原则会持续制造模块边界破口，影响后续每一个新功能的实体命名判断与跨库 JOIN 风险评估。该项是本次升级的核心硬性目标。

**Independent Test**: 在执行重构后的全量初始化脚本到一个**空** MySQL 实例上，运行表前缀校验 SQL，断言「`payflow_admin` 中所有表名以 `admin_` 或 `recon_` 开头」「`payflow_cashier` 中所有表名以 `cashier_` 开头」，违规数 = 0；再启动三个后端服务，健康检查与登录/下单冒烟通过即确认表名统一未破坏运行链路（无需验证生产侧 `RENAME TABLE` 路径）。

**Acceptance Scenarios**:

1. **Given** 执行新的一键初始化脚本到空数据库后，**When** 运行表前缀校验 SQL，**Then** 校验结果为零违规，且每张表都能与所属业务域口径对应。
2. **Given** schema 与代码已对齐且后端服务启动，**When** 触发"创建订单 → 支付 → 回调 → 退款 → 后台查询"端到端冒烟用例，**Then** 全链路通过，无 `Table doesn't exist`、`Unknown column` 类异常；后台查询、对账模块、RBAC 登录均工作正常。
3. **Given** 升级前违规旧表名已废弃，**When** 在应用代码（Java/Vue/TypeScript）中全局搜索旧表名，**Then** 零命中；仅在 `sql/migrations/` 历史归档（如有）中可保留追溯。
4. **Given** 表名变更涉及实体类、Mapper、XML、原生 SQL，**When** 编译并运行各模块测试，**Then** 全部通过；`@TableName` 与新表名一致。

---

### User Story 2 - 一键完成全量初始化与演示数据加载 (Priority: P1)

作为新加入的开发者或演示环境部署者，我需要一条命令就能从零搭建完整的可演示环境：建库 → 建表 → 灌入演示数据 → 应用所有需要的增量与种子，全程无需手工挑选脚本顺序、无需理解迁移文件归档语义。当前 `scripts/install_demo_db.py` 中硬编码了一个跨 `sql/schema/`、`sql/seed/`、`sql/migrations/` 的混合文件列表（共 14 个文件，且要求人工知道哪些迁移与演示相关），违背了"初始化脚本要整理到一个脚本里"和"演示数据整理到一个脚本"的目标。

**Why this priority**: 一键安装是新人 onboarding 与演示环境的命脉；当前混合脚本与多份重复定义（`sql/schema/`、`sql/admin/`、`sql/cashier/`、`sql/full-reseed-payflow-demo.sql`、各 server 内的 `src/main/resources/sql/*.sql`、`src/main/resources/db/migration/*.sql`）让新成员极易选错入口、漏跑迁移，直接影响交付效率。

**Independent Test**: 在干净的 MySQL 实例上仅执行 `python scripts/install_demo_db.py`（或等价的单一入口）即可获得：可登录的后台账号、可下单的商户与渠道、覆盖所有演示页面的种子数据；运行业内冒烟用例（登录 → 仪表盘 → 订单 → 退款 → 对账 → RBAC）全部成功。

**Acceptance Scenarios**:

1. **Given** 一个空的 MySQL 实例与项目根目录，**When** 执行单一一键安装命令，**Then** 终端输出每个阶段（建库、Schema、Seed）的成功日志，结束后两库表与演示数据齐全，可登录 `admin/admin123`。
2. **Given** 已经成功安装一次的环境，**When** 再次执行同一命令（幂等场景），**Then** 命令成功完成或给出明确的"已就绪"提示，不会因重复建表/重复主键而中断。
3. **Given** 一名新人按 README 操作，**When** 在文档中查找"如何拉起演示环境"，**Then** 仅存在一条权威入口与一份演示数据 SQL，不会出现"`full-reseed-payflow-demo.sql` 已废弃"等需要人工辨识的旧脚本指引。
4. **Given** 安装完成后启动三个后端服务，**When** 跑核心冒烟（订单创建、支付回调、退款、对账任务）+ 后台关键页面（仪表盘、订单、退款、对账、商户、RBAC、风控），**Then** 全部成功且后台日志无 ERROR 阻断错误。

---

### User Story 3 - 清理被迭代替代的死代码 (Priority: P1)

作为维护者，我需要把已被新版替代但仍残留的旧代码（包含但不限于 `CashierInternalRefundClient.java` 已被 `CashierInternalClient` 取代后留下的引用、`OrderDetailDrawer.vue` 与 `OrderDetailPanel.vue` 同时存在但仅一个被使用、`sql/full-reseed-payflow-demo.sql`、`sql/admin/*.sql`/`sql/cashier/*.sql` 旧位置、各 server 的 `src/main/resources/sql/*.sql` 中已被 Flyway 迁移取代的脚本、根目录的 `check-roles.ps1`/`fix-roles.ps1`/`fix_roles.py`/`update_sql.py`/`scripts/fix_flyway_v8.py`/`scripts/record_flyway_v6.py`/`scripts/check_v6_schema.py`/`scripts/apply_v6_columns.py`/`scripts/apply_v6_migration.py` 等一次性修复脚本）从仓库中移除；同时清理前后端中无引用的方法、类、组件、TypeScript 类型、API 客户端方法。死代码不仅扩大维护面，还会让新人误以为旧路径仍然有效。

**Why this priority**: 死代码持续制造误导与构建噪音，且与"统一性升级"目标互为前提——只有先把旧分支清掉，统一后的命名与脚本才不会被旧代码继续污染。

**Independent Test**: 选取若干已知死代码线索（例如：被替代的 `*Client`、被替换的旧 Vue 组件、被废弃的 SQL 脚本、根目录一次性 PowerShell/Python 脚本）执行删除并跑全量构建（`mvn -B clean package`、`npm run build`、Flyway 迁移、`install_demo_db.py`、Playwright 关键路径用例），全绿即视为该项独立通过。

**Acceptance Scenarios**:

1. **Given** 删除一组被认定为死代码的文件/方法，**When** 执行全量后端构建与单元测试，**Then** 编译通过、测试不退化、无新报错。
2. **Given** 删除前端中无引用的 Vue 组件与 TS 函数，**When** 执行 `npm run build` 与按需 Playwright 关键路径，**Then** 构建无未解析引用，关键 UI 路径正常。
3. **Given** 删除项目根目录与 `scripts/` 下被认定为一次性修复的 PowerShell/Python 文件，**When** 一键安装与平时开发命令执行，**Then** 不依赖被删脚本即可完成。
4. **Given** 任何在升级前可达的对外接口与运行时入口（HTTP API、回调 URL、菜单按钮），**When** 升级后回归测试，**Then** 行为保持一致，无回归。

---

### User Story 4 - 一致的初始化与演示数据来源 (Priority: P2)

作为运维/演示者，我需要演示数据具有单一权威来源：演示账号、商户、订单、退款、对账记录、RBAC 角色与按钮权限、菜单结构、风控规则、回调样本，全部由「一份演示种子（按业务域分文件，但以一个安装入口编排）」生成，避免在 `sql/seed/`、`sql/admin/`、`sql/cashier/`、各 server 的 `src/main/resources/sql/*-data.sql`、历史迁移内嵌的 INSERT 之间互相覆盖或漂移。

**Why this priority**: 演示数据漂移会让"在我机器上能复现"的问题难以定位；多份种子也增加了改样例数据的成本。此项依赖 P1 的脚本整合先落地。

**Independent Test**: 在一键安装后比对预期演示口径（README 中的"演示数据覆盖的页面"清单）与实际数据库内容，逐项断言记录数与关键状态值；运行后台关键页面 Playwright 验证，每一项口径均可在 UI 中复现。

**Acceptance Scenarios**:

1. **Given** 一键安装完成，**When** 查询 `cashier_orders`、`cashier_refunds`、`recon_task`、`admin_audit_logs`、`admin_dashboard_metrics` 等关键演示表，**Then** 记录数与状态分布与文档声明一致。
2. **Given** 演示账号 `admin/admin123`、`finance_demo/admin123`、`risk_demo/admin123`，**When** 在后台依次登录，**Then** 各账号见到的数据范围与权限按钮与文档一致。
3. **Given** 重新执行一键安装（覆盖式），**When** 再次比对演示数据，**Then** 与首次安装结果一致，无累计/漂移。

---

### User Story 5 - 单一权威 schema 与可追溯迁移 (Priority: P2)

作为生产升级负责人，我需要 `sql/schema/` 始终代表"理想最终态"，可被新环境安全使用；增量迁移仅留一份权威路径（推荐使用各 server 内 `src/main/resources/db/migration/` 的 Flyway 路径），并且 `sql/migrations/` 与 `payflow-admin-server/src/main/resources/sql/admin-alter-*.sql` 等多份重复的脚本要么合并、要么明确标注归档。

**Why this priority**: 历史迁移与 schema 漂移是这次升级的根因之一；保留单一权威路径才能防止下次再次发散。

**Independent Test**: 对一个空库执行新的初始化脚本得到 schema A，再对一个老库逐条应用 Flyway 迁移得到 schema B；比较两者结构（表、列、索引、约束）应为完全一致。

**Acceptance Scenarios**:

1. **Given** 全新空库使用初始化脚本搭建，**When** 与"先建旧库再跑全部 Flyway 迁移"的结果做结构对比，**Then** 两者表结构一致（含字段、索引、外键、字符集）。
2. **Given** 仓库中存在 `sql/admin/`、`sql/cashier/`、`sql/full-reseed-payflow-demo.sql`、`sql/install_demo.sql`、各 server 的 `src/main/resources/sql/*.sql` 等旧路径，**When** 升级完成后审视目录，**Then** 这些旧路径或被合并、或被明确删除、或被标注为"历史归档（请勿使用）"，不得作为可执行入口存在。

---

### User Story 6 - 配套优化：发现并提出额外改进 (Priority: P3)

作为本次升级的副产品，需要在重构过程中识别并提出可立即落地的额外优化项，包括但不限于：移除被 git 跟踪的运行期产物（如 `dump.rdb`）、在 `.gitignore` 中加入构建产物（如 `payflow-admin-server/target/`）、清理未使用的 Maven/npm 依赖、统一 API 响应包装类（避免 `Map<String, Object>` 与潜在自创包装类并存）、检查 `docs/CONTRACT_MATRIX.md` 与实际 API 一致性、将一次性 ad-hoc 脚本（根目录 `*.ps1`、`update_sql.py` 等）从仓库根剔除或归档。这些项以"清单 + 评估 + 可选执行"的形式产出，不在本次必须全部完成，但应在本次留下可追踪记录。

**Why this priority**: 本次是清理窗口期，配套发现的小问题"顺手做掉"成本低；但与核心目标解耦，作为 P3 不阻塞核心交付。

**Independent Test**: 输出一份"附加优化建议清单"作为 spec 附件或 `plan.md` 子章节，每条包含：现状 / 风险 / 建议处理 / 是否本次执行。

**Acceptance Scenarios**:

1. **Given** 升级 PR 接近完成，**When** 审视仓库根、`scripts/`、`docs/`、`payflow-*-server/target/`、`docs/optimization-full-report.md` 等位置，**Then** 至少识别出 5 项额外优化建议并记录处理决策。
2. **Given** 决定本次执行的额外优化项，**When** 合入 PR，**Then** 改动有清晰的最小化原则，不与核心三目标混淆。

---

### Edge Cases

- **无生产环境**：本地/演示库若仍含旧表名，推荐**清库后**执行一键安装，而非对已有库执行 `RENAME TABLE`；若团队需保留本地数据，可在 `plan.md` 中提供可选的一次性迁移脚本，但不作为本规范验收路径。
- 同一张概念表若在 admin 与 cashier 两库重复（如 `merchant_webhook_endpoint`、`webhook_delivery_log`），按**写入方业务域**确定唯一权威库（事务性 → cashier，配置性 → admin），删除另一库副本并统一表名前缀。
- `recon_*` 前缀位于 admin 库，**保留**为合法第三前缀；语义不属于对账的表（如路由决策日志）应归入 `admin_*` 而非 `recon_*`。
- `sys_*` 更名为 `admin_sys_*` 时，须同步更新 RBAC 相关实体、种子数据与菜单权限引用，且不与 `admin_users`（运营用户表）混淆。
- 死代码判断必须考虑反射调用、SpEL/EL、AOP 切面、动态代理、消息消费者、定时任务（XXL-Job 注解）、前端动态 import、模板字符串拼接的菜单 path 等"静态扫描看似无引用"的场景；删除前需有显式判定标准。
- 一键安装脚本需考虑两种环境：①完全空库（首次）；②已有部分表（重复执行）。对②应当幂等或给出明确指引，不得静默失败或部分写入。
- 演示数据中的商户号、订单号、用户登录信息须与 `docs/` 中文档声明保持口径一致；改动需同步文档，避免文档漂移。
- Flyway：因无生产库，可将 baseline/终态迁移与 `sql/schema/` 对齐（合并或刷新 baseline），不必为旧表名新增 `RENAME TABLE` 迁移；版本号不回收复用。
- 前端国际化（i18n）资源中残留的旧菜单/按钮 key 也属死代码，需一并清理。

## Requirements *(mandatory)*

### Functional Requirements

#### 数据库表前缀对齐

- **FR-001**: 系统必须确保 `payflow_admin` 内所有业务表以 `admin_` 或 `recon_` 开头；`sys_*` 统一为 `admin_sys_*`；禁止无前缀的 `risk_*`、`merchant_*`、`webhook_*`、`payment_link` 等；admin 库中误用 `cashier_*` 的表改为 `admin_*` 语义名。
- **FR-002**: 系统必须确保 `payflow_cashier` 内所有业务表以 `cashier_` 开头；非约定表按归属改为 `cashier_*` 或迁回 admin 域并改名。
- **FR-003**: 两库重复的概念表须按**写入方业务域**确定唯一权威库（事务性 → cashier，配置性 → admin），删除另一库副本。
- **FR-004**: 表名变更须同步更新实体 `@TableName`、Mapper XML、原生 SQL 及配置中的表名常量。
- **FR-005**: **无生产环境**：表前缀统一通过直接修订 `sql/schema/payflow_admin.sql`、`sql/schema/payflow_cashier.sql` 终态 DDL 完成；同步更新应用代码；Flyway 与 schema 终态对齐（可合并/刷新 baseline），**不要求** `RENAME TABLE` 在线迁移。
- **FR-006**: 完成后应用代码中对违规旧表名引用为零；`sql/migrations/` 历史归档可保留旧名追溯。

#### 一键初始化与演示数据

- **FR-007**: 系统必须提供单一权威入口（如 `python scripts/install_demo_db.py` 或等价单命令）完成"建库 → 建表 → 灌演示数据"的全流程，无需用户人工选择哪些迁移文件参与演示。
- **FR-008**: 一键安装脚本必须只引用 `sql/schema/` 与 `sql/seed/` 两个目录下的文件，禁止在文件清单中混入 `sql/migrations/` 路径下的迁移文件；演示所需的种子数据必须从迁移文件中抽离并合并入 `sql/seed/`。
- **FR-009**: 演示数据必须按业务域拆分为最少必要的若干 `sql/seed/*.sql` 文件（如 `payflow_admin_seed.sql`、`payflow_cashier_seed.sql`），由一键安装脚本统一编排执行顺序；不允许同一业务域的演示数据散落在多个未编排的脚本中。
- **FR-010**: 一键安装脚本必须支持幂等重跑：对已有完整环境再次执行时，要么成功完成（覆盖式重置），要么给出明确"已就绪/请先清库"的可读提示，不得部分写入后中断。
- **FR-011**: `sql/full-reseed-payflow-demo.sql`、`sql/install_demo.sql`、`sql/admin/`、`sql/cashier/`、各 `payflow-*-server/src/main/resources/sql/*.sql` 中作为初始化或演示数据使用的脚本必须从仓库中删除或被合并，不再作为可执行入口存在。
- **FR-012**: 一键安装完成后，README/CLAUDE.md/相关文档必须只描述这一个入口的使用方式；旧入口的引用必须从文档中清除。

#### 死代码清理

- **FR-013**: 系统必须删除已被新版替代但仍存在于仓库中的 Java 类、方法、Spring Bean、Vue 组件、TypeScript 类型与 API 客户端方法；删除前需通过静态分析工具或全局引用搜索确认零引用，并考虑反射、AOP、定时任务、消息消费者等动态调用场景。
- **FR-014**: 系统必须删除项目根目录的一次性维护脚本（含 `check-roles.ps1`、`fix-roles.ps1`、`fix_roles.py`、`update_sql.py`），以及 `scripts/` 下被认定为一次性 Flyway 修复脚本（`fix_flyway_v8.py`、`record_flyway_v6.py`、`check_v6_schema.py`、`apply_v6_columns.py`、`apply_v6_migration.py`）；保留仍在用的运行期工具（如 `install_demo_db.py`、`verify_admin_password.py`、`run_mysql_sql.py`、`create_merchant_admin.py`、`_verify_merchant_notify_demo.py` 是否保留需逐个评估）。
- **FR-015**: 系统必须删除运行期产物且不应被 git 跟踪的文件（如 `dump.rdb`、`payflow-*-server/target/`），并在 `.gitignore` 中加入对应规则防止再次提交。
- **FR-016**: 系统必须清理已被替代的前端组件，例如同一业务功能下并存的多个 Drawer/Panel 组件（如 `OrderDetailDrawer.vue` 与 `OrderDetailPanel.vue`）应只保留实际被路由/页面引用的版本，未被引用者删除；i18n 资源文件中残留的孤立 key 一并清理。
- **FR-017**: 死代码删除不得改变任何对外可观察行为：现有 HTTP API 路径、参数、返回结构保持不变；既有定时任务、消息消费者、Webhook 出站行为保持不变；商户 SDK 签名算法保持不变。

#### Schema 与迁移单一权威

- **FR-018**: `sql/schema/payflow_admin.sql` 与 `sql/schema/payflow_cashier.sql` 必须代表"理想最终态 DDL"（含统一后的表名）；Flyway baseline/终态须与 schema 对齐；在空库上「仅跑 schema」与「跑 Flyway」的结构应一致（无生产 `RENAME` 路径）。
- **FR-019**: 增量升级路径必须以 Flyway 迁移为唯一权威；`sql/migrations/` 仅作为历史归档（README 已声明），本次升级须将其从一键安装脚本中移除引用，且不得新增到该目录。
- **FR-020**: `payflow-admin-server/src/main/resources/sql/admin-alter-*.sql`、`admin-data.sql`、`admin-system-configs-init.sql` 等位于 server 模块内的辅助 SQL 必须经过审视，要么并入 Flyway 迁移与 `sql/seed/`，要么明确删除。

#### 兼容性与回归

- **FR-021**: 升级完成后，`docs/CONTRACT_MATRIX.md`、`CLAUDE.md`、`docs/REFUND_STATE_MACHINE.md`、`sql/README.md` 中提及的表名、入口脚本、命令必须与代码一致；过期描述必须同步修正。
- **FR-022**: 升级 PR 合入前必须通过：①后端单元测试 + 集成测试全绿；②前端构建无未解析引用；③一键安装脚本在干净 MySQL 实例上成功执行；④三个后端服务启动健康检查通过；⑤至少覆盖"登录、下单、支付回调、退款、对账、RBAC 权限、商户隔离"七条关键路径的端到端验证（按需 Playwright），且后台日志无阻断错误。

#### 附加优化（非阻塞，建议范围）

- **FR-023**: 升级期间产出"附加优化建议清单"附录（在 `plan.md` 或独立文件），覆盖至少：未使用 Maven 依赖、未使用 npm 依赖、潜在重复 API 响应包装类、`docs/CONTRACT_MATRIX.md` 漂移点、`pom.xml` 与 `package.json` 版本一致性、是否存在重复定义的常量/枚举（如错误码范围）等；每条标注是否在本次执行。

### Key Entities *(include if feature involves data)*

- **数据库表前缀策略**: admin 库允许 `admin_`、`recon_`（`recon_*` 保留）；cashier 库仅 `cashier_`；`sys_*` → `admin_sys_*`。附**表名映射表**（旧名 → 新名 → 所属库）作为实施与验收依据。
- **一键安装脚本配置**: 单一权威安装入口的输入（连接参数）、执行阶段（建库 → schema → seed → 校验）、输出（成功提示与演示账号清单），以及幂等执行策略。
- **演示数据画像**: 各演示页面预期看到的数据形态（商户、渠道、订单、退款、对账、RBAC、风控、回调），作为演示种子的对照清单。
- **死代码识别清单**: 升级范围内所有已确认应被删除的文件、类、方法、组件清单，分为"应用代码 / 一次性脚本 / 运行期产物 / 文档过期块"四类。
- **Schema/Flyway 对齐记录**: 无生产库前提下，记录 schema 终态与 Flyway baseline 的对齐方式（合并/刷新），供后续首次上线参考。

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 升级完成后，遍历 `payflow_admin` 与 `payflow_cashier` 全部业务表，前缀合规率达到 100%（违规表数量 = 0）。
- **SC-002**: 一键安装命令在空 MySQL 实例上的成功率达到 100%（在团队约定的目标 MySQL 版本上至少 5 次连续干净执行均成功），从执行到完成耗时不超过 60 秒。
- **SC-003**: README/CLAUDE.md 中描述的"如何拉起演示环境"步骤数 ≤ 2 步（执行单一命令 + 启动后端服务），并与代码实际入口一致。
- **SC-004**: 升级 PR 合入前，应用代码（Java/Vue/TS）中对旧表名（升级前清单中列出的违规表名）的引用次数 = 0；对已确认死代码的引用次数 = 0。
- **SC-005**: 至少 7 条核心端到端路径（登录、下单、支付回调、退款、对账、RBAC 角色/按钮权限、商户数据隔离）在重构后通过率 100%；后台日志中"重构原因导致"的 ERROR 数量 = 0。
- **SC-006**: 与升级前相比，仓库内 SQL 脚本入口数量减少 ≥ 50%（合并/删除冗余脚本）；Flyway 与 `sql/schema/` 终态一致且可追溯（无生产 `RENAME` 路径要求）。
- **SC-007**: 输出至少 5 条经评估的"附加优化建议"，每条注明"是否本次执行"与执行/不执行的理由。
- **SC-008**: 升级 PR 中任何被删除的对外可观察行为（API、菜单、按钮、回调）数 = 0；如确需变更，须在 PR 描述中显式标注并征得 Review 同意。

## Assumptions

- **当前无生产系统**：表前缀统一采用「直接改 schema 终态 + 同步代码」；新环境从正确表名建表；本地旧库建议清库重装，不将 `RENAME TABLE` 作为验收路径。
- **`recon_*` 保留**为 admin 库内合法第三前缀，不改为 `admin_recon_*`。
- **重复表权威归属**：写入方业务域优先（事务性 → cashier，配置性 → admin）。
- **`sys_*` → `admin_sys_*`**：与 `admin_users` 等业务表区分，避免命名冲突。
- 数据库引擎仍为 MySQL，字符集为 `utf8mb4`，本次升级不引入新的数据库或新的表存储引擎。
- 演示数据的覆盖范围沿用 `sql/README.md` 中"演示数据覆盖的页面"章节的口径；商户号 M100001–M100003、用户 `admin/finance_demo/risk_demo` 的演示账号身份保持不变。
- "死代码"以"应用代码（Java/Vue/TS）静态搜索零引用 + 非反射/AOP/动态注册的运行期入口"为基本判定；无法静态判定的情况（如 RocketMQ 消费者、XXL-Job 任务、Spring Bean 通过名称查找）必须在删除前提交人工确认清单。
- Flyway 迁移历史版本号沿现有路径继续递增；admin V8、cashier V5 之后新增的版本号不与既有版本冲突。
- 本次不引入新业务功能，不修改现有支付渠道行为、对账算法、风控规则与 RBAC 权限模型语义；仅做"移动 + 改名 + 删除 + 合并"。
- 前端 i18n、组件、composable、directive 的清理以"静态分析无引用 + 路由表无关联"为前置；动态 path 拼接使用的菜单 key 由 `admin_sys_menus`（及种子数据）决定，须从数据库与代码两侧交叉确认。
- 项目根目录 `dump.rdb`、`payflow-*-server/target/` 是构建/运行期产物，不应被 git 跟踪；本次升级将其纳入 `.gitignore`。
- 升级 PR 完成后，`docs/CONTRACT_MATRIX.md` 与 `CLAUDE.md` 中"模块职责""端口""表前缀"段落保持权威，并与本规范对齐。


