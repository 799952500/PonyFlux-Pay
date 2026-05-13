# Feature Specification: 项目宪法增强 — Java 开发规范整合

**Feature Branch**: `003-constitution-enhancement`
**Created**: 2026-05-13
**Status**: Draft
**Input**: User description: "本次的目的主要是完善项目的宪法，提升编码的准确性和质量。需要改造的文件包含.specify下的所有文件，宪法来源需要你搜索目前网络上比较成熟和热门的java开发规范，并结合我们项目使用的框架和业务进行优化"

## Clarifications

### Session 2026-05-13

- Q: 宪法是否应该定义测试阶段作为开发完成的门禁？ → A: 需要增加测试阶段，开发完成之后必须通过测试，测试完成之后才算真正完成。测试应作为 Definition of Done 的强制组成部分。
- Q: 80% 测试覆盖率要求是否同样适用于 Vue/TypeScript 前端代码？ → A: 前端同样要求 80% 行覆盖率，一视同仁。
- Q: 80% 行覆盖率应该在哪个粒度上强制执行？ → A: 按 Maven 模块聚合计算，整个模块达到 80% 即可。

## User Scenarios & Testing

### User Story 1 - 开发者查阅宪法即可获得明确编码指引 (Priority: P1)

项目开发者（新人或现有成员）在编写代码时，需要一份权威、完整、可执行的编码规范。当遇到编码疑问（如"实体类应该怎么命名"、"金额应该用什么类型"、"异常应该怎么处理"）时，查阅 `.specify/memory/constitution.md` 能够立即获得明确答案，无需再搜索外部资料或凭个人经验判断。

**Why this priority**: 宪法是项目所有编码活动的最高准则。没有完善的宪法，代码质量完全依赖个人水平，无法保证一致性和可维护性。这是所有后续规范落地的基础。

**Independent Test**: 任意一位 Java 开发者阅读宪法后，能够在 2 分钟内找到关于命名规范、异常处理、安全编码的明确规则，并写出符合规范的代码。

**Acceptance Scenarios**:

1. **Given** 开发者需要创建一个新的实体类，**When** 查阅宪法中的命名与分层规范章节，**Then** 能够在 30 秒内找到实体类命名、包路径、字段类型的明确规则。
2. **Given** 开发者需要处理支付回调中的异常，**When** 查阅宪法中的异常处理规范章节，**Then** 能够确定使用哪个异常类、错误码范围、以及响应格式。
3. **Given** 开发者需要存储商户密钥，**When** 查阅宪法中的安全规范章节，**Then** 能够找到加密算法要求、密钥管理方式和禁止事项。

---

### User Story 2 - CI/Code Review 可依据宪法进行自动化检查 (Priority: P2)

Code Review 和 CI 流水线需要可量化的检查项。宪法中的每一条规则必须足够明确，使其能够转化为 Checkstyle/PMD/SonarQube 规则或人工 Review Checklist。模糊的"建议"或"最佳实践"需要被提升为可验证的具体条目。

**Why this priority**: 规范如果不可检查，就等于没有规范。将宪法条目转化为自动化检查可以消除人工 Review 中的主观争论，提升效率。

**Independent Test**: 提取宪法中的所有"强制"级别规则，能够逐条对应到一个具体的检查手段（IDE 插件、CI 脚本、Review Checklist 项目）。

**Acceptance Scenarios**:

1. **Given** 一条宪法中的强制规则（如"禁止使用 `Executors` 创建线程池"），**When** CI 流水线扫描代码，**Then** 违反该规则的代码构建失败并给出明确错误信息。
2. **Given** Code Review 清单来源于宪法，**When** Reviewer 逐项检查，**Then** 每一项都能在宪法中找到对应的原文条款。
3. **Given** 开发者使用 IDE，**When** 安装项目推荐的代码检查插件，**Then** 编码时实时收到违反宪法规则的警告。

---

### User Story 3 - 模板文件自动引导开发者遵循规范 (Priority: P3)

`.specify/templates/` 下的所有模板文件（spec-template、plan-template、tasks-template、checklist-template、constitution-template）需要内嵌宪法规则引导，使开发者在使用 `/speckit-*` 命令时自然遵循规范，减少事后返工。

**Why this priority**: 模板是开发者与规范体系的交互界面。好的模板设计能在源头减少违规，降低 Review 成本。但模板优化依赖于宪法内容先稳定。

