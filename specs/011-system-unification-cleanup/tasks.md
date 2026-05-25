# Tasks: 系统统一性升级与代码瘦身

**Input**: Design documents from `/specs/011-system-unification-cleanup/`  
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: spec 要求 FR-022 全量回归与 quickstart 冒烟；包含编译/安装/前缀校验/E2E 任务，无强制 TDD 单测（除非既有测试需修复）。

**Organization**: 按用户故事分组；US1→US2 有顺序依赖（安装依赖正确 schema）；US3 可与 US4/US5 部分并行。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行（不同文件、无未完成依赖）
- **[Story]**: US1–US6 对应 spec.md

---

## Phase 1: Setup（共享基础设施）

**Purpose**: 分支确认、验收脚本、仓库卫生基线。

- [X] T001 确认当前分支为 `011-system-unification-cleanup` 且设计文档只读 `specs/011-system-unification-cleanup/`
- [X] T002 [P] 实现表前缀校验脚本 `scripts/validate_table_prefixes.py`（逻辑见 `specs/011-system-unification-cleanup/contracts/table-prefix-validation.md`）
- [X] T003 [P] 更新 `.gitignore` 增加 `dump.rdb`、`**/target/`、`scripts/__pycache__/`、`*.pyc`
- [X] T004 记录升级前基线：执行 `mvn -B -DskipTests compile` 并保存是否通过（供 PR 对比）

---

## Phase 2: Foundational（阻断性前置条件）

**Purpose**: 清库指引与映射清单就绪；**阻断所有用户故事**。

**CRITICAL**: 完成前不得开始表名与代码修改。

- [ ] T005 在 `sql/README.md` 增加「升级本特性前请 DROP DATABASE」说明（链接 quickstart 清库 SQL）
- [ ] T006 用 `rg '@TableName' payflow-admin-server payflow-cashier-server payflow-recon-server` 生成实施勾选清单，追加到 `specs/011-system-unification-cleanup/data-model.md` 末尾「实施勾选」小节
- [ ] T007 用 `rg 'sys_|risk_rules|merchant_application|webhook_delivery_log|merchant_webhook' --glob '*.java' --glob '*.xml' --glob '*.sql'` 列出待替换 SQL 字符串清单（记入上述勾选小节或 PR 描述）

**Checkpoint**: 映射表 + 全仓引用清单就绪。

---

## Phase 3: User Story 1 - 数据库表前缀全面对齐 (Priority: P1) MVP

**Goal**: `payflow_admin` 仅 `admin_*`/`recon_*`；`payflow_cashier` 仅 `cashier_*`；`sys_*`→`admin_sys_*`；webhook 拆库。

**Independent Test**: 空库执行 schema 后运行 `scripts/validate_table_prefixes.py` 通过；`mvn -B -DskipTests compile` 通过。

### Implementation for User Story 1 — Schema 终态

- [X] T008 [US1] 按 `data-model.md` 重命名/删表更新 `sql/schema/payflow_admin.sql`（含 `admin_sys_*`、`admin_risk_rules`、`admin_merchant_*`、`admin_payment_link`、`admin_routing_decision_log`；删除 `webhook_delivery_log`；`cashier_risk_blacklist`→`admin_risk_blacklist`）
- [X] T009 [US1] 按 `data-model.md` 更新 `sql/schema/payflow_cashier.sql`（`webhook_delivery_log`→`cashier_webhook_delivery_log`；删除 `merchant_webhook_endpoint`）
- [ ] T010 [P] [US1] 新增 admin webhook 配置表 DDL 片段 `admin_merchant_webhook_endpoint`（若 T008 未完整包含）

### Implementation for User Story 1 — admin-server 实体与 SQL

