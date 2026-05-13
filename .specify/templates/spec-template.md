# Feature Specification: [FEATURE NAME]

**Feature Branch**: `[###-feature-name]`
**Created**: [DATE]
**Status**: Draft
**Input**: User description: "$ARGUMENTS"

## Constitution Compliance *(mandatory)*

在编写规范前，确认本功能涉及的宪法原则：

| 宪法原则 | 是否涉及 | 说明 |
|----------|----------|------|
| I. 模块边界纪律 | [ ] 是 / [ ] 否 | 新增代码放在哪个 Maven 模块？ |
| II. 支付渠道抽象 | [ ] 是 / [ ] 否 | 是否新增支付渠道或修改支付流程？ |
| III. 数据库分区 | [ ] 是 / [ ] 否 | 新增表是 admin 前缀还是 cashier 前缀？ |
| IV. API 响应规范 | [ ] 是 / [ ] 否 | 新增 API 是否返回统一 `{ code, message, data }` 格式？ |
| V. 密钥与配置安全 | [ ] 是 / [ ] 否 | 是否涉及密钥存储或 JWT/CORS 配置？ |
| 编码规范 | [ ] 是 / [ ] 否 | 新增类/方法是否遵循命名和成员顺序规范？ |
| 数据库访问规范 | [ ] 是 / [ ] 否 | 是否涉及 SQL、实体类、分页查询？ |
| 安全编码规范 | [ ] 是 / [ ] 否 | 是否需要日志脱敏、参数校验、防重放？ |
| 测试规范 | [ ] 是 / [ ] 否 | 是否满足 DoD 五条件和 80% 覆盖率要求？ |

> 涉及的原则将在 `plan.md` Constitution Check 中逐项检查。

## User Scenarios & Testing *(mandatory)*

<!--
  User stories 按优先级排序（P1 最高）。每个 user story 必须可独立测试。
  思考每个 story 作为一个独立的功能切片，可以独立开发、测试、部署、演示。
-->

### User Story 1 - [Brief Title] (Priority: P1)

[描述用户旅程，用中文]

**Why this priority**: [解释价值和为什么是此优先级]

**Independent Test**: [描述如何独立测试——例如，"可以通过[具体操作]完整测试并交付[具体价值]"]

**Acceptance Scenarios**:

1. **Given** [初始状态], **When** [操作], **Then** [预期结果]
2. **Given** [初始状态], **When** [操作], **Then** [预期结果]

---

### User Story 2 - [Brief Title] (Priority: P2)

[描述用户旅程，用中文]

**Why this priority**: [解释价值和为什么是此优先级]

**Independent Test**: [描述如何独立测试]

**Acceptance Scenarios**:

1. **Given** [初始状态], **When** [操作], **Then** [预期结果]

---

[Add more user stories as needed, each with an assigned priority]

### Edge Cases

- [边界条件] 发生时，系统如何处理？
- [错误场景] 发生时，用户会看到什么？

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 系统必须 [具体能力]
- **FR-002**: 系统必须 [具体能力]
- **FR-003**: 用户必须能够 [关键交互]

### Key Entities *(include if feature involves data)*

- **[Entity 1]**: 代表什么，关键属性（不涉及实现细节）
- **[Entity 2]**: 代表什么，与其他实体的关系

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: [可度量指标，如 "用户可在 2 分钟内完成订单创建"]
- **SC-002**: [可度量指标，如 "系统支持 1000 并发用户无性能下降"]
- **SC-003**: [用户满意度指标，如 "90% 用户首次操作成功"]

## Assumptions

- [关于目标用户的假设]
- [关于范围边界的假设]
- [关于数据/环境的假设]
- [依赖现有系统/服务的说明]
