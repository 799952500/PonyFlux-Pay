# Implementation Plan: 项目宪法增强 — Java 开发规范整合

**Branch**: `003-constitution-enhancement` | **Date**: 2026-05-13 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/003-constitution-enhancement/spec.md`

## Summary

基于阿里巴巴 Java 开发手册（泰山/嵩山/黄山版）、Google Java Style Guide、Spring Boot 3 生产级最佳实践和 MyBatis-Plus 深度开发规范，将项目宪法（`.specify/memory/constitution.md`）从当前的 5 个核心原则扩展为 13+ 个章节的完整编码规范体系。同步改造 `.specify/templates/` 下 5 个模板文件，使其从通用占位符升级为与项目技术栈对齐的具体引导内容。最终建立以宪法为最高准则、模板为开发引导、CI 检查为自动化执行的完整质量保障体系。

## Technical Context

**Language/Version**: Java 17 / Spring Boot 3.2.5 / Vue 3 + TypeScript
**Primary Dependencies**: MyBatis-Plus 3.5.7, Maven (multi-module), Element Plus 2.5+
**Storage**: N/A（文档改造项目，不涉及数据库变更）
**Testing**: JUnit 5 / Mockito / Vitest（宪法将规范测试框架，本功能本身无代码测试）
**Target Platform**: 项目文档体系（`.specify/` 目录下的 Markdown 文件）
**Project Type**: 项目治理文档改造（constitution + templates 增强）
**Performance Goals**: N/A（文档型产出）
**Constraints**: 宪法文件大小不超过 800 行（保持可读性），每个强制规则必须有正例和反例代码片段
**Scale/Scope**: 改造 1 份宪法文件 + 5 个模板文件 + 更新 CLAUDE.md 引用；新增 6-8 个宪法章节

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 原则 | 检查项 | 状态 |
|------|--------|------|
| I. 模块边界纪律 | 新规范不会破坏现有模块边界定义，仅增强说明和案例 | ✅ PASS |
| II. 支付渠道抽象 | 编码规范不会替代或弱化策略模式要求 | ✅ PASS |
| III. 数据库分区 | 数据库访问规范将明确双库分区规则并补充 MyBatis-Plus 配置示例 | ✅ PASS |
| IV. API 响应规范 | 异常与日志规范将强化统一响应格式要求 | ✅ PASS |
| V. 密钥与配置安全 | 安全编码规范将扩展加密、脱敏、防重放要求 | ✅ PASS |

**Gate Result**: ALL PASS — 无违规需要豁免。本次改造是对现有原则的补充和增强，不修改任何不可协商规则。

## Project Structure

### Documentation (this feature)

```text
specs/003-constitution-enhancement/
├── plan.md              # This file
├── research.md          # Phase 0: 规范来源研究与决策
├── data-model.md        # Phase 1: 宪法章节结构设计
├── quickstart.md        # Phase 1: 宪法使用快速入门指南
└── tasks.md             # Phase 2: 实施任务 (/speckit-tasks)
```

### Modified Files (repository root)

```text
.specify/
├── memory/
│   └── constitution.md          # 主要改造目标：从 5 原则扩展到 13+ 章节
├── templates/
│   ├── spec-template.md         # 增加 Constitution Compliance 段落
│   ├── plan-template.md         # Constitution Check 门禁具象化
│   ├── tasks-template.md        # 模块边界分组引导
│   ├── checklist-template.md    # 增加编码/安全/测试检查类别
│   └── constitution-template.md # 更新章节结构占位
└── feature.json                 # 无需修改（已存在）
```

**Structure Decision**: 本功能仅修改 `.specify/` 目录下的 Markdown 文件，不涉及 Java/Vue 源码变更。所有规范来源研究整合到 research.md，章节结构设计整合到 data-model.md。

## Complexity Tracking

> 无宪法违规，此节留空。