- [ ] T011 [P] [US1] 更新 RBAC 实体 `@TableName`：`payflow-admin-server/src/main/java/com/payflow/admin/entity/SysUser.java` → `admin_sys_users`
- [ ] T012 [P] [US1] 更新 `SysRole.java`、`SysMenu.java`、`SysRoleMenu.java`、`SysUserRole.java` → `admin_sys_*`
- [ ] T013 [P] [US1] 更新 `RiskRule.java` → `admin_risk_rules`；`MerchantApplicationEntity.java` 等进件实体 → `admin_merchant_*`
- [ ] T014 [P] [US1] 更新短名实体：`Merchant.java`→`admin_merchants`、`Channel.java`→`admin_channels`、`PaymentMethod.java`→`admin_payment_methods`、`PaymentAccount.java`→`admin_payment_accounts`、`MerchantPaymentRoute.java`→`admin_merchant_payment_routes`、`MerchantPaymentMethod.java`→`admin_merchant_payment_methods`
- [ ] T015 [P] [US1] 更新 `RoutingDecisionLog.java` → `admin_routing_decision_log`（`payflow-admin-server/src/main/java/com/payflow/admin/entity/RoutingDecisionLog.java`）
- [ ] T016 [US1] `rg` 并修复 admin-server 内原生 SQL / Mapper XML 旧表名（`payflow-admin-server/src/main/resources/mapper/`、`src/main/java/**/*.java`）

### Implementation for User Story 1 — cashier-server 实体与 SQL

- [ ] T017 [US1] 更新 `WebhookDeliveryLog.java` → `cashier_webhook_delivery_log`（`payflow-cashier-server/src/main/java/com/payflow/cashier/entity/WebhookDeliveryLog.java`）
- [ ] T018 [US1] 将 `MerchantWebhookEndpoint` 迁至 admin 域或改为读 `admin_merchant_webhook_endpoint`（`payflow-cashier-server` 与 `payflow-admin-server` 二选一，在 PR 描述记录决策；更新 `MerchantWebhookEndpoint.java` 及调用方）
- [ ] T019 [US1] `rg` 并修复 cashier-server 内 SQL/XML 旧表名

### Implementation for User Story 1 — recon-server

- [ ] T020 [P] [US1] 检查并修复 `payflow-recon-server` 内引用 `recon_routing_decision_log` 或违规表名的 SQL/实体（若有则改为 `admin_routing_decision_log` 或保持 `recon_*`）

### Implementation for User Story 1 — Flyway 与测试修复

- [ ] T021 [US1] 刷新或追加 Flyway 与 schema 一致：`payflow-admin-server/src/main/resources/db/migration/admin/`（新 baseline 或 V9+，禁止改已发布版本语义外的历史文件策略见 research.md）
- [ ] T022 [US1] 刷新或追加 `payflow-cashier-server/src/main/resources/db/migration/cashier/`（含 V5 webhook 表名修正）
- [ ] T023 [US1] 修复因表名变更失败的单元/集成测试（`payflow-admin-server/src/test/`、`payflow-cashier-server/src/test/`）
- [X] T024 [US1] 空库仅执行 `sql/schema/*.sql` 后运行 `python scripts/validate_table_prefixes.py` 通过

**Checkpoint**: US1 独立可测；应用代码 `rg` 旧违规表名为 0（`sql/migrations/` 归档除外）。

---

## Phase 4: User Story 2 - 一键完成全量初始化 (Priority: P1)

**Goal**: `install_demo_db.py` 仅引用 `sql/schema/` + `sql/seed/`。

**Independent Test**: `python scripts/install_demo_db.py` 在空库成功；可 `admin/admin123` 登录。

**Depends on**: Phase 3 US1 schema 完成。

### Implementation for User Story 2

- [ ] T025 [US2] 将 `sql/migrations/2026-05-20_hide-merchant-payments-menu.sql` 等 7 个文件中的 DML 合并进 `sql/seed/payflow_admin_seed.sql` 或 `sql/schema/payflow_admin.sql`（逐文件核对菜单/权限/配置）
- [ ] T026 [P] [US2] 合并 `sql/migrations/2026-05-20_merchant_risk_config.sql`、`2026-05-21_merchant_data_isolation_governance.sql` 等到对应 seed
- [ ] T027 [P] [US2] 合并 `sql/migrations/2026-05-22_*.sql`、`2026-05-23_*.sql` 等到 seed
- [X] T028 [US2] 精简 `scripts/install_demo_db.py` 的 `SQL_FILES` 为仅 6 个文件（schema×3 + seed×3；见 `contracts/install-contract.md`）
- [ ] T029 [US2] 更新 `sql/seed/payflow_admin_seed.sql` 中所有 `sys_*` 表名为 `admin_sys_*`
- [ ] T030 [US2] 更新 `sql/seed/payflow_cashier_seed.sql` 表名与 US1 一致
- [ ] T031 [US2] 连续 2 次执行 `python scripts/install_demo_db.py` 验证幂等或明确提示（SC-002 预检）