**Independent Test**: 使用 `/speckit-specify` 创建一个测试需求，生成的 spec.md 中自动包含与宪法一致的约束检查项。

**Acceptance Scenarios**:

1. **Given** 开发者使用 `/speckit-plan` 生成实施计划，**When** 模板展开，**Then** Constitution Check 门禁自动列出与本项目宪法对应的检查项。
2. **Given** 开发者使用 `/speckit-tasks` 生成任务列表，**When** 任务生成，**Then** 任务自动按模块边界分组，不会出现跨模块混排的任务。
3. **Given** 开发者使用 `/speckit-checklist` 生成检查清单，**When** 清单生成，**Then** 包含编码规范、安全规范、模块边界等与宪法对齐的检查类别。

---

### Edge Cases

- 当宪法中的规范与 CLAUDE.md 的运行时指导冲突时，以哪个为准？（现有治理规则已规定需修订其一，但需强化 CLAUDE.md 与宪法的自动一致性检查）
- 项目新增模块或技术栈时，宪法的更新流程是什么？
- 第三方依赖的版本升级是否受宪法约束？（如 MyBatis-Plus 大版本升级）
- 多语言混合场景（Java 后端 + Vue/TypeScript 前端），宪法是否覆盖前端规范？

## Requirements

### Functional Requirements

**宪法内容增强**：

- **FR-001**: 宪法必须增加"编码规范"章节，涵盖：命名规范（类/方法/变量/常量/包）、缩进与格式、注释规范、代码结构顺序，内容综合自 Alibaba Java Development Manual（泰山/嵩山/黄山版）和 Google Java Style Guide。
- **FR-002**: 宪法必须增加"集合与并发处理"章节，明确：集合初始化容量要求、foreach 循环中禁止修改集合、线程池创建规范（禁止 Executors）、ThreadLocal 清理规则。
- **FR-003**: 宪法必须增加"数据库访问规范"章节，涵盖：MyBatis-Plus 实体类规范（`@TableName`、`@TableId`、`@Version`、`@TableLogic`）、禁止 `SELECT *`、分页查询必须设上限、SQL 注入防护（禁止 `${}`）、多表 JOIN 不超过 3 张表、批量操作单次上限。
- **FR-004**: 宪法必须增加"安全编码规范"章节，涵盖：敏感数据 AES-256-GCM 加密存储、日志脱敏规则（手机号/身份证/银行卡号/密码）、防重放与幂等性设计、SQL 注入与 XSS 防护、文件上传安全检查。
- **FR-005**: 宪法必须增加"异常与日志规范"章节，涵盖：全局异常处理器统一响应格式、错误码分配规则精细化到子范围、日志级别使用规范（ERROR/WARN/INFO/DEBUG）、TraceId 全链路追踪要求、禁止日志中打印敏感信息。
- **FR-006**: 宪法必须增加"测试规范"章节，涵盖：单元测试与集成测试边界、Mock 使用原则、Testcontainers 推荐。必须规定最低行覆盖率为 80%（统一适用于所有 Maven 模块，按模块聚合计算；前端项目同理按项目聚合计算），并明确测试是开发完成的强制门禁——未通过测试的代码不得合并、不得部署、不得标记为"已完成"。
- **FR-017**: 宪法必须定义"Definition of Done"，明确一个功能/任务只有在以下条件全部满足时才能标记为完成：(a) 代码通过 Code Review，(b) 单元测试全部通过，(c) 集成测试全部通过，(d) 相关文档已更新，(e) 宪法合规检查通过。
- **FR-007**: 宪法现有的五大原则（模块边界、支付渠道抽象、数据库分区、API 响应、密钥安全）必须保留并增强，增加更多具体违规案例和对应修复方案。
- **FR-008**: 宪法必须为每条规则标注约束力级别：**强制**（违反导致构建失败/拒绝合并）、**推荐**（违反需在 Review 中说明理由）、**参考**（提供上下文指导）。

**模板文件改造**：

