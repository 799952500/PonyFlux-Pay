# Tasks: 项目宪法增强 — Java 开发规范整合

**Input**: Design documents from `/specs/003-constitution-enhancement/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, quickstart.md

**Tests**: 本功能为文档改造项目，不需要自动化测试。验收标准为人工 Review 验证宪法内容和模板文件的正确性。

**Organization**: 任务按用户故事分组，每个故事可独立实施和验收。

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 可并行执行（不同文件，无依赖）
- **[Story]**: 归属的用户故事（US1/US2/US3）
- 每个任务包含精确的文件路径

## Path Conventions

- `.specify/memory/constitution.md` — 项目宪法主文件
- `.specify/templates/*.md` — 模板文件
- `CLAUDE.md` — 代理上下文文件

---

## Phase 1: Setup（准备工作）

**Purpose**: 确认现有文件状态，备份原始宪法，准备规范引用清单

- [x] T001 备份当前宪法文件到 `.specify/memory/constitution.md.bak`
- [x] T002 [P] 阅读并记录当前 `.specify/memory/constitution.md` 的所有现有规则条目，生成现状清单
- [x] T003 [P] 汇总 research.md 中确定的规范来源条目，生成待写入的规则清单（按章节分组）

---

## Phase 2: Foundational（宪法核心章节编写）

**Purpose**: 编写宪法新增的 8 个章节，这是所有用户故事的前置依赖

**⚠️ CRITICAL**: 所有用户故事的工作必须在本阶段完成后才能开始

### 编码规范章节（FR-001）

- [x] T004 [P] 编写"编码规范 — 命名规范"子章节到 `.specify/memory/constitution.md`，涵盖：类名 UpperCamelCase、方法/变量 lowerCamelCase、常量 UPPER_SNAKE_CASE、包名全小写单数、枚举 Enum 后缀、布尔变量禁止 is 前缀、POJO 命名约定（DO/DTO/VO）
- [x] T005 [P] 编写"编码规范 — 代码格式与注释"子章节到 `.specify/memory/constitution.md`，涵盖：4空格缩进、K&R大括号、单行120字符限制、禁止通配符导入、Javadoc 规范（类/公共方法必须有）、TODO 格式规范
- [x] T006 [P] 编写"编码规范 — 类成员顺序与 POJO 规范"子章节到 `.specify/memory/constitution.md`，涵盖：成员顺序（常量→静态变量→实例变量→构造器→公共方法→私有方法）、POJO 属性必须用包装类型、禁止给 POJO 属性设默认值、构造器注入（`@RequiredArgsConstructor`）替代字段注入

### 集合与并发处理章节（FR-002）

- [x] T007 [P] 编写"集合与并发处理"章节到 `.specify/memory/constitution.md`，涵盖：集合初始化必须指定容量、`isEmpty()` 判空、`foreach` 中禁止 add/remove、`Arrays.asList()` 返回集合不可增删、禁止 `Executors` 创建线程池（必须用 `ThreadPoolExecutor`）、`ThreadLocal` 必须在 `finally` 中 `remove()`、优先使用 `java.util.concurrent`

### 数据库访问规范章节（FR-003）

- [x] T008 [P] 编写"数据库访问规范 — 实体类规范"子章节到 `.specify/memory/constitution.md`，涵盖：`@TableName` 指定表名、`@TableId(type=IdType.ASSIGN_ID)` 雪花主键、`@Version` 乐观锁、`@TableLogic` 逻辑删除、`@TableField(fill=...)` 自动填充、Long 型 ID 必须 `@JsonSerialize(using=ToStringSerializer.class)`
- [x] T009 [P] 编写"数据库访问规范 — 查询与操作规范"子章节到 `.specify/memory/constitution.md`，涵盖：禁止 `SELECT *`、分页必须设 `maxLimit`（≤500）、Lambda 方式引用列名（`User::getUserName`）、禁止 XML 中使用 `${}`（仅 ORDER BY 例外）、`BlockAttackInnerInterceptor` 防全表更新删除、批量操作单次 ≤500 条、多表 JOIN ≤3 张表、手写 SQL 需手动加 `deleted=0`

### 安全编码规范章节（FR-004）

- [x] T010 编写"安全编码规范"章节到 `.specify/memory/constitution.md`，涵盖：敏感数据 AES-256-GCM 加密存储、主密钥环境变量注入（`payflow.crypto.master-key`）、日志脱敏规则（手机号 `139****1219`、身份证、银行卡号、密码字段禁止明文）、HMAC-SHA256 请求签名 + 时间戳防重放（±5分钟窗口）、Redis 幂等锁、参数白名单校验、文件上传类型/大小检查、CSRF/XSS 防护

### 异常与日志规范章节（FR-005）

- [x] T011 [P] 编写"异常与日志规范"章节到 `.specify/memory/constitution.md`，涵盖：`@RestControllerAdvice` 全局异常处理、`BizException` 业务异常使用规范、错误码范围表（1xxx~7xxx）、日志级别使用场景（ERROR/WARN/INFO/DEBUG）、TraceId 全链路追踪（MDC 设置）、禁止日志中打印敏感信息（手机号/密码/密钥）

### 测试规范章节（FR-006 + FR-017）

- [x] T012 编写"测试规范"章节到 `.specify/memory/constitution.md`，涵盖：Definition of Done 五条件（Code Review 通过、单元测试通过、集成测试通过、文档更新、宪法合规）、最低行覆盖率 80%（Maven 模块聚合计算 / 前端项目聚合计算）、JaCoCo 配置示例、JUnit 5 + Mockito 单元测试规范、Testcontainers 集成测试规范、Vitest + Vue Test Utils 前端测试规范

### 自动化执行章节（FR-014）

- [x] T013 编写"自动化执行"章节到 `.specify/memory/constitution.md`，涵盖：IntelliJ IDEA 推荐插件（Alibaba Java Coding Guidelines / Checkstyle-IDEA）、VS Code 推荐插件（ESLint / Prettier）、CI 流水线质量门禁（Checkstyle → SonarQube → JaCoCo → 构建）、Git pre-commit hook 策略

### 前端规范章节（FR-015）

- [x] T014 编写"前端规范"章节到 `.specify/memory/constitution.md`，涵盖：Vue 3 组件命名规范（PascalCase 文件名 / kebab-case 模板引用）、Composition API 优先（`<script setup lang="ts">`）、TypeScript 类型定义规范（禁止 `any` 滥用、接口命名 `I` 前缀可选）、Pinia Store 命名与拆分策略、Axios 拦截器与响应解包规范、前端测试覆盖率 80%

---

**Checkpoint**: 所有新增章节编写完成，宪法文件具备完整结构。接下来按用户故事进行增强和打磨。

---

## Phase 3: User Story 1 - 开发者查阅宪法获得明确编码指引 (Priority: P1) 🎯 MVP

**Goal**: 宪法的每一条强制规则都有正例代码和反例代码，开发者能在 30 秒内找到答案

**Independent Test**: 任意 Java 开发者阅读宪法后，能在 2 分钟内找到命名、异常处理、安全编码的明确规则并写出合规代码

### 实施任务

- [x] T015 [P] [US1] 为"编码规范"章节的每条强制规则添加正例代码块和反例代码块到 `.specify/memory/constitution.md`
- [x] T016 [P] [US1] 为"集合与并发处理"章节的每条强制规则添加正例代码块和反例代码块到 `.specify/memory/constitution.md`
- [x] T017 [P] [US1] 为"数据库访问规范"章节的每条强制规则添加正例代码块和反例代码块到 `.specify/memory/constitution.md`
- [x] T018 [P] [US1] 为"安全编码规范"章节的每条强制规则添加正例代码块和反例代码块到 `.specify/memory/constitution.md`
- [x] T019 [P] [US1] 为"异常与日志规范"章节的每条强制规则添加正例代码块和反例代码块到 `.specify/memory/constitution.md`
- [x] T020 [P] [US1] 为"测试规范"章节的 Definition of Done 和覆盖率要求添加模板配置示例到 `.specify/memory/constitution.md`
- [x] T021 [US1] 为宪法每条规则添加约束力级别标注（`[强制]`/`[推荐]`/`[参考]`）到 `.specify/memory/constitution.md`，确保标注与 research.md 中的优先级一致
- [x] T022 [US1] 增强现有五大核心原则，为每个原则增加 2+ 个具体的违规案例和修复方案到 `.specify/memory/constitution.md`（参考优化报告中的 P0/P1 案例）

---

**Checkpoint**: User Story 1 完成——宪法具备完整的代码示例和约束力标注，可独立用于开发者指导

---

## Phase 4: User Story 2 - CI/Code Review 可依据宪法自动化检查 (Priority: P2)

**Goal**: 宪法中每条强制规则能对应到至少一种自动化检查手段

**Independent Test**: 提取宪法中所有强制规则，逐条对应到 IDE 插件规则/Checkstyle 规则/CI 脚本/Review Checklist

### 实施任务

- [x] T023 [US2] 为宪法"合规审查"部分增加 Code Review Checklist，将每条强制规则转化为可勾选的检查项到 `.specify/memory/constitution.md`（按章节分组，每项可打勾）
- [x] T024 [P] [US2] 验证并记录每条强制规则对应的自动化检查手段（Alibaba 插件规则号 / Checkstyle 模块 / SonarQube 规则键），生成映射表附录到 `.specify/memory/constitution.md`
- [x] T025 [P] [US2] 编写 CI 流水线配置指南，包含 Checkstyle (`google_checks.xml` 定制版)、JaCoCo (80% 行覆盖率门禁)、SonarQube 质量门禁的启用步骤到 `.specify/memory/constitution.md`
- [x] T026 [US2] 编写 IDE 插件安装与配置指南（Alibaba Java Coding Guidelines / Checkstyle-IDEA / ESLint），使开发者可一键导入统一配置到 `.specify/memory/constitution.md`

---

**Checkpoint**: User Story 2 完成——宪法的强制规则可被自动化验证，Review Checklist 可直接使用

---

## Phase 5: User Story 3 - 模板文件引导开发者遵循规范 (Priority: P3)

**Goal**: `.specify/templates/` 下 5 个模板文件从通用占位符替换为与项目对齐的具体内容

**Independent Test**: 使用 `/speckit-specify` 创建测试需求，生成的 spec.md 自动包含宪法合规段落

### 实施任务

- [x] T027 [US3] 改造 `.specify/templates/spec-template.md`：新增 "Constitution Compliance" 段落（引导列出功能涉及的宪法原则），将通用占位符替换为项目特有说明（引用 CLAUDE.md 中的模块列表和技术栈）
- [x] T028 [US3] 改造 `.specify/templates/plan-template.md`：Constitution Check 门禁从占位符 `[Gates determined based on constitution file]` 替换为基于宪法实际 5 大原则的具体检查项清单（带勾选框和宪法引用）
- [x] T029 [US3] 改造 `.specify/templates/tasks-template.md`：Phase 拆分建议从通用 `src/` 替换为项目实际模块列表（payflow-common / payflow-payment-core / payflow-payment-channels / payflow-cashier-server / payflow-admin-server / payflow-recon-server / 前端），并按模块边界添加任务分组说明
- [x] T030 [US3] 改造 `.specify/templates/checklist-template.md`：检查类别从通用占位符替换为编码规范、安全规范、测试覆盖率、API 规范、模块边界、文档更新等具体类别，每类包含子项提示
- [x] T031 [US3] 改造 `.specify/templates/constitution-template.md`：章节结构从通用占位符替换为与增强后宪法对齐的章节列表，包含编码规范、集合并发、数据库访问、安全编码、异常日志、测试规范、自动化执行、前端规范等占位说明
- [x] T032 [US3] 遍历所有 5 个模板文件，移除所有通用示例文本（如 `# [FEATURE NAME]`、`[e.g., Python 3.11]`、Option 1/2/3 结构选择器等），替换为具体说明

---

**Checkpoint**: 所有用户故事完成——宪法和模板文件形成完整闭环

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: 最终质量检查、一致性验证、文档同步

- [x] T033 [P] 全文检查宪法中所有代码示例的语法正确性，确保 Java 17 + Vue 3 语法一致
- [x] T034 [P] 检查 CLAUDE.md 与 constitution.md 之间的一致性，确保端口号、模块列表、技术版本号等无冲突
- [x] T035 [P] 验证宪法文件行数 ≤ 800 行（SC-001 可读性约束），如超出则精简代码示例或合并相似规则
- [x] T036 [P] 统计并验证强制规则数量 ≥ 60 条，且每条都有正例/反例代码（SC-005）
- [x] T037 逐章通读宪法全文，修正错别字、格式不一致、术语不统一等问题
- [x] T038 [P] 更新 `CLAUDE.md` 中的项目简介段落，反映宪法增强后的编码规范引用
- [x] T039 验证 quickstart.md 中"快速定位"表格的所有章节引用与实际宪法章节名称一致
- [x] T040 删除备份文件 `.specify/memory/constitution.md.bak`（确认改造成功后）

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 无依赖 — 可立即开始
- **Foundational (Phase 2)**: 依赖 Setup 完成 — **阻断所有用户故事**
- **User Story 1 (Phase 3)**: 依赖 Foundational 完成 — 在宪法草稿基础上添加代码示例
- **User Story 2 (Phase 4)**: 依赖 Foundational 完成 — 可与 US1 并行
- **User Story 3 (Phase 5)**: 依赖 Foundational 完成 — 可与 US1/US2 并行
- **Polish (Phase 6)**: 依赖所有用户故事完成

### User Story Dependencies

- **User Story 1 (P1)**: 依赖 Foundational — 无其他故事依赖
- **User Story 2 (P2)**: 依赖 Foundational — 无其他故事依赖，可与 US1 并行
- **User Story 3 (P3)**: 依赖 Foundational — 无其他故事依赖，可与 US1/US2 并行

### Within Each User Story

- 各章节的正例/反例任务（T015-T020）可并行执行
- US2 中 Checklist（T023）和自动化映射（T024）可并行
- US3 中 5 个模板改造（T027-T031）可并行执行

### Parallel Opportunities

- **Phase 1**: T002 + T003 可并行
- **Phase 2**: T004-T006（编码规范三子章节）可并行；T008+T009（数据库规范两子章节）可并行；各章节间（T004-T014）高度独立可大量并行
- **Phase 3**: T015-T020（六个章节的代码示例）全部可并行
- **Phase 4**: T024+T025 可并行
- **Phase 5**: T027-T031（五个模板改造）全部可并行
- **Phase 6**: T033-T036 全部可并行

---

## Parallel Example: Phase 2 Foundational

```bash
# 并行编写 6 个独立章节（不同内容，无相互依赖）：
Task: "编写编码规范 — 命名规范子章节到 .specify/memory/constitution.md"
Task: "编写编码规范 — 代码格式与注释子章节到 .specify/memory/constitution.md"
Task: "编写编码规范 — 类成员顺序与POJO规范子章节到 .specify/memory/constitution.md"
Task: "编写集合与并发处理章节到 .specify/memory/constitution.md"
Task: "编写数据库访问规范 — 实体类规范子章节到 .specify/memory/constitution.md"
Task: "编写安全编码规范章节到 .specify/memory/constitution.md"

# 以上 6 个任务操作同一个文件的不同段落，需要协调顺序写入
# 实际执行建议：按章节顺序串行写入，避免合并冲突
```

## Parallel Example: Phase 3 User Story 1

```bash
# 并行添加代码示例（按章节独立，可同时准备内容后统一写入）：
Task: "为编码规范章节每条强制规则添加正反例代码块"
Task: "为集合与并发处理章节每条强制规则添加正反例代码块"
Task: "为数据库访问规范章节每条强制规则添加正反例代码块"
Task: "为安全编码规范章节每条强制规则添加正反例代码块"
Task: "为异常与日志规范章节每条强制规则添加正反例代码块"
Task: "为测试规范章节添加模板配置示例"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. 完成 Phase 1: Setup（T001-T003）
2. 完成 Phase 2: Foundational（T004-T014）— CRITICAL
3. 完成 Phase 3: User Story 1（T015-T022）
4. **STOP and VALIDATE**: 独立测试——让一位开发者阅读宪法并尝试编写合规代码
5. 验收通过后即可交付 MVP 版本宪法

### Incremental Delivery

1. Setup + Foundational → 宪法草稿（有结构但缺代码示例）
2. + User Story 1 → 完整宪法 MVP（带代码示例和约束力标注）✅ MVP
3. + User Story 2 → 宪法 + 自动化执行指南 + Review Checklist
4. + User Story 3 → 宪法 + 全部模板文件一体化
5. + Polish → 最终发布版本

### 单人执行策略

按顺序执行：Phase 1 → Phase 2（按章节顺序写入）→ Phase 3（按章节顺序添加示例）→ Phase 4 → Phase 5 → Phase 6。预估总时间约 2-3 小时（含验证）。

---

## Notes

- 同一文件 `.specify/memory/constitution.md` 被大量任务引用，实际执行时需协调写入顺序：Phase 2 按章节顺序写入（T004→T005→...→T014），Phase 3 在 Phase 2 基础上追加代码示例
- [P] 标记的任务指"内容准备可并行"，但写入同一文件时需串行以避免合并冲突
- 每个 Phase 结束有 Checkpoint，建议在该点进行阶段性验证
- 宪法文件不得超过 800 行（约 600-800 行目标）