**Checkpoint**: 单命令演示库安装；不再依赖 `sql/migrations/`。

---

## Phase 5: User Story 3 - 清理死代码 (Priority: P1)

**Goal**: 删除废弃脚本、SQL 入口、未引用前后端代码。

**Independent Test**: `mvn -B clean package` 与 `npm run build`（admin-client）通过；一键安装仍成功。

### Implementation for User Story 3

- [ ] T032 [P] [US3] 删除根目录 `check-roles.ps1`、`fix-roles.ps1`、`fix_roles.py`、`update_sql.py`
- [ ] T033 [P] [US3] 删除 `scripts/fix_flyway_v8.py`、`scripts/record_flyway_v6.py`、`scripts/check_v6_schema.py`、`scripts/apply_v6_columns.py`、`scripts/apply_v6_migration.py`
- [X] T034 [P] [US3] 删除或归档 `sql/full-reseed-payflow-demo.sql`、`sql/install_demo.sql`、`sql/admin/`、`sql/cashier/` 目录
- [X] T035 [P] [US3] 删除 `payflow-admin-server/src/main/resources/sql/admin-*.sql` 与 `payflow-cashier-server/src/main/resources/sql/*.sql`（内容已并入 schema/seed 后）
- [ ] T036 [US3] 确认并删除 `payflow-admin-server/src/main/java/com/payflow/admin/client/CashierInternalRefundClient.java`（若仍存在）；统一使用 `CashierInternalClient.java`
- [ ] T037 [US3] 对比引用，删除未使用的 `payflow-admin-client/src/components/orders/OrderDetailDrawer.vue` 或 `OrderDetailPanel.vue` 之一并修正 import
- [ ] T038 [P] [US3] `rg` 清理 admin-client/cashier-client 无引用 TS 类型、API 方法、i18n 孤立 key
- [ ] T039 [US3] 从 git 索引移除 `dump.rdb`（若已跟踪）：`git rm --cached dump.rdb`

**Checkpoint**: 仓库无废弃可执行 SQL 入口；构建绿。

---

## Phase 6: User Story 4 - 演示数据单一权威 (Priority: P2)

**Goal**: 演示数据仅来自编排后的 seed；与 `sql/README.md` 口径一致。

**Independent Test**: 安装后查询关键表记录数与文档一致；三演示账号权限正确。

**Depends on**: US2。

- [ ] T040 [US4] 核对 `sql/README.md`「演示数据覆盖的页面」与 seed 数据一致，不一致则改 seed 或改文档
- [ ] T041 [P] [US4] 可选：合并 `sql/seed/payflow_cashier_merchant_notify_demo.sql` 入 `sql/seed/payflow_cashier_seed.sql` 并更新 `install_demo_db.py`
- [ ] T042 [US4] 验证 `finance_demo`、`risk_demo` 对 `admin_sys_*` 角色菜单与商户隔离（M100001）与 seed 一致

**Checkpoint**: US4 独立测试通过。

---

## Phase 7: User Story 5 - 单一权威 schema 与 Flyway (Priority: P2)

**Goal**: schema 与 Flyway 空库结构一致；`sql/migrations/` 仅归档。

**Independent Test**: 空库「schema 安装」与「Flyway migrate」表结构 diff 为空（或 documented 差异为 0）。

- [ ] T043 [US5] 更新 `sql/README.md`：明确 `sql/migrations/` 为历史归档、禁止加入 `install_demo_db.py`
- [ ] T044 [US5] 对比空库 schema vs Flyway 输出，修复 `payflow-admin-server`/`payflow-cashier-server`/`payflow-recon-server` 迁移差异
- [ ] T045 [P] [US5] 若存在 `payflow-admin-server/src/main/resources/sql/admin-alter-*.sql`，合并进 Flyway 或删除

**Checkpoint**: 双路径建库结构一致。

---

## Phase 8: User Story 6 - 附加优化清单 (Priority: P3)

**Goal**: 记录并执行 plan.md 中 P3 必选项。

