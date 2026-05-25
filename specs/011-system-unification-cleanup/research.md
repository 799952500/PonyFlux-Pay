# Research: 系统统一性升级与代码瘦身

**Feature**: `011-system-unification-cleanup` | **Date**: 2026-05-23

## R1: 表前缀统一策略（无生产库）

**Decision**: 直接修订 `sql/schema/payflow_*.sql` 终态 DDL + 全量同步 Java `@TableName`/SQL 字符串；本地旧库**清库重装**；不编写 `RENAME TABLE` Flyway 迁移。

**Rationale**: 澄清已确认无生产系统；从零建库成本最低，避免双轨（旧名视图 + 新名表）维护。

**Alternatives considered**:
- Flyway `RENAME TABLE` 增量迁移 — 拒绝：无生产库、增加迁移复杂度。
- 仅改代码不改 schema — 拒绝：新环境仍会建出错表名。

## R2: 合法前缀集合

**Decision**:
- `payflow_admin`: `admin_*` + `recon_*`（保留第三前缀）
- `payflow_cashier`: 仅 `cashier_*`
- RBAC：`sys_*` → `admin_sys_*`

**Rationale**: 对齐宪法 III 与 `/speckit-clarify` 四项决策。

## R3: 重复表权威归属

**Decision**:

| 概念 | 权威库 | 目标表名 | 处理 |
|------|--------|----------|------|
| Webhook 端点配置 | admin | `admin_merchant_webhook_endpoint` | 从 cashier schema 删除 |
| Webhook 投递日志 | cashier | `cashier_webhook_delivery_log` | 从 admin schema 删除 |
| 渠道/商户配置 | admin | 已有 `admin_*` | cashier 侧 `cashier_channels` 等为交易路由缓存，保留 |
| 风控黑名单（admin 库误名） | admin | `admin_risk_blacklist` | 自 `cashier_risk_blacklist` 改名 |

**Rationale**: 写入方业务域优先（配置 → admin，事务日志 → cashier）。

## R4: 一键安装入口

**Decision**: 保留 `python scripts/install_demo_db.py` 为唯一入口；`SQL_FILES` 仅含 `sql/schema/` + `sql/seed/`；将当前 7 个 `sql/migrations/*.sql` 中的 DML/DDL 合并进 schema 或 seed。

**Rationale**: 满足 FR-007/FR-008；消除新人误跑历史迁移。

**Alternatives considered**:
- 新 shell 脚本替代 Python — 拒绝：已有 `run_mysql_sql.py` 与团队习惯。

## R5: Flyway 与 schema 对齐

**Decision**: 无生产库前提下，各服务 Flyway **刷新 baseline**（合并为与 `sql/schema/` 一致的 V1 或追加终态 V9+），保证「空库 + Flyway」≈「空库 + schema 脚本」。

**Rationale**: FR-018；避免双源漂移复发。

**Alternatives considered**:
- 废弃 Flyway 仅用 schema — 拒绝：Spring Boot 启动仍依赖 Flyway，需保留单一路径。

## R6: 死代码识别方法

**Decision**: 两阶段删除：(1) `rg`/IDE 引用搜索 + Maven/TS 编译；(2) 人工清单处理反射、XXL-Job、`@Scheduled`、动态路由。

**Rationale**: 降低误删运行时入口风险（spec Edge Cases）。

## R7: 实体与 schema 不一致修复

**Decision**: 本次一并修正 `@TableName` 与 schema 不一致项（如 `merchants`→`admin_merchants`、`channels`→`admin_channels`），不仅处理无前缀违规表。

**Rationale**: 扫描发现多处实体仍用迁移前短名，与 `sql/schema` 已部分对齐的状态冲突。

## R8: 附加优化（P3）

**Decision**: 本次**必做**：`.gitignore` 补 `dump.rdb`、`target/`、删除根目录 ad-hoc 脚本。**可选**：未使用依赖扫描、API 包装类统一（记入 plan 附录，不阻塞）。

**Rationale**: FR-015/FR-023；控制 PR 范围。
