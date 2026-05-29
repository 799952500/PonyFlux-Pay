# Quickstart: 对账差异处置工作流升级

**Created**: 2026-05-28  
**Feature**: [spec.md](spec.md)  
**Plan**: [plan.md](plan.md)

本 Quickstart 用于本地联调与演示，目标是用最少步骤跑通：生成差异 → 自动派单 → SLA 提醒/升级 → 长尾摘要 → 订阅日报快照。

## 1. 前置条件

- 已初始化 demo 数据库（包含 `payflow_admin` 与 `payflow_cashier`）
- `payflow-admin-server` 与 `payflow-admin-client` 可正常启动
- 已完成本特性对应的 Flyway 迁移（新增 `recon_*` 表）

## 2. 启动服务

后端：

```bash
mvn -B -pl payflow-admin-server spring-boot:run
```

前端：

```bash
cd payflow-admin-client && npm run dev
```

## 3. 生成对账差异（演示数据路径）

可选两种方式（实现阶段二选一，演示至少保留一种）：

1) **使用现有对账页面手动跑批**：在管理后台“资金对账”页面手动触发一次对账，生成 `recon_diff` 记录。  
2) **使用 demo seed 数据**：在 `sql/seed/payflow_admin_seed.sql` 预置一定数量的 `recon_diff`（含不同账龄）。

## 4. 验证“工单化 + 派单”

在管理后台进入：
- “对账 → 我的差异工单”

预期：
- 新差异进入 `UNASSIGNED/ASSIGNED`（取决于是否自动派单已开启）
- 可执行：认领、开始处理、改派、终态处置（PROCESSED/IGNORED）
- 每次动作在“审计”区域可追溯（基于 `recon_handler_audit`）

## 5. 验证 SLA 提醒与升级（演示口径）

将某个差异类型 SLA 临时配置为 1 分钟（仅演示），并让一条差异停留在非终态：
- 50 秒左右：收到“临近超时”站内通知
- 60 秒后：收到“已超时升级”站内通知（升级到 `recon:manage` 角色）

## 6. 验证长尾摘要

准备账龄 ≥ 7 天的差异（seed 或造数），等待/触发摘要任务：
- `recon:manage` 角色收到每日摘要通知（仅当存在长尾差异）

## 7. 验证报告订阅

在“个人中心 → 报告订阅”订阅日报或周报，然后触发一次报告生成：
- 通知中心收到 `RECON_REPORT` 通知
- 点击进入报告详情，指标与“归因看板”口径一致

## 8. E2E 验证（按需）

当实现涉及 UI 与跨服务流程时，使用 Playwright 验证关键路径，并同时观察后台日志无阻断错误：

```bash
cd payflow-admin-client && npx playwright test
```