- **FR-009**: `spec-template.md` 必须增加"Constitution Compliance"段落，使每个功能规范在创建时就考虑与宪法的对齐。
- **FR-010**: `plan-template.md` 的 Constitution Check 门禁必须从占位符替换为基于实际宪法原则的具体检查项清单。
- **FR-011**: `tasks-template.md` 必须增加按模块边界组织的任务分组建议，与宪法原则 I（模块边界纪律）对齐。
- **FR-012**: `checklist-template.md` 必须增加编码规范、安全规范、测试规范等检查类别，与宪法新章节对齐。
- **FR-013**: `constitution-template.md` 必须更新结构，使其包含编码规范、安全规范、测试规范等新增章节的占位说明。

**治理机制增强**：

- **FR-014**: 宪法必须增加"自动化执行"章节，描述如何通过 IDE 插件（Alibaba Java Coding Guidelines、Checkstyle、SonarQube）和 CI 流水线落地宪法规则。
- **FR-015**: 宪法必须增加"前端规范"章节或明确前端规范的引用位置，涵盖：Vue 3/TypeScript 编码规范、组件命名与结构规范、前端状态管理（Pinia）规范、前端测试覆盖率要求（与后端一致为 80% 行覆盖率）、前端 API 调用规范（与 CONTRACT_MATRIX.md 对齐）。
- **FR-016**: 合规审查部分必须增加具体的 Review Checklist，将宪法每一条"强制"规则转化为可勾选的检查项。

### Key Entities

- **宪法原则（Principle）**: 一条编码或架构规则，包含：名称、约束力级别（强制/推荐/参考）、规则描述、违反后果、正例与反例代码、对应检查工具。
- **约束力级别（Severity）**: 枚举值 — `MANDATORY`（强制，阻断合并）、`RECOMMENDED`（推荐，需说明理由）、`REFERENCE`（参考，上下文指导）。
- **模板（Template）**: `.specify/templates/` 下的 Markdown 文件，内嵌宪法引导。每个模板有明确的填充规则和与宪法的关联章节。
- **合规检查项（Compliance Item）**: 从宪法原则中提取的可验证条目，能对应到具体的自动化检查或人工 Review 步骤。

## Success Criteria

### Measurable Outcomes

- **SC-001**: 宪法覆盖的规范领域从当前的 5 个核心原则扩展到至少 10 个章节（新增：编码规范、集合与并发、数据库访问、安全编码、异常与日志、测试规范、自动化执行、前端规范），所有章节均有具体可执行的规则条目。
- **SC-002**: 宪法中 90% 以上的"强制"级别规则能够对应到至少一种自动化检查手段（IDE 插件规则 / Checkstyle 规则 / CI 脚本 / Git Hook）。
- **SC-003**: 新加入项目的开发者（有 Java 基础但不了解本项目）阅读宪法后，能够在第一次 Code Review 中违规率低于 20%（以强制规则计数）。
- **SC-004**: `.specify/` 下所有模板文件（5 个模板）的占位符内容 100% 替换为与项目实际技术栈和宪法原则对齐的具体内容，不再包含通用示例文本。
- **SC-005**: 宪法的每一条强制规则都至少有一个正例代码示例和一个反例代码示例，使规则含义不会产生歧义。
- **SC-006**: Code Review 中因"编码风格"产生的主观讨论减少 50% 以上（以 Review 评论中涉及格式、命名、风格的条目占比衡量）。
- **SC-007**: 所有合并到主分支的代码必须通过宪法定义的测试门禁——单元测试和集成测试 100% 通过，且行覆盖率达到 80% 以上。不满足此条件的 PR 不得合并。

## Assumptions

- 项目继续使用当前技术栈（Java 17、Spring Boot 3.2.5、MyBatis-Plus 3.5.7、Vue 3），宪法规范针对此技术栈编写，不涉及技术栈变更。
- 阿里巴巴 Java 开发手册（泰山/嵩山/黄山版）和 Google Java Style Guide 是业界公认的权威规范来源，其核心规则可直接采纳为本项目强制标准。
- 开发团队使用 IntelliJ IDEA 作为主要 IDE，可以安装 Alibaba Java Coding Guidelines 和 Checkstyle-IDEA 插件。
- 前端规范将在本宪法中以独立章节或引用方式体现，不创建独立的前端宪法文件（避免碎片化）。
- 现有的支付渠道抽象（原则 II）、数据库分区（原则 III）、API 响应（原则 IV）已经过实践验证，不需要结构性调整，但需要补充更多违规案例和修复方案。
- `.specify/extensions/` 下的 Git 钩子脚本不在本次改造范围内，它们属于基础设施而非内容规范。
