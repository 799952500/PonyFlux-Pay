# Quickstart: 系统统一性升级验收

**Feature**: `011-system-unification-cleanup` | **Branch**: `011-system-unification-cleanup`

## 前置条件

- MySQL 8.x，本机 `root/root`（或自定义参数）
- JDK 17、Maven、Node 18+
- 建议：**清库**后验收（无生产库场景）

```sql
DROP DATABASE IF EXISTS payflow_admin;
DROP DATABASE IF EXISTS payflow_cashier;
```

## 1. 一键安装演示库

```bash
python scripts/install_demo_db.py
```

**通过**: 无报错；可登录后台 `admin` / `admin123`。

## 2. 表前缀校验

执行 [contracts/table-prefix-validation.md](./contracts/table-prefix-validation.md) 中 SQL，两查询均为 **0 行**。

## 3. 后端编译与启动

```bash
mvn -B -DskipTests compile
mvn -B -pl payflow-admin-server spring-boot:run
# 另开终端
mvn -B -pl payflow-cashier-server spring-boot:run
mvn -B -pl payflow-recon-server spring-boot:run
```

**通过**: 启动无 `Table doesn't exist`；健康检查/登录接口 `code=0`。

## 4. 核心冒烟（映射 SC-005）

| # | 路径 | 验证点 |
|---|------|--------|
| 1 | 后台登录 | JWT 成功 |
| 2 | 订单列表 | 有演示订单 |
| 3 | 退款列表 | 有演示退款 |
| 4 | 对账任务 | recon 数据可读 |
| 5 | RBAC 角色/菜单 | `admin_sys_*` 种子生效 |
| 6 | 商户隔离 | `finance_demo` 仅见 M100001 |
| 7 | 支付回调链 | cashier 日志无 ERROR |

按需 Playwright:

```bash
cd payflow-admin-client && npx playwright test
```

**通过**: 关键用例绿；三服务日志无阻断 ERROR。

## 5. 死代码与构建

```bash
mvn -B clean package
cd payflow-admin-client && npm run build
cd payflow-cashier-client && npm run build
```

**通过**: 无编译错误；无未解析 import。

## 6. 文档与入口一致性（SC-003）

- `CLAUDE.md`、`sql/README.md` 仅描述 `install_demo_db.py` 单入口
- 无指向 `full-reseed-payflow-demo.sql` 的可执行指引

## 成功标准对照

| SC | 本 quickstart 步骤 |
|----|-------------------|
| SC-001 | 步骤 2 |
| SC-002 | 步骤 1（可重复 5 次） |
| SC-003 | 步骤 6 |
| SC-004 | 步骤 5 + rg 旧表名 |
| SC-005 | 步骤 4 |
| SC-006 | PR 内脚本目录 diff |
| SC-007 | plan 附录优化清单 |
| SC-008 | 步骤 4 API 行为不变 |
