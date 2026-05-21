---

description: "功能实施任务列表模板"

---

# Tasks: [FEATURE NAME]

**Input**: Design documents from `/specs/[###-feature-name]/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: 示例中包含了测试任务。测试是 OPTIONAL——仅在 spec.md 明确要求时包含。

**Organization**: 任务按用户故事分组，每个故事可独立实施和测试。任务分组采用 PonyFlux-Pay 项目的 Maven 模块边界。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行执行（不同文件，无依赖）
- **[Story]**: 归属的用户故事（US1、US2、US3）
- 任务描述必须包含精确的文件路径

## Path Conventions

### 后端模块

| 模块 | 源码路径 | 用途 |
|------|----------|------|
| payflow-common | `payflow-common/src/main/java/com/ponyflux/payflow/common/` | 共享工具、异常、加密、常量 |
| payflow-payment-core | `payflow-payment-core/src/main/java/com/ponyflux/payflow/payment/core/` | 支付 SPI、枚举、DTO |
| payflow-payment-channels/* | `payflow-payment-channels/payflow-payment-{channel}/src/main/java/` | 渠道 API 处理器 |
| payflow-cashier-server | `payflow-cashier-server/src/main/java/com/ponyflux/payflow/cashier/` | 支付服务、订单管理 |
| payflow-admin-server | `payflow-admin-server/src/main/java/com/ponyflux/payflow/admin/` | 管理后台 |
| payflow-recon-server | `payflow-recon-server/src/main/java/com/ponyflux/payflow/recon/` | 对账引擎 |
| payflow-sdk-java | `payflow-sdk-java/src/main/java/` | HMAC-SHA256 签名 SDK |

### 前端项目

| 项目 | 路径 | 技术栈 |
|------|------|--------|
| admin-client | `payflow-admin-client/src/` | Vue 3 + TypeScript + Element Plus |
| cashier-client | `payflow-cashier-client/src/` | Vue 3 + TypeScript + Element Plus |

### 数据库迁移

| 类型 | 路径 | 说明 |
|------|------|------|
| 增量迁移 | `sql/migrations/YYYY-MM-DD_描述.sql` | Schema 变更，向后兼容 |
| 全量安装 | `sql/full-reseed-payflow-demo.sql` | Demo 数据重装 |

---

## Phase 1: Setup（共享基础设施）

**Purpose**: 项目初始化和基础结构

- [ ] T001 按实施计划创建项目结构
- [ ] T002 初始化依赖配置（Maven `pom.xml` 或 npm `package.json`）
- [ ] T003 [P] 配置代码检查和格式化工具（Checkstyle / ESLint / Prettier）

---

## Phase 2: Foundational（阻断性前置条件）

**Purpose**: 必须在所有用户故事之前完成的核心基础设施

**⚠️ CRITICAL**: 在本阶段完成之前，不得开始任何用户故事工作。

- [ ] T004 数据库 Schema 变更（`sql/migrations/`）
- [ ] T005 [P] 配置认证/授权框架（JWT 拦截器、权限中间件）
- [ ] T006 [P] 配置 API 路由和中间件结构
- [ ] T007 创建所有用户故事共享的基础实体/模型
- [ ] T008 配置全局异常处理和日志基础设施
- [ ] T009 配置环境配置管理

**Checkpoint**: 基础设施就绪 — 可以开始并行实现用户故事

---

## Phase 3: User Story 1 - [Title] (Priority: P1) 🎯 MVP

**Goal**: [此故事交付的内容]

**Independent Test**: [如何独立验证此故事]

### Tests for User Story 1（OPTIONAL — 仅当 spec 要求时） ⚠️

> **NOTE: 先写测试，确保测试 FAIL 后再实现**

- [ ] T010 [P] [US1] 接口契约测试 `tests/contract/test_[name].java`
- [ ] T011 [P] [US1] 集成测试 `tests/integration/test_[name].java`

### Implementation for User Story 1

> 按模块边界组织任务。涉及多个模块时，按依赖顺序：common → core → channels → server → 前端

- [ ] T012 [P] [US1] 创建实体类 `payflow-xxx/src/main/java/.../entity/XxxEntity.java`
- [ ] T013 [P] [US1] 创建 DTO `payflow-xxx/src/main/java/.../dto/XxxDTO.java`
- [ ] T014 [US1] 实现 Mapper `payflow-xxx/src/main/java/.../mapper/XxxMapper.java`
- [ ] T015 [US1] 实现 Service `payflow-xxx/src/main/java/.../service/XxxService.java`（依赖 T012, T013, T014）
- [ ] T016 [US1] 实现 Controller `payflow-xxx/src/main/java/.../controller/XxxController.java`
- [ ] T017 [US1] 前端页面 `payflow-xxx-client/src/views/XxxView.vue`
- [ ] T018 [US1] 前端 API 模块 `payflow-xxx-client/src/api/xxx.js`
- [ ] T019 [US1] 添加验证和错误处理
- [ ] T020 [US1] 更新 `docs/CONTRACT_MATRIX.md`

**Checkpoint**: User Story 1 应完全可独立运行和测试

---

## Phase 4: User Story 2 - [Title] (Priority: P2)

**Goal**: [此故事交付的内容]

**Independent Test**: [如何独立验证此故事]

### Tests for User Story 2（OPTIONAL） ⚠️

- [ ] T021 [P] [US2] 契约测试 `tests/contract/test_[name].java`

### Implementation for User Story 2

- [ ] T022 [P] [US2] 创建模型
- [ ] T023 [US2] 实现 Service
- [ ] T024 [US2] 实现 Controller/端点

**Checkpoint**: User Stories 1 和 2 应各自独立运行

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: 影响多个用户故事的改进

- [ ] TXXX [P] 更新 `docs/CONTRACT_MATRIX.md`
- [ ] TXXX 代码清理和重构
- [ ] TXXX 性能优化
- [ ] TXXX [P] 补充单元测试（如 spec 要求）
- [ ] TXXX 安全加固（脱敏、加密、防注入检查）
- [ ] TXXX 运行 quickstart.md 验证
- [ ] TXXX 按需使用 Playwright/Playwright CLI 验证关键前端交互和跨服务流程
- [ ] TXXX 监控相关后台服务日志，根据异常日志修复后重复验证至无阻断错误
- [ ] TXXX 宪法合规检查 — 验证所有强制规则通过

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 无依赖 — 可立即开始
- **Foundational (Phase 2)**: 依赖 Setup 完成 — **阻断所有用户故事**
- **User Stories (Phase 3+)**: 全部依赖 Foundational 完成
  - 用户故事之间可并行（如果多开发者）或按优先级顺序串行
- **Polish (Final Phase)**: 依赖所有用户故事完成

### User Story Dependencies

- **User Story 1 (P1)**: Foundational 完成后可开始 — 无其他故事依赖
- **User Story 2 (P2)**: Foundational 完成后可开始 — 可独立于 US1
- **User Story 3 (P3)**: Foundational 完成后可开始 — 可独立于 US1/US2

### Within Each User Story

- 测试（如包含）必须先写并 FAIL，再实现
- 模型优先于 Service
- Service 优先于 Controller
- 核心实现优先于集成
- 当前故事完成后再进入下一优先级

### Module Boundary Order

涉及多个 Maven 模块时，按依赖顺序实施：
```
payflow-common → payflow-payment-core → payflow-payment-channels/*
    → payflow-cashier-server / payflow-admin-server / payflow-recon-server
    → 前端（admin-client / cashier-client）
```

---

## Parallel Example: User Story 1

```bash
# 同时启动 User Story 1 的所有测试（如包含测试）：
Task: "契约测试 tests/contract/test_[name].java"
Task: "集成测试 tests/integration/test_[name].java"

# 同时创建 User Story 1 的所有模型（不同文件，无依赖）：
Task: "创建实体类 payflow-xxx/.../entity/Xxx1.java"
Task: "创建实体类 payflow-xxx/.../entity/Xxx2.java"
Task: "创建 DTO payflow-xxx/.../dto/XxxDTO.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. 完成 Phase 1: Setup
2. 完成 Phase 2: Foundational（CRITICAL — 阻断所有故事）
3. 完成 Phase 3: User Story 1
4. **STOP and VALIDATE**: 独立测试 User Story 1，涉及 UI 或跨服务流程时按需执行 Playwright/Playwright CLI 验证并监控后台日志
5. 验证发现阻断日志或异常时先修复根因并重复测试，全部通过后部署/演示

### Incremental Delivery

1. Setup + Foundational → 基础就绪
2. + User Story 1 → 独立测试 + 按需 Playwright/日志验证 → 部署/演示（MVP！）
3. + User Story 2 → 独立测试 + 按需 Playwright/日志验证 → 部署/演示
4. + User Story 3 → 独立测试 + 按需 Playwright/日志验证 → 部署/演示
5. 每个故事增加价值而不破坏已有故事

### Parallel Team Strategy

多开发者时：

1. 团队共同完成 Setup + Foundational
2. Foundational 完成后：
   - 开发者 A: User Story 1
   - 开发者 B: User Story 2
   - 开发者 C: User Story 3
3. 各故事独立完成和集成

---