**Independent Test**: plan 附录 7 项均有「是否本次执行」结论。

- [ ] T046 [US6] 在 `specs/011-system-unification-cleanup/plan.md` 或 `docs/` 更新附加优化清单执行结果（勾选已做/ backlog）
- [ ] T047 [P] [US6] 扫描 `docs/CONTRACT_MATRIX.md` 表名/路径漂移并修正明显项
- [ ] T048 [P] [US6] 运行 `mvn dependency:analyze` 记录未使用依赖（仅文档，可不删）

**Checkpoint**: SC-007 满足。

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: 全量回归与文档闭环。

- [ ] T049 更新 `CLAUDE.md`、`sql/README.md` 仅保留 `install_demo_db.py` 单入口说明
- [ ] T050 [P] 更新 `docs/CONTRACT_MATRIX.md`、`docs/REFUND_STATE_MACHINE.md` 中过时表名（如有）
- [X] T051 执行 `mvn -B clean package` 全模块
- [X] T052 [P] 执行 `cd payflow-admin-client && npm run build` 与 `cd payflow-cashier-client && npm run build`
- [ ] T053 按 `specs/011-system-unification-cleanup/quickstart.md` 完整走查（含 7 条核心路径）
- [ ] T054 [P] 按需 `cd payflow-admin-client && npx playwright test`；监控 admin/cashier/recon 日志无阻断 ERROR
- [X] T055 执行 `python scripts/validate_table_prefixes.py` 与 `rg` 旧表名应用代码零命中复核
- [ ] T056 宪法合规自检：对照 `plan.md` Constitution Check 九项打勾

---

## Dependencies & Execution Order

### Phase Dependencies

```text
Setup (1) → Foundational (2) → US1 (3) → US2 (4) ─┬→ US4 (6)
                                                 ├→ US5 (7)
US3 (5) 可在 US2 后与 US4/US5 并行（避免删 SQL 早于 seed 合并）
US6 (8) → Polish (9)
```

### User Story Dependencies

| 故事 | 依赖 | 可并行 |
|------|------|--------|
| US1 | Foundational | 实体更新 T011–T015 可并行 |
| US2 | US1 | seed 合并 T025–T027 可并行 |
| US3 | US2 建议完成后（防删未合并 SQL） | T032–T035 可并行 |
| US4 | US2 | T040–T041 |
| US5 | US1 + US2 | T044–T045 |
| US6 | US1–US5 末 | T047–T048 可并行 |

### MVP Scope

**MVP = Phase 1 + 2 + 3（US1）+ 4（US2）+ 9 中 T051/T055 最小集**

交付：空库正确表前缀 + 一键安装 + 编译通过。

---

## Parallel Example: User Story 1

```bash
# 并行改 admin 实体（不同文件）：
T011 SysUser → T012 SysRole/SysMenu → T013 RiskRule → T014 Merchant/Channel/...

# 并行改 schema 两文件（若两人协作）：
T008 payflow_admin.sql | T009 payflow_cashier.sql
```

---

## Parallel Example: User Story 3

```bash
# 并行删除互不依赖路径：
T032 根目录脚本 | T033 scripts/ 一次性脚本 | T034 sql/ 废弃目录 | T035 server resources/sql
```

---

## Implementation Strategy

1. 完成 Setup + Foundational（清单就绪）
2. **US1**：先 schema，再实体，再 XML/Java SQL，再 Flyway，再 validate
3. **US2**：合并 migrations → 改 install_demo_db → 更新 seed 表名
4. **US3**：删废弃文件（确认 US2 已合并）
5. **US4/US5**：演示与 Flyway 一致性
6. **Polish**：全量构建 + quickstart + Playwright

---

## Task Summary

| Phase | 任务 ID | 数量 |
|-------|---------|------|
| Setup | T001–T004 | 4 |
| Foundational | T005–T007 | 3 |
| US1 | T008–T024 | 17 |
| US2 | T025–T031 | 7 |
| US3 | T032–T039 | 8 |
| US4 | T040–T042 | 3 |
| US5 | T043–T045 | 3 |
| US6 | T046–T048 | 3 |
| Polish | T049–T056 | 8 |
| **合计** | **T001–T056** | **56** |

**Suggested MVP**: T001–T031 + T051 + T055（约 33 项）
