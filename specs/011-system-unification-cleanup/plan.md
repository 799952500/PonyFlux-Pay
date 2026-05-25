# Implementation Plan: 系统统一性升级与代码瘦身

**Branch**: `011-system-unification-cleanup` | **Date**: 2026-05-23 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/011-system-unification-cleanup/spec.md`

**Note**: 本计划由 `/speckit-plan` 命令填充。

## Summary

在无生产库前提下，完成三件事：**(1)** 双库表名对齐宪法前缀（`admin_*` / `recon_*` / `cashier_*`，`sys_*`→`admin_sys_*`）；**(2)** 演示环境单入口 `install_demo_db.py` 仅依赖 `sql/schema/` + `sql/seed/`；**(3)** 前后端与 SQL 死代码清理。不实施 `RENAME TABLE`；本地建议清库重装。详见 [research.md](./research.md)、[data-model.md](./data-model.md)。

## Technical Context

**Language/Version**: Java 17 / TypeScript (Vue 3.4)  
**Primary Dependencies**: Spring Boot 3.2.5 / MyBatis-Plus 3.5.7 / Flyway / Vue 3 + Vite 5 / Element Plus  
**Storage**: MySQL `payflow_admin` + `payflow_cashier`；Redis（运行时，本特性不改）  
**Testing**: JUnit 5 + Mockito；`mvn compile`；按需 Playwright；`install_demo_db.py` + 前缀校验 SQL  
**Target Platform**: Linux/Windows 开发机；JVM 17 三服务  
**Project Type**: Maven 多模块 web-service + 双 Vue 前端  
**Performance Goals**: 一键安装 ≤60s（SC-002）；无运行时性能变更  
**Constraints**: 不改 HTTP API 路径/契约；不改支付/对账业务语义；无生产迁移路径  
**Scale/Scope**: ~35 admin 表 + ~12 cashier 表重命名/删重；三后端 + 双前端 + sql/scripts 目录整理

## Constitution Check

*GATE: Phase 0 研究前 — 已通过。Phase 1 设计后 — 复查通过。*

| # | 宪法原则 | 状态 | 说明 |
|---|----------|------|------|
| 1 | I. 模块边界纪律 | PASS | 不新增模块；webhook 配置归 admin、投递日志归 cashier。 |
| 2 | II. 支付渠道抽象 | PASS | 仅改表名/实体，不注入具体 Handler。 |
| 3 | III. 数据库分区 | PASS | 本特性核心即落实前缀；`recon_*` 保留。 |
| 4 | IV. API 响应规范 | PASS | 不新增 API；可选统一包装类放 P3。 |
| 5 | V. 密钥与配置安全 | N/A | 无密钥变更。 |
| 6 | 编码规范 | PASS | 重命名遵循既有命名。 |
| 7 | 数据库访问 | PASS | 不改查询模式；同步 `@TableName`。 |
| 8 | 安全编码 | PASS | RBAC/审计表改名后种子与权限一致。 |
| 9 | 测试规范 | PASS | quickstart 定义 E2E + 日志闭环。 |

**Gate Result**: ALL PASS

## Project Structure

### Documentation (this feature)

```text
specs/011-system-unification-cleanup/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── install-contract.md
│   └── table-prefix-validation.md
└── tasks.md              # /speckit-tasks
```

### Source Code (touch map)

```text
sql/
├── schema/payflow_admin.sql      # 表名终态 + 删 admin 侧 webhook 日志
├── schema/payflow_cashier.sql    # 表名终态 + 删 cashier 侧 webhook 端点
├── seed/payflow_admin_seed.sql   # admin_sys_* + 合并原 migrations DML
├── seed/payflow_cashier_seed.sql
└── README.md

scripts/
├── install_demo_db.py            # 仅 schema + seed
└── validate_table_prefixes.py    # 新增（可选）

payflow-admin-server/
├── entity/**/*.java              # @TableName + Sys* → admin_sys_*
├── mapper/**, resources/mapper/**
└── resources/db/migration/admin/ # baseline 与 schema 对齐

payflow-cashier-server/
├── entity/**/*.java
└── resources/db/migration/cashier/

payflow-recon-server/
└── resources/db/migration/recon/

payflow-admin-client/             # 死代码组件/TS/API
payflow-cashier-client/

# 删除/归档
sql/admin/, sql/cashier/, sql/full-reseed-payflow-demo.sql
payflow-*/src/main/resources/sql/*.sql（并入 schema/seed 后删）
check-roles.ps1, fix-roles.ps1, fix_roles.py, update_sql.py
scripts/fix_flyway_v8.py, record_flyway_v6.py, ...
```

**Structure Decision**: 不新增 Maven 模块。Webhook 实体若仅从 cashier 读配置，评估迁至 admin-server 或保留 cashier Mapper 读 admin 表（实现阶段二选一，须满足模块边界）。

## Complexity Tracking

> 无宪法豁免。

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| — | — | — |

## Phase 0: Research

已完成：[research.md](./research.md)

关键决策：无 `RENAME`；`recon_*` 保留；webhook 拆库；`install_demo_db` 去 migrations；Flyway baseline 与 schema 对齐。

## Phase 1: Design & Contracts

已完成：

- [data-model.md](./data-model.md) — 表映射、实体对齐、seed 职责
- [contracts/install-contract.md](./contracts/install-contract.md) — 安装顺序与禁止项
- [contracts/table-prefix-validation.md](./contracts/table-prefix-validation.md) — 验收 SQL
- [quickstart.md](./quickstart.md) — 端到端验收步骤

## Implementation Phases（供 /speckit-tasks 拆分）

### Phase A — 表映射与 schema 终态（P1）

1. 按 [data-model.md](./data-model.md) 修改 `payflow_admin.sql` / `payflow_cashier.sql`
2. 删除重复表定义（admin 的 `webhook_delivery_log`；cashier 的 `merchant_webhook_endpoint`）
3. 将 `recon_routing_decision_log` 改为 `admin_routing_decision_log`

### Phase B — 应用代码同步（P1）

1. `rg '@TableName'` 全量更新
2. 原生 SQL、Mapper XML、Flyway 新 baseline
3. `MerchantWebhookEndpoint` / `WebhookDeliveryLog` 包路径与数据源归属

### Phase C — Seed 与一键安装（P1）

1. 合并 7 个 `sql/migrations` 内容到 seed/schema
2. 精简 `install_demo_db.py` 的 `SQL_FILES`
3. （可选）`validate_table_prefixes.py`

### Phase D — 死代码与仓库卫生（P1）

1. 删除 [spec.md](./spec.md) US3 所列脚本与废弃 SQL 目录
2. 前端：保留 `OrderDetailDrawer` 或 `OrderDetailPanel` 之一
3. `.gitignore`：`dump.rdb`、`target/`、`__pycache__/`
4. 删除 `CashierInternalRefundClient` 等已替代类（若仍存在）

### Phase E — 文档与 Flyway 一致性（P2）

1. 更新 `CLAUDE.md`、`sql/README.md`、`docs/CONTRACT_MATRIX.md`（表名引用）
2. 验证「schema 安装」与「Flyway 空库迁移」结构一致

### Phase F — 回归（P1）

1. `mvn -B clean package`
2. `install_demo_db.py` ×5
3. quickstart 七条路径 + Playwright
4. 输出 [附加优化清单](#附加优化建议清单-p3)

## Post-Design Constitution Check

| # | 原则 | 状态 |
|---|------|------|
| 1–9 | 同前 | ALL PASS |

## 附加优化建议清单 (P3)

| # | 项 | 建议 | 本次执行 |
|---|-----|------|----------|
| 1 | `dump.rdb` 入 `.gitignore` 并移出 git | 必须 | 是 |
| 2 | `**/target/` 入 `.gitignore` | 必须 | 是 |
| 3 | 根目录 `*.ps1`/`fix_roles.py`/`update_sql.py` | 删除 | 是 |
| 4 | 未使用 Maven 依赖 | `mvn dependency:analyze` | 否（记 backlog） |
| 5 | API 响应包装类统一为 `AdminApiResponseKit` 等 | 评估重复 | 否 |
| 6 | `docs/CONTRACT_MATRIX.md` 漂移扫描 | PR 末人工 | 部分 |
| 7 | 合并 `payflow_cashier_merchant_notify_demo.sql` 入主 seed | 减文件数 | 可选 |

## 风险与缓解

| 风险 | 缓解 |
|------|------|
| 本地旧库残留旧表名 | 文档强调 DROP DATABASE；安装脚本检测失败提示 |
| 误删反射/定时任务入口 | 删除前人工清单；全量 `mvn test` |
| Flyway 与 schema 再次漂移 | Phase E 结构 diff；CI 可加校验 job（后续） |
| RBAC 种子与 `admin_sys_*` 不一致 | 单文件 `payflow_admin_seed.sql` 统一维护 |

## 下一步

运行 **`/speckit-tasks`** 生成可执行任务列表，然后 **`/speckit-implement`** 按 Phase A→F 实施。
